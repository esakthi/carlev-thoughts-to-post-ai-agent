"""Content Enrichment Agent using LangChain and Ollama."""

import logging
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
        """Initialize the content enrichment agent.

        Args:
            model_name: Ollama model to use (defaults to settings)
            base_url: Ollama API URL (defaults to settings)
        """
        self.model_name = model_name or settings.ollama_model
        self.base_url = base_url or settings.ollama_base_url
        self._llm: Optional[ChatOllama] = None
        self._conversation_history: dict[str, list] = {}

    def _get_llm(self) -> ChatOllama:
        """Get or create the LLM instance."""
        if not self._llm:
            self._llm = ChatOllama(
                model=self.model_name,
                base_url=self.base_url,
                temperature=0.7,
                num_ctx=4096,
            )
            logger.info(f"Initialized Ollama LLM with model: {self.model_name}")
        return self._llm

    def _get_prompt_template(
        self, platform: PlatformType, request: ThoughtRequest
    ) -> ChatPromptTemplate:
        """Create a prompt template for the given platform using dynamic prompts from request."""
        logger.info(f"Constructing prompt template for platform: {platform.value}")

        # Use dynamic system prompt from request, fallback to hardcoded if not provided
        if request.model_role:
            logger.debug(f"Using dynamic model role from request")
            system_prompt = request.model_role
        else:
            logger.debug(f"Using default BASE_SYSTEM_PROMPT")
            system_prompt = BASE_SYSTEM_PROMPT

        # Include category description if available
        if request.search_description:
            logger.debug(f"Adding category context: {request.search_description[:50]}...")
            system_prompt += f"\n\nCategory Context: {request.search_description}"

        # Use platform-specific prompt from request, fallback to hardcoded
        platform_prompt = request.platform_prompts.get(platform)
        if platform_prompt:
            logger.debug(f"Using dynamic platform prompt for {platform.value}")
        else:
            logger.debug(f"Using hardcoded default platform prompt for {platform.value}")
            platform_prompt = PLATFORM_PROMPTS.get(platform, PLATFORM_PROMPTS[PlatformType.LINKEDIN])

        logger.info(f"Final system prompt length: {len(system_prompt) + len(platform_prompt)} characters")
        return ChatPromptTemplate.from_messages([
            ("system", system_prompt + "\n\n" + platform_prompt),
            MessagesPlaceholder(variable_name="history"),
            ("human", "{input}"),
        ])

    def get_history(self, request_id: str) -> list:
        """Get conversation history for a request."""
        return self._conversation_history.get(request_id, [])

    def set_history(self, request_id: str, history: list) -> None:
        """Set conversation history for a request."""
        self._conversation_history[request_id] = history

    def _add_to_history(
        self, request_id: str, human_msg: str, ai_msg: str
    ) -> None:
        """Add messages to conversation history."""
        if request_id not in self._conversation_history:
            self._conversation_history[request_id] = []

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
        """Enrich a thought for a specific platform.

        Args:
            request: The original thought request
            platform: Target social media platform
            additional_context: Optional additional instructions

        Returns:
            EnrichedContent with the enriched post content
        """
        llm = self._get_llm()
        prompt = self._get_prompt_template(platform, request)

        # Build the input message
        logger.debug(f"Building input text for {platform.value}")
        input_text = f"Original thought/topic: {request.original_thought}"
        if request.additional_instructions:
            logger.debug(f"Adding additional instructions: {request.additional_instructions}")
            input_text += f"\n\nAdditional instructions: {request.additional_instructions}"
        if additional_context:
            logger.debug(f"Adding additional context: {additional_context}")
            input_text += f"\n\nContext: {additional_context}"

        # Get conversation history for refinements
        history = self.get_history(request.request_id)
        logger.info(f"Retrieved {len(history)} messages from history for request {request.request_id}")

        # Create and run the chain
        chain = prompt | llm | StrOutputParser()

        logger.info(f"Enriching content for {platform.value}: {request.original_thought[:50]}...")

        # Retry logic for 503 Service Unavailable or other transient errors
        max_retries = 3
        retry_delay = 1.0  # Start with 1 second

        import time
        import random

        for attempt in range(max_retries + 1):
            try:
                enriched_text = chain.invoke({
                    "history": history,
                    "input": input_text,
                })
                logger.info(f"Model has returned the enriched text: {enriched_text}")
                # Store in conversation history
                self._add_to_history(request.request_id, input_text, enriched_text)
                logger.info(f"Added the enriched text to the conversation history")
                # Parse hashtags from the content
                hashtags = self._extract_hashtags(enriched_text)
                logger.info(f"Extracted the hashtags from the enriched text: {hashtags}")
                # Create the enriched content
                return EnrichedContent(
                    platform=platform,
                    body=enriched_text,
                    hashtags=hashtags,
                )
                logger.info(f"Created the enriched content: {enriched_text}")
                logger.info(f"Hashtags: {enriched_content.hashtags}")
            except Exception as e:
                # Check for 503 or if it's the last attempt
                is_503 = "503" in str(e)
                if attempt < max_retries and (is_503 or "Connection refused" in str(e)):
                    sleep_time = retry_delay * (2 ** attempt) + random.uniform(0, 1)
                    logger.warning(
                        f"Attempt {attempt + 1}/{max_retries + 1} failed for {platform.value}. "
                        f"Retrying in {sleep_time:.2f}s... Error: {e}"
                    )
                    time.sleep(sleep_time)
                else:
                    logger.error(
                        f"Error enriching content for {platform.value} after {attempt + 1} attempts: {e}", 
                        exc_info=True
                    )
                    raise

    def enrich_all_platforms(
        self, request: ThoughtRequest
    ) -> list[EnrichedContent]:
        """Enrich a thought for all requested platforms.

        Args:
            request: The thought request with selected platforms

        Returns:
            List of EnrichedContent for each platform
        """
        logger.info(f"Starting enrichment for {len(request.platforms)} platforms")
        results = []

        for platform in request.platforms:
            logger.info(f"-> Processing platform: {platform.value}")
            try:
                enriched = self.enrich_for_platform(request, platform)
                results.append(enriched)
                logger.info(f"Successfully enriched for {platform.value}")
            except Exception as e:
                logger.error(f"Failed to enrich for {platform}: {e}")
                # Continue with other platforms even if one fails

        logger.info(f"Completed enrichment. Success: {len(results)}/{len(request.platforms)}")
        return results

    def refine_content(
        self,
        request: ThoughtRequest,
        platform: PlatformType,
        refinement_instruction: str,
    ) -> EnrichedContent:
        """Refine previously generated content based on feedback.

        Args:
            request: The thought request (for dynamic prompts and history lookup)
            platform: Target platform
            refinement_instruction: What to change/improve

        Returns:
            Refined EnrichedContent
        """
        llm = self._get_llm()
        prompt = self._get_prompt_template(platform, request)
        history = self.get_history(request.request_id)

        if not history:
            # If no history, we can't refine, but we can try to re-enrich as if it's the first time
            # Or just log it and use empty history
            logger.warning(f"No history found for request {request.request_id}. Initializing with empty history.")
            history = []

        input_text = f"Please refine the content with the following feedback: {refinement_instruction}"
        logger.debug(f"Refinement input: {input_text}")

        chain = prompt | llm | StrOutputParser()

        logger.info(f"Invoking LLM for refinement of request {request.request_id} ({platform.value})")

        refined_text = chain.invoke({
            "history": history,
            "input": input_text,
        })

        logger.info(f"Refinement complete. New content length: {len(refined_text)}")
        self._add_to_history(request.request_id, input_text, refined_text)

        logger.debug("Extracting hashtags from refined content")
        hashtags = self._extract_hashtags(refined_text)
        logger.info(f"Extracted {len(hashtags)} hashtags")

        return EnrichedContent(
            platform=platform,
            body=refined_text,
            hashtags=hashtags,
        )

    def _extract_hashtags(self, text: str) -> list[str]:
        """Extract hashtags from content text."""
        import re
        hashtags = re.findall(r"#(\w+)", text)
        return list(set(hashtags))  # Remove duplicates

    def clear_history(self, request_id: str) -> None:
        """Clear conversation history for a request."""
        if request_id in self._conversation_history:
            del self._conversation_history[request_id]
