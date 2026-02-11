"""
Example: How to use Ollama with your configured settings

This file shows practical examples of how to use the qwen3-vl:235b-cloud model
in your content enrichment and image generation agents.
"""

import requests
from src.config import settings


# ============================================================================
# EXAMPLE 1: Text Generation (Content Enrichment Agent)
# ============================================================================

def generate_content(prompt: str, max_tokens: int = 100) -> str:
    """
    Generate text content using the configured Ollama model.

    Args:
        prompt: The input prompt for the model
        max_tokens: Maximum tokens to generate (optional)

    Returns:
        Generated text
    """

    # Use settings loaded from .env
    url = f"{settings.ollama_base_url}/api/generate"
    model = settings.ollama_model  # "qwen3-vl:235b-cloud"

    payload = {
        "model": model,
        "prompt": prompt,
        "stream": False,
        "temperature": 0.7,  # Adjust creativity (0.0 = deterministic, 1.0 = creative)
    }

    try:
        response = requests.post(url, json=payload, timeout=120)
        response.raise_for_status()

        result = response.json()
        return result.get("response", "")

    except requests.exceptions.RequestException as e:
        print(f"Error calling Ollama: {e}")
        return ""


# ============================================================================
# EXAMPLE 2: Image Understanding (Vision-Language Model)
# ============================================================================

def analyze_image(image_base64: str, question: str) -> str:
    """
    Use the qwen3-vl model to analyze an image and answer a question about it.

    Args:
        image_base64: Base64-encoded image data
        question: Question about the image

    Returns:
        Analysis or answer about the image
    """

    url = f"{settings.ollama_base_url}/api/generate"
    model = settings.ollama_image_model  # "qwen3-vl:235b-cloud"

    # Ollama accepts image data via URL or base64
    # Format: [image_data] in the prompt indicates image content
    prompt = f"[Image: {image_base64}]\n{question}"

    payload = {
        "model": model,
        "prompt": prompt,
        "stream": False,
    }

    try:
        response = requests.post(url, json=payload, timeout=120)
        response.raise_for_status()

        result = response.json()
        return result.get("response", "")

    except requests.exceptions.RequestException as e:
        print(f"Error analyzing image: {e}")
        return ""


# ============================================================================
# EXAMPLE 3: Streaming Responses (for real-time output)
# ============================================================================

def generate_content_streaming(prompt: str):
    """
    Generate text using streaming (real-time output as it's generated).

    Args:
        prompt: The input prompt for the model

    Yields:
        Chunks of generated text as they arrive
    """

    url = f"{settings.ollama_base_url}/api/generate"
    model = settings.ollama_model

    payload = {
        "model": model,
        "prompt": prompt,
        "stream": True,  # Enable streaming
    }

    try:
        response = requests.post(url, json=payload, timeout=120, stream=True)
        response.raise_for_status()

        # Process streaming response (newline-delimited JSON)
        for line in response.iter_lines():
            if line:
                try:
                    chunk = response.json() if isinstance(line, dict) else __import__('json').loads(line)
                    text = chunk.get("response", "")
                    if text:
                        yield text
                except:
                    pass

    except requests.exceptions.RequestException as e:
        print(f"Error in streaming: {e}")


# ============================================================================
# EXAMPLE 4: Checking Model Status
# ============================================================================

def check_model_available() -> bool:
    """
    Check if the configured model is available in Ollama.

    Returns:
        True if model is available, False otherwise
    """

    try:
        url = f"{settings.ollama_base_url}/api/tags"
        response = requests.get(url, timeout=10)
        response.raise_for_status()

        models = response.json().get("models", [])
        model_names = [m.get("name") for m in models]

        is_available = settings.ollama_model in model_names

        print(f"Model '{settings.ollama_model}' available: {is_available}")
        print(f"Installed models: {model_names}")

        return is_available

    except Exception as e:
        print(f"Error checking model status: {e}")
        return False


# ============================================================================
# EXAMPLE 5: Integration with Your Content Enrichment Agent
# ============================================================================

class ContentEnrichmentExample:
    """
    Example of how to integrate Ollama with your content enrichment agent.
    """

    def __init__(self):
        self.model_url = settings.ollama_base_url
        self.model_name = settings.ollama_model

    def enrich_thought(self, thought: str) -> dict:
        """
        Take a user's thought and enrich it with AI-generated content.
        """

        enriched_prompt = f"""
Enhance and expand on this thought to make it more suitable for social media posting:

Original thought: {thought}

Please provide:
1. A polished version (2-3 sentences)
2. Key hashtags (3-5 relevant ones)
3. Engagement question (1 question to ask your audience)
"""

        content = generate_content(enriched_prompt, max_tokens=200)

        return {
            "original": thought,
            "enriched": content,
            "model_used": self.model_name,
            "status": "success" if content else "failed"
        }


# ============================================================================
# EXAMPLE 6: Integration with Your Image Generation Agent
# ============================================================================

class ImageGenerationExample:
    """
    Example of how to use the image generation or analysis capabilities.
    """

    def __init__(self):
        self.generator_type = settings.image_generator_type  # "ollama"
        self.model_name = settings.ollama_image_model
        self.url = settings.ollama_base_url

    def describe_image(self, image_base64: str) -> str:
        """
        Generate a description of an image using the vision-language model.
        """

        if self.generator_type == "ollama":
            return analyze_image(image_base64, "Describe this image in detail.")
        else:
            print(f"Image generator type '{self.generator_type}' not yet implemented")
            return ""


# ============================================================================
# USAGE IN YOUR MAIN APP
# ============================================================================

if __name__ == "__main__":

    # Print current configuration
    print(f"Ollama URL: {settings.ollama_base_url}")
    print(f"Text Model: {settings.ollama_model}")
    print(f"Image Model: {settings.ollama_image_model}")
    print(f"Image Generator Type: {settings.image_generator_type}")
    print()

    # Check if model is available
    print("Checking model availability...")
    if check_model_available():
        print("✓ Model is ready to use!\n")

        # Example 1: Generate content
        print("=== Example 1: Text Generation ===")
        thought = "AI is transforming how we work and create"
        result = generate_content(f"Expand on this thought: {thought}")
        print(f"Generated: {result[:200]}...\n")

        # Example 2: Content enrichment
        print("=== Example 2: Content Enrichment ===")
        enricher = ContentEnrichmentExample()
        enriched = enricher.enrich_thought("Just finished reading an amazing AI paper!")
        print(f"Enriched content: {enriched['enriched'][:200]}...\n")

        # Example 3: Streaming (partial output)
        print("=== Example 3: Streaming Response ===")
        print("Generating streaming response... (showing first 3 chunks)")
        count = 0
        for chunk in generate_content_streaming("Write a haiku about technology"):
            if chunk:
                print(chunk, end="", flush=True)
                count += 1
                if count >= 10:  # Just show a few chunks as example
                    print("\n... [truncated for example]\n")
                    break

    else:
        print("✗ Model not available. Make sure Ollama is running and model is installed.")

