"""Content Enrichment Agent using LangChain and Ollama."""

import logging
import time
import random
import re
from typing import Optional

from langchain_ollama import ChatOllama
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage
from langchain_core.output_parsers import StrOutputParser

from ..config import settings
from ..models import EnrichedContent, PlatformType, ThoughtRequest

logger = logging.getLogger(__name__)


# Platform-specific system prompts
PLATFORM_PROMPTS = {
    PlatformType.LINKEDIN: """You are an expert LinkedIn content creator specializing in professional 
thought leadership posts. Your content should be:
- Professional yet engaging and personable
- Between 1200-1500 characters (optimal for LinkedIn engagement)
- Include a compelling hook in the first line
- Use line breaks for readability
- End with a thought-provoking question or call-to-action
- Include 3-5 relevant hashtags

Write content that establishes authority while being relatable to professionals.""",

    PlatformType.FACEBOOK: """You are an expert Facebook content creator. Your content should be:
- Conversational and engaging
- Between 100-250 words for optimal engagement
- Include emotional hooks that encourage sharing
- Use emojis sparingly but effectively
- End with a question to encourage comments
- Include 2-3 relevant hashtags

Write content that feels personal and encourages community interaction.""",

    PlatformType.INSTAGRAM: """You are an expert Instagram caption writer. Your content should be:
- Engaging and visually descriptive
- Start with a hook before the "more" fold (first 125 characters)
- Between 150-300 words total
- Use emojis strategically throughout
- Include a clear call-to-action
- End with 20-30 relevant hashtags (separated by line breaks)

Write content that complements visual media and encourages saves and shares.""",
}


BASE_SYSTEM_PROMPT = """You are an AI content enrichment specialist. Your task is to transform 
raw thoughts and ideas into polished, engaging social media content.

Guidelines:
1. Expand on the core idea while maintaining the original intent
2. Add relevant context, examples, or data points when appropriate
3. Use clear, accessible language
4. Create content that provides value to readers
5. Maintain authenticity - the content should feel genuine, not overly promotional

Always respond with ONLY the enriched content text, no additional commentary or formatting markers."""


class ContentEnrichmentAgent:
    """Agent for enriching thoughts into platform-specific social media content."""

    def __init__(
        self,
        model_name: Optional[str] = None,
        base_url: Optional[str] = None,
    ):
        """Initialize the content enrichment agent."""
        self.model_name = model_name or settings.ollama_model
        self.base_url = base_url or settings.ollama_base_url
        self._llm: Optional[ChatOllama] = None
        self._conversation_history: dict[str, list] = {}
        logger.info(f"Initialized ContentEnrichmentAgent: model='{self.model_name}', url='{self.base_url}'")

    def _get_llm(self) -> ChatOllama:
        """Get or create the LLM instance."""
        if not self._llm:
            logger.info(f"FACTORY: Initializing Ollama Chat LLM: {self.model_name}")
            self._llm = ChatOllama(
                model=self.model_name,
                base_url=self.base_url,
                temperature=0.7,
                num_ctx=4096,
            )
        return self._llm

    def _get_prompt_template(
        self, platform: PlatformType, request: ThoughtRequest
    ) -> ChatPromptTemplate:
        """Create a prompt template for the given platform using dynamic prompts from request."""
        logger.info(f"PROMPT ASSEMBLY: Constructing template for {platform.value}")

        # Control Flow Logic: System Prompt Selection
        if request.model_role:
            logger.info("Decision: Using DYNAMIC model role from request category metadata")
            system_prompt = request.model_role
        else:
            logger.info("Decision: Using DEFAULT base system prompt")
            system_prompt = BASE_SYSTEM_PROMPT

        # Category context logic
        if request.search_description:
            logger.info("Decision: Adding category-specific search description to system prompt")
            system_prompt += f"\n\nCategory Context: {request.search_description}"
        else:
            logger.debug("No search description provided in request")

        # Platform Prompt Logic
        # Try to find platform-specific configuration
        config = next((c for c in request.platform_configurations if c.platform == platform), None)

        platform_prompt = None
        if config and config.prompt:
            logger.info(f"Decision: Using DYNAMIC platform prompt from config for {platform.value}")
            platform_prompt = config.prompt
        elif request.platform_prompts.get(platform):
            logger.info(f"Decision: Using LEGACY platform prompt from dict for {platform.value}")
            platform_prompt = request.platform_prompts.get(platform)
        else:
            logger.info(f"Decision: Using HARDCODED default platform prompt for {platform.value}")
            platform_prompt = PLATFORM_PROMPTS.get(platform, PLATFORM_PROMPTS[PlatformType.LINKEDIN])

        logger.debug(f"PROMPT ASSEMBLY: Final system prompt length: {len(system_prompt)} chars")
        logger.debug(f"PROMPT ASSEMBLY: Platform prompt length: {len(platform_prompt)} chars")

        return ChatPromptTemplate.from_messages([
            ("system", system_prompt + "\n\n" + platform_prompt),
            MessagesPlaceholder(variable_name="history"),
            ("human", "{input}"),
        ])

    def get_history(self, request_id: str) -> list:
        return self._conversation_history.get(request_id, [])

    def set_history(self, request_id: str, history: list) -> None:
        self._conversation_history[request_id] = history

    def _add_to_history(
        self, request_id: str, human_msg: str, ai_msg: str
    ) -> None:
        """Add messages to conversation history."""
        if request_id not in self._conversation_history:
            logger.debug(f"HISTORY: Initializing new history list for request {request_id}")
            self._conversation_history[request_id] = []

        logger.debug(f"HISTORY: Appending Human + AI exchange to request {request_id}")
        self._conversation_history[request_id].extend([
            HumanMessage(content=human_msg),
            AIMessage(content=ai_msg),
        ])

    def enrich_for_platform(
        self,
        request: ThoughtRequest,
        platform: PlatformType,
        additional_context: Optional[str] = None,
    ) -> EnrichedContent:
        """Enrich a thought for a specific platform."""
        logger.info(f"ENRICH FLOW: Starting enrichment for {platform.value} (Request: {request.request_id})")

        llm = self._get_llm()
        prompt = self._get_prompt_template(platform, request)

        # Build input message
        logger.debug("INPUT BUILD: Merging thought, instructions, and context")
        input_text = f"Original thought/topic: {request.original_thought}"

        if request.additional_instructions:
            logger.info(f"INPUT BUILD: Adding request-level additional instructions")
            input_text += f"\n\nAdditional instructions: {request.additional_instructions}"

        # Control Logic: Additional context source decision
        if not additional_context:
            logger.debug("Decision: Searching for platform-specific context in configuration")
            config = next((c for c in request.platform_configurations if c.platform == platform), None)
            if config and config.additional_context:
                logger.info(f"Decision: Found platform-level context for {platform.value}")
                additional_context = config.additional_context

        if additional_context:
            logger.info("INPUT BUILD: Appending context/feedback block")
            input_text += f"\n\nContext: {additional_context}"

        # Get history
        history = self.get_history(request.request_id)
        logger.info(f"ENRICH FLOW: Retrieved {len(history)} items from conversation history")

        # Create and run chain
        chain = prompt | llm | StrOutputParser()

        # Retry logic loop
        max_retries = 3
        retry_delay = 1.0

        logger.info(f"ENRICH FLOW: Invoking LLM with max {max_retries} retries...")
        for attempt in range(max_retries + 1):
            logger.info(f"ATTEMPT {attempt+1}/{max_retries+1} for platform {platform.value}")
            try:
                start_time = time.time()
                enriched_text = chain.invoke({
                    "history": history,
                    "input": input_text,
                })
                duration = time.time() - start_time

                logger.info(f"LLM SUCCESS: Received response in {duration:.2f}s. Length: {len(enriched_text)}")

                # History update
                self._add_to_history(request.request_id, input_text, enriched_text)

                # Hashtag extraction
                logger.info("LOGIC: Extracting hashtags from LLM response...")
                hashtags = self._extract_hashtags(enriched_text)
                logger.info(f"LOGIC: Found {len(hashtags)} unique hashtags")

                # Success Decision
                logger.info(f"ENRICH FLOW: Successfully completed {platform.value}")
                return EnrichedContent(
                    platform=platform,
                    body=enriched_text,
                    hashtags=hashtags,
                )

            except Exception as e:
                # Decision: Should we retry?
                error_str = str(e)
                is_retryable = "503" in error_str or "Connection refused" in error_str or "timeout" in error_str.lower()

                if attempt < max_retries and is_retryable:
                    sleep_time = retry_delay * (2 ** attempt) + random.uniform(0, 1)
                    logger.warning(f"RETRY DECISION: Platform {platform.value} failed with retryable error: {e}")
                    logger.info(f"Waiting {sleep_time:.2f}s before next attempt...")
                    time.sleep(sleep_time)
                else:
                    logger.error(f"RETRY DECISION: Max retries reached or non-retryable error for {platform.value}")
                    logger.error(f"FINAL ERROR: {e}", exc_info=True)
                    raise

    def refine_content(
        self,
        request: ThoughtRequest,
        platform: PlatformType,
        refinement_instruction: str,
    ) -> EnrichedContent:
        """Refine previously generated content based on feedback."""
        logger.info(f"REFINE FLOW: Processing feedback for {platform.value} (Request: {request.request_id})")
        logger.info(f"Feedback: '{refinement_instruction}'")

        llm = self._get_llm()
        prompt = self._get_prompt_template(platform, request)
        history = self.get_history(request.request_id)

        # History Validation Logic
        if not history:
            logger.warning(f"REFINE LOGIC: NO history found for request {request.request_id}")
            logger.info("Decision: Initializing empty history for refinement (cold start refinement)")
            history = []
        else:
            logger.info(f"REFINE LOGIC: Proceeding with {len(history)} items of context")

        input_text = f"Please refine the content with the following feedback: {refinement_instruction}"
        chain = prompt | llm | StrOutputParser()

        logger.info("REFINE FLOW: Invoking LLM refinement...")
        refined_text = chain.invoke({
            "history": history,
            "input": input_text,
        })

        logger.info(f"REFINE SUCCESS: Content length: {len(refined_text)}")
        self._add_to_history(request.request_id, input_text, refined_text)

        hashtags = self._extract_hashtags(refined_text)
        return EnrichedContent(
            platform=platform,
            body=refined_text,
            hashtags=hashtags,
        )

    def _extract_hashtags(self, text: str) -> list[str]:
        """Extract hashtags from content text."""
        # Decision: Use regex to find hashtags
        hashtags = re.findall(r"#(\w+)", text)
        unique_hashtags = list(set(hashtags))
        logger.debug(f"EXTRACTOR: Found raw tags: {hashtags} -> Unique: {unique_hashtags}")
        return unique_hashtags

    def clear_history(self, request_id: str) -> None:
        if request_id in self._conversation_history:
            logger.info(f"HISTORY: Purging history for request {request_id}")
            del self._conversation_history[request_id]
        else:
            logger.debug(f"HISTORY: Attempted purge for {request_id}, but not found.")
