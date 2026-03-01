"""Image Generation Agent for creating social media visuals."""

import base64
import logging
import os
import textwrap
import uuid
import threading
import time
from abc import ABC, abstractmethod
from datetime import datetime
from typing import Optional, Callable

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
    def generate(
        self,
        prompt: str,
        params: Optional[dict] = None,
        progress_callback: Optional[Callable[[float], None]] = None
    ) -> GeneratedImage:
        """Generate an image from a prompt."""
        pass


class PillowFallbackGenerator(ImageGenerator):
    """Fallback image generator using Pillow when no AI image service is available.

    Creates a real, readable 1024x1024 PNG with the prompt text rendered on it.
    This is a proper image file — not a 1x1 transparent pixel — so it renders
    visibly in the UI and can be opened for validation.
    """

    def generate(
        self,
        prompt: str,
        params: Optional[dict] = None,
        progress_callback: Optional[Callable[[float], None]] = None
    ) -> GeneratedImage:
        """Return a branded placeholder PNG with the prompt text rendered on it."""
        try:
            from PIL import Image, ImageDraw, ImageFont
            import io

            width, height = 1024, 1024

            # Dark gradient-style background
            img = Image.new("RGB", (width, height), color=(18, 18, 32))
            draw = ImageDraw.Draw(img)

            # Subtle border
            border = 20
            draw.rectangle(
                [border, border, width - border, height - border],
                outline=(100, 80, 200),
                width=4,
            )

            # Header banner
            draw.rectangle([border, border, width - border, 120], fill=(40, 30, 80))
            header_text = "✦ Thoughts to Post — AI Generated Image"
            try:
                font_header = ImageFont.truetype("arial.ttf", 28)
                font_prompt = ImageFont.truetype("arial.ttf", 22)
                font_footer = ImageFont.truetype("arial.ttf", 18)
            except OSError:
                font_header = ImageFont.load_default()
                font_prompt = font_header
                font_footer = font_header

            draw.text((width // 2, 68), header_text, fill=(180, 140, 255), font=font_header, anchor="mm")

            # Decorative divider line
            draw.line([(border + 10, 125), (width - border - 10, 125)], fill=(100, 80, 200), width=2)

            # Prompt label
            draw.text((width // 2, 165), "Image Prompt:", fill=(160, 160, 200), font=font_footer, anchor="mm")

            # Wrap and draw prompt text
            wrapped = textwrap.wrap(prompt, width=52)
            y = 200
            for line in wrapped[:18]:  # Max 18 lines
                draw.text((width // 2, y), line, fill=(220, 210, 255), font=font_prompt, anchor="mm")
                y += 32
                if y > 880:
                    draw.text((width // 2, y), "...", fill=(160, 160, 200), font=font_prompt, anchor="mm")
                    break

            # Footer
            draw.rectangle([border, height - 80, width - border, height - border], fill=(40, 30, 80))
            ts = datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
            draw.text(
                (width // 2, height - 52),
                f"Generated: {ts}  |  Stable Diffusion — starting up",
                fill=(120, 120, 160),
                font=font_footer,
                anchor="mm",
            )

            # Encode to base64
            buf = io.BytesIO()
            img.save(buf, format="PNG")
            image_b64 = base64.b64encode(buf.getvalue()).decode("utf-8")

            logger.info("Pillow fallback: generated 1024x1024 placeholder PNG")
            return GeneratedImage(
                id=str(uuid.uuid4()),
                image_base64=image_b64,
                image_format="png",
                prompt_used=prompt,
                width=width,
                height=height,
                created_at=datetime.utcnow(),
            )

        except ImportError:
            logger.warning("Pillow not installed — returning minimal 1x1 placeholder. Run: pip install Pillow")
            placeholder_b64 = (
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
            )
            return GeneratedImage(
                id=str(uuid.uuid4()),
                image_base64=placeholder_b64,
                image_format="png",
                prompt_used=prompt,
                width=1,
                height=1,
                created_at=datetime.utcnow(),
            )


class StableDiffusionGenerator(ImageGenerator):
    """Image generator using Stable Diffusion API (Automatic1111/ComfyUI)."""

    def __init__(self, api_url: Optional[str] = None):
        """Initialize the Stable Diffusion generator."""
        self.api_url = api_url or settings.stable_diffusion_url

    def generate(
        self,
        prompt: str,
        params: Optional[dict] = None,
        progress_callback: Optional[Callable[[float], None]] = None
    ) -> GeneratedImage:
        """Generate an image using Stable Diffusion with progress tracking.

        Falls back to PillowFallbackGenerator if SD is unreachable so the
        pipeline always returns an image even while SD is starting up.
        """
        logger.info(f"Generating image via Stable Diffusion at {self.api_url}")
        logger.debug(f"SD Prompt: {prompt}")

        endpoint = f"{self.api_url}/sdapi/v1/txt2img"

        # Default parameters
        width, height = 1024, 1024
        steps = 30
        cfg_scale = 7
        sampler = "DPM++ 2M Karras"

        if params:
            if params.get("resolution"):
                res = params["resolution"].split("x")
                if len(res) == 2:
                    width, height = int(res[0]), int(res[1])
            steps = params.get("steps") or steps
            cfg_scale = params.get("cfgScale") or params.get("cfg_scale") or cfg_scale
            sampler = params.get("sampler") or sampler

        payload = {
            "prompt": prompt,
            "negative_prompt": "blurry, low quality, distorted, watermark, text, logo, NSFW, offensive",
            "steps": steps,
            "width": width,
            "height": height,
            "cfg_scale": cfg_scale,
            "sampler_name": sampler,
        }

        # Heuristic timeout estimation
        estimated_seconds = (width * height / (512 * 512)) * (steps / 20) * 10
        timeout = max(180.0, estimated_seconds * 2.0) # Increased timeout for safety
        logger.info(f"Estimated runtime: {estimated_seconds}s. Using timeout: {timeout}s")

        # Progress tracking state
        stop_polling = threading.Event()
        last_reported_progress = 0.0

        def poll_progress():
            nonlocal last_reported_progress
            logger.debug(f"Starting progress polling for SD at {self.api_url}")
            while not stop_polling.is_set():
                try:
                    with httpx.Client(timeout=5.0) as client:
                        resp = client.get(f"{self.api_url}/sdapi/v1/progress")
                        if resp.status_code == 200:
                            data = resp.json()
                            progress = data.get("progress", 0.0) # 0.0 to 1.0

                            # Report every 10% or if it's near completion
                            if progress_callback and (progress - last_reported_progress >= 0.1 or progress > 0.95):
                                logger.info(f"SD Progress: {progress*100:.1f}%")
                                progress_callback(progress * 100)
                                last_reported_progress = progress
                except Exception as e:
                    logger.debug(f"Progress polling error (non-fatal): {e}")

                # Poll every 2 seconds
                stop_polling.wait(2.0)

        polling_thread = threading.Thread(target=poll_progress, daemon=True)
        polling_thread.start()

        try:
            if progress_callback:
                progress_callback(5.0) # Indicate start

            with httpx.Client(timeout=timeout) as client:
                logger.debug(f"Sending request to Stable Diffusion: {endpoint}")
                response = client.post(endpoint, json=payload)
                response.raise_for_status()

                result = response.json()
                image_base64 = result["images"][0]
                logger.info(f"Stable Diffusion generated image successfully. Base64 length: {len(image_base64)}")

                if progress_callback:
                    progress_callback(100.0)

                return GeneratedImage(
                    id=str(uuid.uuid4()),
                    image_base64=image_base64,
                    image_format="png",
                    prompt_used=prompt,
                    width=width,
                    height=height,
                    created_at=datetime.utcnow(),
                )

        except (httpx.ConnectError, httpx.ConnectTimeout, ConnectionRefusedError, OSError) as e:
            # SD is not running yet — use Pillow fallback so the UI still shows something
            logger.warning(
                f"Stable Diffusion unreachable ({e}). "
                f"Using Pillow fallback image. Start SD WebUI at {self.api_url} to enable real generation."
            )
            return PillowFallbackGenerator().generate(prompt)

        except Exception as e:
            logger.error(f"Stable Diffusion generation failed with unexpected error: {e}")
            raise
        finally:
            stop_polling.set()
            polling_thread.join(timeout=1.0)


class DalleGenerator(ImageGenerator):
    """Image generator using OpenAI DALL-E API."""

    def __init__(self, api_key: Optional[str] = None):
        """Initialize the DALL-E generator."""
        self.api_key = api_key or settings.openai_api_key
        if not self.api_key:
            raise ValueError("OpenAI API key required for DALL-E generator")

    def generate(
        self,
        prompt: str,
        params: Optional[dict] = None,
        progress_callback: Optional[Callable[[float], None]] = None
    ) -> GeneratedImage:
        """Generate an image using DALL-E."""
        logger.info("Generating image via OpenAI DALL-E 3")
        logger.debug(f"DALL-E Prompt: {prompt}")
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
                logger.debug("Sending request to OpenAI DALL-E")
                response = client.post(endpoint, headers=headers, json=payload)
                response.raise_for_status()

                result = response.json()
                image_base64 = result["data"][0]["b64_json"]
                logger.info(f"DALL-E generated image successfully. Base64 length: {len(image_base64)}")

                return GeneratedImage(
                    id=str(uuid.uuid4()),
                    image_base64=image_base64,
                    image_format="png",
                    prompt_used=prompt,
                    width=1024,
                    height=1024,
                    created_at=datetime.utcnow(),
                )

        except Exception as e:
            logger.error(f"DALL-E generation failed: {e}")
            raise


class OllamaGenerator(ImageGenerator):
    """Image generator using Ollama API (experimental)."""

    def __init__(self, api_url: Optional[str] = None, model_name: Optional[str] = None):
        """Initialize the Ollama generator."""
        self.api_url = api_url or settings.ollama_base_url
        self.model_name = model_name or settings.ollama_image_model

    def generate(
        self,
        prompt: str,
        params: Optional[dict] = None,
        progress_callback: Optional[Callable[[float], None]] = None
    ) -> GeneratedImage:
        """Generate an image using Ollama."""
        endpoint = f"{self.api_url}/api/generate"

        payload = {
            "model": self.model_name,
            "prompt": prompt,
            "stream": False,
        }

        try:
            logger.info(f"Calling Ollama image generation with model: {self.model_name}")
            logger.debug(f"Ollama Prompt: {prompt}")
            with httpx.Client(timeout=180.0) as client:
                response = client.post(endpoint, json=payload)
                response.raise_for_status()

                result = response.json()
                image_base64 = None

                if "image" in result:
                    image_base64 = result["image"]
                elif "images" in result and result["images"]:
                    image_base64 = result["images"][0]
                elif "response" in result:
                    resp_text = result["response"].strip()
                    if "data:image" in resp_text and "base64," in resp_text:
                        image_base64 = resp_text.split("base64,")[1].split('"')[0].split("'")[0]
                    elif len(resp_text) > 1000 and " " not in resp_text:
                        image_base64 = resp_text

                if not image_base64:
                    logger.warning(f"Ollama model {self.model_name} did not return image data.")
                    return PillowFallbackGenerator().generate(prompt)

                return GeneratedImage(
                    id=str(uuid.uuid4()),
                    image_base64=image_base64,
                    image_format="png",
                    prompt_used=prompt,
                    width=1024,
                    height=1024,
                    created_at=datetime.utcnow(),
                )

        except Exception as e:
            logger.error(f"Ollama image generation failed: {e}")
            raise


def _save_image_to_disk(image: GeneratedImage, platform: str, request_id: str) -> None:
    """Save a generated image to the configured image_save_dir for validation.

    File name format: {platform}_{request_id}_{timestamp_ms}.png
    Silently skips if the directory cannot be created or writing fails.
    """
    try:
        save_dir = settings.image_save_dir
        os.makedirs(save_dir, exist_ok=True)

        # Sanitize request_id (MongoDB ObjectId or UUID) for use in filename
        safe_id = request_id.replace("-", "").replace("/", "")[:24]
        ts = datetime.utcnow().strftime("%Y%m%d_%H%M%S_%f")[:20]
        platform_slug = platform.lower().replace(" ", "_")
        filename = f"{platform_slug}_{safe_id}_{ts}.png"
        filepath = os.path.join(save_dir, filename)

        image_bytes = base64.b64decode(image.image_base64)
        with open(filepath, "wb") as f:
            f.write(image_bytes)

        logger.info(f"Saved generated image to: {filepath}")

    except Exception as e:
        logger.warning(f"Could not save image to disk (non-fatal): {e}")


class ImageGenerationAgent:
    """Agent for generating images to accompany social media content."""

    def __init__(self, generator_type: Optional[str] = None, model_name: Optional[str] = None):
        """Initialize the image generation agent."""
        self.generator_type = generator_type or settings.image_generator_type
        self.model_name = model_name or settings.ollama_model
        self._generator: Optional[ImageGenerator] = None
        self._prompt_llm: Optional[ChatOllama] = None

    def _get_generator(self) -> ImageGenerator:
        """Get or create the image generator."""
        if not self._generator:
            logger.info(f"Initializing image generator of type: {self.generator_type}")
            if self.generator_type == "stable_diffusion":
                # StableDiffusionGenerator now handles its own fallback internally
                self._generator = StableDiffusionGenerator()
            elif self.generator_type == "dalle":
                try:
                    self._generator = DalleGenerator()
                except ValueError as e:
                    logger.warning(f"DALL-E unavailable, using Pillow fallback: {e}")
                    self._generator = PillowFallbackGenerator()
            elif self.generator_type == "ollama":
                try:
                    self._generator = OllamaGenerator()
                except Exception as e:
                    logger.warning(f"Ollama image generation unavailable, using Pillow fallback: {e}")
                    self._generator = PillowFallbackGenerator()
            else:
                self._generator = PillowFallbackGenerator()
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

    def generate_image_prompt(
        self,
        content: str,
        user_image_prompt: Optional[str] = None,
        refinement_instructions: Optional[str] = None
    ) -> str:
        """Generate or refine an image prompt based on content, user base prompt, and feedback."""
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
- If a base prompt is provided, enhance it and combine it with the post content.
- If refinement instructions are provided, incorporate them into the prompt.

Respond with ONLY the enhanced image prompt, nothing else."""

        human_msg = f"Social media content:\n\n{content}"
        if user_image_prompt:
            human_msg += f"\n\nBase image prompt to enhance: {user_image_prompt}"
        else:
            human_msg += "\n\nCreate a suitable visual prompt for this content."

        if refinement_instructions:
            human_msg += f"\n\nRefine the final prompt with these specific instructions: {refinement_instructions}"

        prompt_template = ChatPromptTemplate.from_messages([
            ("system", system_msg),
            ("human", human_msg),
        ])

        chain = prompt_template | llm | StrOutputParser()

        try:
            image_prompt = chain.invoke({})
            logger.info(f"Generated/Refined image prompt: {image_prompt[:100]}...")
            return image_prompt
        except Exception as e:
            logger.error(f"Error generating image prompt: {e}")
            return f"Professional illustration representing: {content[:100]}, modern style"

    def generate_for_content(
        self,
        content: EnrichedContent,
        user_image_prompt: Optional[str] = None,
        image_params: Optional[dict] = None,
        refinement_instructions: Optional[str] = None,
        request_id: Optional[str] = None,
        progress_callback: Optional[Callable[[float], None]] = None,
    ) -> GeneratedImage:
        """Generate an image based on enriched content and save it to disk."""
        logger.info(f"Generating image for {content.platform.value} content")
        image_prompt = self.generate_image_prompt(
            content.body,
            user_image_prompt=user_image_prompt,
            refinement_instructions=refinement_instructions
        )
        generator = self._get_generator()
        image = generator.generate(image_prompt, params=image_params, progress_callback=progress_callback)

        # Save to disk for validation (non-fatal if it fails)
        _save_image_to_disk(
            image,
            platform=content.platform.value,
            request_id=request_id or image.id,
        )

        return image
