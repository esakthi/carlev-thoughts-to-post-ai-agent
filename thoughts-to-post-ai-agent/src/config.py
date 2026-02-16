"""Configuration management using Pydantic Settings."""

from pydantic_settings import BaseSettings
from pydantic import Field
from typing import Literal


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # Kafka Configuration
    kafka_bootstrap_servers: str = Field(
        default="localhost:9092",
        description="Kafka bootstrap servers"
    )
    kafka_request_topic: str = Field(
        default="thoughts-post-request",
        description="Topic to consume enrichment requests from"
    )
    kafka_response_topic: str = Field(
        default="thoughts-to-post-response",
        description="Topic to publish enrichment responses to"
    )
    kafka_search_request_topic: str = Field(
        default="thoughts-search-request",
        description="Topic to consume search requests from"
    )
    kafka_search_response_topic: str = Field(
        default="thoughts-search-response",
        description="Topic to publish search responses to"
    )
    kafka_consumer_group: str = Field(
        default="ai-agent-group",
        description="Kafka consumer group ID"
    )

    # Ollama Configuration
    ollama_base_url: str = Field(
        default="http://localhost:11434",
        description="Ollama API base URL"
    )
    ollama_model: str = Field(
        default="qwen3-vl:235b-cloud",
        description="Ollama model for text generation"
    )
    ollama_image_model: str = Field(
        default="qwen3-vl:235b-cloud",
        description="Ollama model for image understanding"
    )

    # Image Generation Configuration
    image_generator_type: Literal["ollama", "stable_diffusion", "dalle"] = Field(
        default="stable_diffusion",
        description="Type of image generator to use"
    )
    stable_diffusion_url: str = Field(
        default="http://localhost:7860",
        description="Stable Diffusion API URL"
    )
    openai_api_key: str | None = Field(
        default=None,
        description="OpenAI API key for DALL-E"
    )

    # Logging
    log_level: str = Field(default="INFO", description="Logging level")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


# Global settings instance
settings = Settings()
