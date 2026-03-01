"""Video Generation Agent for creating social media video content."""

import logging
import uuid
import time
from datetime import datetime
from typing import Optional

from langchain_ollama import ChatOllama
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

from ..config import settings
from ..models import EnrichedContent

logger = logging.getLogger(__name__)


class VideoGenerationAgent:
    """Agent for generating short videos to accompany social media content."""

    def __init__(self, model_name: Optional[str] = None):
        """Initialize the video generation agent."""
        self.model_name = model_name or settings.ollama_model
        self._prompt_llm: Optional[ChatOllama] = None

    def _get_prompt_llm(self) -> ChatOllama:
        """Get or create the LLM for prompt generation."""
        if not self._prompt_llm:
            self._prompt_llm = ChatOllama(
                model=self.model_name,
                base_url=settings.ollama_base_url,
                temperature=0.8,
            )
        return self._prompt_llm

    def generate_video_prompt(
        self,
        content: str,
        user_video_prompt: Optional[str] = None,
        refinement_instructions: Optional[str] = None
    ) -> str:
        """Generate or refine a video prompt based on content and user input."""
        llm = self._get_prompt_llm()

        system_msg = """You are an expert at creating video generation prompts for AI video models (like Runway, Pika, or AnimateDiff).
Your task is to create a clear, detailed motion-focused prompt that will generate a high-quality short video.

Guidelines:
- Describe the movement and action clearly
- Specify camera work (e.g., "slow zoom in", "panning shot")
- Specify style and lighting
- Keep the prompt focused on a single, impactful scene
- Do NOT include text in the video
- If a base video prompt is provided, enhance and refine it based on the post content.

Respond with ONLY the video prompt, nothing else."""

        human_msg = f"Social media content:\n\n{content}"
        if user_video_prompt:
            human_msg += f"\n\nBase video prompt to enhance: {user_video_prompt}"
        else:
            human_msg += "\n\nCreate a suitable motion/video prompt for this content."

        if refinement_instructions:
            human_msg += f"\n\nRefine the final prompt with these specific instructions: {refinement_instructions}"

        prompt_template = ChatPromptTemplate.from_messages([
            ("system", system_msg),
            ("human", human_msg),
        ])

        chain = prompt_template | llm | StrOutputParser()

        try:
            video_prompt = chain.invoke({})
            logger.info(f"Generated video prompt: {video_prompt[:100]}...")
            return video_prompt
        except Exception as e:
            logger.error(f"Error generating video prompt: {e}")
            return f"Cinematic motion representing: {content[:100]}, professional style"

    def generate_for_content(
        self,
        content: EnrichedContent,
        user_video_prompt: Optional[str] = None,
        video_params: Optional[dict] = None,
        request_id: Optional[str] = None,
    ) -> dict:
        """Simulate video generation (Cloud-based placeholder)."""
        logger.info(f"Simulating video generation for {content.platform.value} content with params: {video_params}")

        video_prompt = self.generate_video_prompt(
            content.body,
            user_video_prompt=user_video_prompt
        )

        # In a real implementation, this would call a cloud API like Runway Gen-2
        # For now, we simulate an async request to the cloud.
        logger.info(f"Submitting async video job to cloud for prompt: {video_prompt[:50]}...")
        logger.info(f"Video config: Resolution={video_params.get('resolution')}, Duration={video_params.get('duration')}s, FPS={video_params.get('fps')}")

        # Simulate some processing time or cloud interaction
        time.sleep(1)

        # Return a result that indicates a video is "processing" in the cloud
        # Since we don't have a real cloud API yet, we return a placeholder URL
        return {
            "id": str(uuid.uuid4()),
            "status": "PROCESSING",
            "cloud_provider": "Runway/ModelScope",
            "prompt_used": video_prompt,
            "video_url": "https://example.com/videos/placeholder.mp4", # Placeholder
            "created_at": datetime.utcnow().isoformat()
        }
