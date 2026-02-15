"""Image Generation Agent for creating social media visuals."""

import base64
import logging
from abc import ABC, abstractmethod
from typing import Optional

import httpx
from langchain_ollama import ChatOllama
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

from ..config import settings
from ..models import GeneratedImage, EnrichedContent

logger = logging.getLogger(__name__)


class ImageGenerator(ABC):
    """Abstract base class for image generators."""

    @abstractmethod
    def generate(self, prompt: str) -> GeneratedImage:
        """Generate an image from a prompt."""
        pass


class StableDiffusionGenerator(ImageGenerator):
    """Image generator using Stable Diffusion API (Automatic1111/ComfyUI)."""

    def __init__(self, api_url: Optional[str] = None):
        """Initialize the Stable Diffusion generator.

        Args:
            api_url: Stable Diffusion API URL (defaults to settings)
        """
        self.api_url = api_url or settings.stable_diffusion_url

    def generate(self, prompt: str) -> GeneratedImage:
        """Generate an image using Stable Diffusion.

        Args:
            prompt: The image generation prompt

        Returns:
            GeneratedImage with base64 encoded image
        """
        # Automatic1111 txt2img endpoint
        endpoint = f"{self.api_url}/sdapi/v1/txt2img"

        payload = {
            "prompt": prompt,
            "negative_prompt": "blurry, low quality, distorted, watermark, text, logo",
            "steps": 30,
            "width": 1024,
            "height": 1024,
            "cfg_scale": 7,
            "sampler_name": "DPM++ 2M Karras",
        }

        try:
            with httpx.Client(timeout=120.0) as client:
                response = client.post(endpoint, json=payload)
                response.raise_for_status()

                result = response.json()
                image_base64 = result["images"][0]

                return GeneratedImage(
                    image_base64=image_base64,
                    image_format="png",
                    prompt_used=prompt,
                    width=1024,
                    height=1024,
                )

        except Exception as e:
            logger.error(f"Stable Diffusion generation failed: {e}")
            raise


class DalleGenerator(ImageGenerator):
    """Image generator using OpenAI DALL-E API."""

    def __init__(self, api_key: Optional[str] = None):
        """Initialize the DALL-E generator.

        Args:
            api_key: OpenAI API key (defaults to settings)
        """
        self.api_key = api_key or settings.openai_api_key
        if not self.api_key:
            raise ValueError("OpenAI API key required for DALL-E generator")

    def generate(self, prompt: str) -> GeneratedImage:
        """Generate an image using DALL-E.

        Args:
            prompt: The image generation prompt

        Returns:
            GeneratedImage with base64 encoded image
        """
        endpoint = "https://api.openai.com/v1/images/generations"

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

        payload = {
            "model": "dall-e-3",
            "prompt": prompt,
            "n": 1,
            "size": "1024x1024",
            "response_format": "b64_json",
        }

        try:
            with httpx.Client(timeout=120.0) as client:
                response = client.post(endpoint, headers=headers, json=payload)
                response.raise_for_status()

                result = response.json()
                image_base64 = result["data"][0]["b64_json"]

                return GeneratedImage(
                    image_base64=image_base64,
                    image_format="png",
                    prompt_used=prompt,
                    width=1024,
                    height=1024,
                )

        except Exception as e:
            logger.error(f"DALL-E generation failed: {e}")
            raise


class OllamaGenerator(ImageGenerator):
    """Image generator using Ollama API (experimental)."""

    def __init__(self, api_url: Optional[str] = None, model_name: Optional[str] = None):
        """Initialize the Ollama generator.

        Args:
            api_url: Ollama API URL (defaults to settings)
            model_name: Ollama model for image generation
        """
        self.api_url = api_url or settings.ollama_base_url
        self.model_name = model_name or settings.ollama_image_model

    def generate(self, prompt: str) -> GeneratedImage:
        """Generate an image using Ollama.

        Args:
            prompt: The image generation prompt

        Returns:
            GeneratedImage with base64 encoded image
        """
        # Experimental: Using Ollama's generate endpoint for image models
        endpoint = f"{self.api_url}/api/generate"

        payload = {
            "model": self.model_name,
            "prompt": prompt,
            "stream": False,
        }

        try:
            logger.info(f"Calling Ollama image generation with model: {self.model_name}")
            with httpx.Client(timeout=180.0) as client:
                response = client.post(endpoint, json=payload)
                response.raise_for_status()

                result = response.json()

                # Experimental image models in Ollama may return the image in different ways
                # Some return it in a 'response' field, some in an 'images' list
                image_base64 = None

                if "image" in result:
                    image_base64 = result["image"]
                elif "images" in result and result["images"]:
                    image_base64 = result["images"][0]
                elif "response" in result:
                    resp_text = result["response"]
                    # If the response is just the base64 string
                    if len(resp_text) > 1000 and not resp_text.startswith("{"):
                        image_base64 = resp_text.strip()
                    # If it's wrapped in a data URI
                    elif "data:image" in resp_text and "base64," in resp_text:
                        image_base64 = resp_text.split("base64,")[1].split('"')[0].split("'")[0]

                if not image_base64:
                    logger.warning(
                        f"Ollama model {self.model_name} did not return recognizable image data. "
                        "Falling back to placeholder."
                    )
                    return PlaceholderGenerator().generate(prompt)

                return GeneratedImage(
                    image_base64=image_base64,
                    image_format="png",
                    prompt_used=prompt,
                    width=1024,
                    height=1024,
                )

        except Exception as e:
            logger.error(f"Ollama image generation failed: {e}")
            raise


class PlaceholderGenerator(ImageGenerator):
    """Placeholder generator when no image service is available."""

    def generate(self, prompt: str) -> GeneratedImage:
        """Return a placeholder image.

        Args:
            prompt: The image generation prompt (stored but not used)

        Returns:
            GeneratedImage with a placeholder
        """
        # Create a simple 1x1 transparent PNG as placeholder
        # This base64 represents a 1x1 transparent PNG.
        # LinkedIn may display this as a plain colored block.
        placeholder_b64 = (
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        )

        logger.warning(
            "Using 1x1 placeholder image. If you see a plain green block on social media, "
            "it's likely because the image generator is not configured correctly or the "
            "selected model (e.g. qwen3-vl) doesn't support image generation."
        )

        return GeneratedImage(
            image_base64=placeholder_b64,
            image_format="png",
            prompt_used=prompt,
            width=1,
            height=1,
        )


class ImageGenerationAgent:
    """Agent for generating images to accompany social media content."""

    def __init__(
        self,
        generator_type: Optional[str] = None,
        model_name: Optional[str] = None,
    ):
        """Initialize the image generation agent.

        Args:
            generator_type: Type of generator ("stable_diffusion", "dalle", "ollama")
            model_name: Ollama model for prompt generation
        """
        self.generator_type = generator_type or settings.image_generator_type
        self.model_name = model_name or settings.ollama_model
        self._generator: Optional[ImageGenerator] = None
        self._prompt_llm: Optional[ChatOllama] = None

    def _get_generator(self) -> ImageGenerator:
        """Get or create the image generator."""
        if not self._generator:
            if self.generator_type == "stable_diffusion":
                try:
                    self._generator = StableDiffusionGenerator()
                except Exception:
                    logger.warning("Stable Diffusion unavailable, using placeholder")
                    self._generator = PlaceholderGenerator()
            elif self.generator_type == "dalle":
                try:
                    self._generator = DalleGenerator()
                except ValueError:
                    logger.warning("DALL-E unavailable (no API key), using placeholder")
                    self._generator = PlaceholderGenerator()
            elif self.generator_type == "ollama":
                try:
                    self._generator = OllamaGenerator()
                except Exception:
                    logger.warning("Ollama image generation unavailable, using placeholder")
                    self._generator = PlaceholderGenerator()
            else:
                self._generator = PlaceholderGenerator()

        return self._generator

    def _get_prompt_llm(self) -> ChatOllama:
        """Get or create the LLM for prompt generation."""
        if not self._prompt_llm:
            self._prompt_llm = ChatOllama(
                model=self.model_name,
                base_url=settings.ollama_base_url,
                temperature=0.8,
            )
        return self._prompt_llm

    def generate_image_prompt(self, content: EnrichedContent) -> str:
        """Generate an image prompt based on enriched content.

        Args:
            content: The enriched social media content

        Returns:
            An optimized prompt for image generation
        """
        llm = self._get_prompt_llm()

        prompt_template = ChatPromptTemplate.from_messages([
            ("system", """You are an expert at creating image generation prompts for AI art models.
Your task is to create a clear, detailed prompt that will generate a professional, 
visually appealing image suitable for social media posts.

Guidelines:
- Focus on the main concept or metaphor in the content
- Use descriptive visual language
- Specify style (e.g., "professional photography", "modern illustration", "minimalist design")
- Include lighting and mood descriptors
- Keep the prompt under 200 words
- Do NOT include text in the image
- Make it suitable for professional/business content

Respond with ONLY the image prompt, nothing else."""),
            ("human", """Create an image generation prompt for the following social media content:

{content}

Generate a prompt for a visually compelling image that represents this content."""),
        ])

        chain = prompt_template | llm | StrOutputParser()

        try:
            image_prompt = chain.invoke({"content": content.body})
            logger.info(f"Generated image prompt: {image_prompt[:100]}...")
            return image_prompt

        except Exception as e:
            logger.error(f"Error generating image prompt: {e}")
            # Fallback to a simplified prompt
            return f"Professional illustration representing: {content.body[:100]}, modern, clean, corporate style"

    def generate_for_content(self, content: EnrichedContent) -> GeneratedImage:
        """Generate an image based on enriched content.

        Args:
            content: The enriched social media content

        Returns:
            GeneratedImage for the content
        """
        # First, generate an optimized prompt
        image_prompt = self.generate_image_prompt(content)

        # Then generate the image
        generator = self._get_generator()
        return generator.generate(image_prompt)

    def generate_from_prompt(self, prompt: str) -> GeneratedImage:
        """Generate an image directly from a prompt.

        Args:
            prompt: Direct image generation prompt

        Returns:
            GeneratedImage
        """
        generator = self._get_generator()
        return generator.generate(prompt)
