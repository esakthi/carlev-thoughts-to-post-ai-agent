"""Image Generation Agent for creating social media visuals."""

import base64
import logging
import uuid
from abc import ABC, abstractmethod
from datetime import datetime
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
        """Initialize the Stable Diffusion generator."""
        self.api_url = api_url or settings.stable_diffusion_url
        logger.info(f"Initialized StableDiffusionGenerator at: {self.api_url}")

    def generate(self, prompt: str) -> GeneratedImage:
        """Generate an image using Stable Diffusion."""
        logger.info(f"SD GENERATE: Requesting image from {self.api_url}")
        logger.debug(f"SD PROMPT: '{prompt}'")

        endpoint = f"{self.api_url}/sdapi/v1/txt2img"
        logger.debug(f"SD ENDPOINT: {endpoint}")

        payload = {
            "prompt": prompt,
            "negative_prompt": "blurry, low quality, distorted, watermark, text, logo, NSFW, offensive",
            "steps": 30,
            "width": 1024,
            "height": 1024,
            "cfg_scale": 7,
            "sampler_name": "DPM++ 2M Karras",
        }
        logger.debug(f"SD PAYLOAD: {payload}")

        try:
            logger.info("SD REQUEST: Sending POST request to Automatic1111 API...")
            with httpx.Client(timeout=120.0) as client:
                response = client.post(endpoint, json=payload)

                logger.debug(f"SD RESPONSE: Received status code {response.status_code}")
                response.raise_for_status()

                result = response.json()
                # Control flow: Check for images in response
                if "images" in result and result["images"]:
                    image_base64 = result["images"][0]
                    logger.info(f"SD SUCCESS: Received image. Base64 size: {len(image_base64)} chars")
                else:
                    logger.error("SD ERROR: Response JSON does not contain expected 'images' field or list is empty")
                    logger.debug(f"SD RAW RESPONSE KEYS: {list(result.keys())}")
                    raise ValueError("No image data in Stable Diffusion response")

                img_id = str(uuid.uuid4())
                logger.info(f"SD FINISHED: Assigning internal ID {img_id}")
                return GeneratedImage(
                    id=img_id,
                    image_base64=image_base64,
                    image_format="png",
                    prompt_used=prompt,
                    width=1024,
                    height=1024,
                    created_at=datetime.utcnow()
                )

        except httpx.ConnectError as e:
            logger.error(f"SD CONNECTION FAIL: Could not connect to Stable Diffusion at {self.api_url}. Is the service running?")
            raise
        except Exception as e:
            logger.error(f"SD GENERAL FAIL: Unexpected error during generation: {e}", exc_info=True)
            raise


class DalleGenerator(ImageGenerator):
    """Image generator using OpenAI DALL-E API."""

    def __init__(self, api_key: Optional[str] = None):
        """Initialize the DALL-E generator."""
        self.api_key = api_key or settings.openai_api_key
        if not self.api_key:
            logger.error("DALLE INIT FAIL: No OpenAI API key provided in settings")
            raise ValueError("OpenAI API key required for DALL-E generator")
        logger.info("Initialized DalleGenerator for OpenAI DALL-E 3")

    def generate(self, prompt: str) -> GeneratedImage:
        """Generate an image using DALL-E."""
        logger.info("DALLE GENERATE: Requesting image from OpenAI API")
        endpoint = "https://api.openai.com/v1/images/generations"

        headers = {
            "Authorization": f"Bearer {self.api_key[:5]}...{self.api_key[-4:] if len(self.api_key) > 10 else ''}",
            "Content-Type": "application/json",
        }

        payload = {
            "model": "dall-e-3",
            "prompt": prompt,
            "n": 1,
            "size": "1024x1024",
            "response_format": "b64_json",
        }
        logger.debug(f"DALLE PAYLOAD: {payload}")

        try:
            with httpx.Client(timeout=120.0) as client:
                logger.info("DALLE REQUEST: Sending POST to OpenAI...")
                response = client.post(endpoint, headers={"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"}, json=payload)

                logger.debug(f"DALLE RESPONSE: Received status code {response.status_code}")
                response.raise_for_status()

                result = response.json()

                # Control flow: Check DALLE data format
                if "data" in result and len(result["data"]) > 0 and "b64_json" in result["data"][0]:
                    image_base64 = result["data"][0]["b64_json"]
                    logger.info(f"DALLE SUCCESS: Image received. Size: {len(image_base64)} chars")
                else:
                    logger.error("DALLE ERROR: Unexpected response format from OpenAI")
                    logger.debug(f"DALLE RAW RESPONSE: {result}")
                    raise ValueError("Failed to extract image from DALL-E response")

                return GeneratedImage(
                    id=str(uuid.uuid4()),
                    image_base64=image_base64,
                    image_format="png",
                    prompt_used=prompt,
                    width=1024,
                    height=1024,
                    created_at=datetime.utcnow()
                )

        except Exception as e:
            logger.error(f"DALLE FAIL: {e}", exc_info=True)
            raise


class OllamaGenerator(ImageGenerator):
    """Image generator using Ollama API (experimental)."""

    def __init__(self, api_url: Optional[str] = None, model_name: Optional[str] = None):
        """Initialize the Ollama generator."""
        self.api_url = api_url or settings.ollama_base_url
        self.model_name = model_name or settings.ollama_image_model
        logger.info(f"Initialized OllamaGenerator (Experimental) at {self.api_url} with model {self.model_name}")

    def generate(self, prompt: str) -> GeneratedImage:
        """Generate an image using Ollama."""
        endpoint = f"{self.api_url}/api/generate"
        logger.info(f"OLLAMA IMAGE: Calling Ollama API generate for model {self.model_name}")

        payload = {
            "model": self.model_name,
            "prompt": prompt,
            "stream": False,
        }

        try:
            with httpx.Client(timeout=180.0) as client:
                response = client.post(endpoint, json=payload)
                logger.debug(f"OLLAMA RESPONSE: HTTP Status {response.status_code}")
                response.raise_for_status()

                result = response.json()
                image_base64 = None

                # Control Logic: Heuristic image extraction from experimental Ollama models
                logger.info(f"OLLAMA ANALYZE: Searching for image data in result keys: {list(result.keys())}")

                if "image" in result:
                    logger.info("Decision: Found direct 'image' field in response")
                    image_base64 = result["image"]
                elif "images" in result and result["images"]:
                    logger.info(f"Decision: Found 'images' list with {len(result['images'])} items")
                    image_base64 = result["images"][0]
                elif "response" in result:
                    logger.info("Decision: Searching for base64 patterns in 'response' text field")
                    resp_text = result["response"].strip()
                    if "data:image" in resp_text and "base64," in resp_text:
                        logger.info("Matched Data URI pattern (data:image/...;base64,)")
                        image_base64 = resp_text.split("base64,")[1].split('"')[0].split("'")[0]
                    elif len(resp_text) > 1000 and " " not in resp_text:
                        logger.info("Text block > 1000 chars with no spaces. Assuming raw base64 string.")
                        image_base64 = resp_text

                if not image_base64:
                    logger.warning(f"OLLAMA FAIL: No recognizable image found in model '{self.model_name}' response.")
                    logger.info("Decision: Falling back to PlaceholderGenerator")
                    return PlaceholderGenerator().generate(prompt)

                logger.info(f"OLLAMA SUCCESS: Image extracted. Length: {len(image_base64)}")
                return GeneratedImage(
                    id=str(uuid.uuid4()),
                    image_base64=image_base64,
                    image_format="png",
                    prompt_used=prompt,
                    width=1024,
                    height=1024,
                    created_at=datetime.utcnow()
                )

        except Exception as e:
            logger.error(f"OLLAMA EXCEPTION: {e}", exc_info=True)
            raise


class PlaceholderGenerator(ImageGenerator):
    """Placeholder generator when no image service is available."""

    def generate(self, prompt: str) -> GeneratedImage:
        """Return a placeholder image."""
        logger.info("PLACEHOLDER: Generating 1x1 transparent PNG fallback")
        placeholder_b64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="

        return GeneratedImage(
            id=str(uuid.uuid4()),
            image_base64=placeholder_b64,
            image_format="png",
            prompt_used=prompt,
            width=1,
            height=1,
            created_at=datetime.utcnow()
        )


class ImageGenerationAgent:
    """Agent for generating images to accompany social media content."""

    def __init__(self, generator_type: Optional[str] = None, model_name: Optional[str] = None):
        """Initialize the image generation agent."""
        self.generator_type = generator_type or settings.image_generator_type
        self.model_name = model_name or settings.ollama_model
        self._generator: Optional[ImageGenerator] = None
        self._prompt_llm: Optional[ChatOllama] = None
        logger.info(f"Initialized ImageGenerationAgent: type='{self.generator_type}', llm='{self.model_name}'")

    def _get_generator(self) -> ImageGenerator:
        """Get or create the image generator."""
        if not self._generator:
            logger.info(f"FACTORY: Initializing image generator instance for type '{self.generator_type}'")

            # Control flow for generator selection
            if self.generator_type == "stable_diffusion":
                try:
                    self._generator = StableDiffusionGenerator()
                except Exception as e:
                    logger.warning(f"FACTORY FAIL: Stable Diffusion setup error: {e}. Falling back to Placeholder.")
                    self._generator = PlaceholderGenerator()
            elif self.generator_type == "dalle":
                try:
                    self._generator = DalleGenerator()
                except ValueError as e:
                    logger.warning(f"FACTORY FAIL: DALL-E setup error: {e}. Falling back to Placeholder.")
                    self._generator = PlaceholderGenerator()
            elif self.generator_type == "ollama":
                try:
                    self._generator = OllamaGenerator()
                except Exception as e:
                    logger.warning(f"FACTORY FAIL: Ollama image model error: {e}. Falling back to Placeholder.")
                    self._generator = PlaceholderGenerator()
            else:
                logger.info(f"FACTORY DECISION: Generator type '{self.generator_type}' unknown or none. Using Placeholder.")
                self._generator = PlaceholderGenerator()

        return self._generator

    def _get_prompt_llm(self) -> ChatOllama:
        """Get or create the LLM for prompt generation."""
        if not self._prompt_llm:
            logger.info(f"FACTORY: Initializing Ollama LLM for prompt optimization: {self.model_name}")
            self._prompt_llm = ChatOllama(
                model=self.model_name,
                base_url=settings.ollama_base_url,
                temperature=0.8,
            )
        return self._prompt_llm

    def generate_image_prompt(self, content: str, refinement_instructions: Optional[str] = None) -> str:
        """Generate or refine an image prompt based on content and feedback."""
        logger.info("PROMPT GEN: Optimizing image prompt from content...")
        llm = self._get_prompt_llm()

        system_msg = """You are an expert at creating image generation prompts for AI art models.
Your task is to create a clear, detailed prompt that will generate a professional, 
visually appealing image suitable for social media posts.

Guidelines:
- Focus on the main concept or metaphor in the content
- Use descriptive visual language
- Specify style (e.g., "professional photography", "modern illustration")
- Include lighting and mood descriptors
- Keep the prompt under 200 words
- Do NOT include text in the image
- Make it suitable for professional/business content
- If refinement instructions are provided, incorporate them into the prompt.

Respond with ONLY the image prompt, nothing else."""

        human_msg = f"Create an image generation prompt for the following social media content:\n\n{content}"

        # Control Logic: Refinement decision
        if refinement_instructions:
            logger.info(f"PROMPT GEN: Incorporating refinement feedback: '{refinement_instructions}'")
            human_msg += f"\n\nRefine the prompt with these specific instructions: {refinement_instructions}"
        else:
            logger.info("PROMPT GEN: Generating initial prompt from scratch")

        prompt_template = ChatPromptTemplate.from_messages([
            ("system", system_msg),
            ("human", human_msg),
        ])

        chain = prompt_template | llm | StrOutputParser()

        try:
            logger.info("PROMPT GEN: Invoking Ollama LLM...")
            image_prompt = chain.invoke({})
            logger.info(f"PROMPT GEN SUCCESS: Final prompt length: {len(image_prompt)} chars")
            logger.debug(f"PROMPT GEN RESULT: '{image_prompt}'")
            return image_prompt
        except Exception as e:
            logger.error(f"PROMPT GEN FAIL: {e}. Decision: Using simple fallback prompt.")
            return f"Professional illustration representing: {content[:100]}, modern style"

    def generate_for_content(self, content: EnrichedContent, refinement_instructions: Optional[str] = None) -> GeneratedImage:
        """Generate an image based on enriched content."""
        logger.info(f"AGENT FLOW: Starting image generation for platform {content.platform.value}")

        # Logic: First get prompt
        image_prompt = self.generate_image_prompt(content.body, refinement_instructions)

        # Logic: Then dispatch to generator
        generator = self._get_generator()
        logger.info(f"AGENT FLOW: Dispatching to generator backend: {generator.__class__.__name__}")

        generated = generator.generate(image_prompt)
        logger.info(f"AGENT FLOW: Image generation complete (ID: {generated.id})")
        return generated
