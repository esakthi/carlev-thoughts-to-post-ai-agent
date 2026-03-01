"""Data classes for the AI Agent using Pydantic for validation."""

import logging
import uuid
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class PlatformType(str, Enum):
    """Supported social media platforms."""

    LINKEDIN = "linkedin"
    FACEBOOK = "facebook"
    INSTAGRAM = "instagram"


class RequestStatus(str, Enum):
    """Status of an enrichment request."""

    PENDING = "pending"
    PROCESSING = "processing"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    PARTIALLY_COMPLETED = "partially_completed"
    FAILED = "failed"


class GenerationParameters(BaseModel):
    """Parameters for image/video generation."""
    resolution: Optional[str] = None
    steps: Optional[int] = None
    cfg_scale: Optional[float] = Field(default=None, alias="cfgScale")
    seed: Optional[int] = None
    model_type: Optional[str] = Field(default=None, alias="modelType")
    sampler: Optional[str] = None
    batch_size: Optional[int] = Field(default=None, alias="batchSize")
    async_mode: Optional[bool] = Field(default=None, alias="asyncMode")
    duration: Optional[int] = None
    fps: Optional[int] = None

    class Config:
        populate_by_name = True


class PlatformConfiguration(BaseModel):
    """Configuration for a specific social media platform."""
    platform: PlatformType
    prompt: Optional[str] = None
    image_prompt: Optional[str] = Field(default=None, alias="imagePrompt")
    video_prompt: Optional[str] = Field(default=None, alias="videoPrompt")
    additional_context: Optional[str] = Field(default=None, alias="additionalContext")
    image_params: Optional[GenerationParameters] = Field(default=None, alias="imageParams")
    video_params: Optional[GenerationParameters] = Field(default=None, alias="videoParams")

    class Config:
        populate_by_name = True

    @field_validator("platform", mode="before")
    @classmethod
    def normalize_platform(cls, value):
        """Normalize platform name to lowercase."""
        if value is None:
            return value
        if isinstance(value, str):
            return value.lower()
        return value


class ThoughtRequest(BaseModel):
    """Input request from Kafka containing the thought to enrich."""

    request_id: str = Field(..., alias="requestId")
    user_id: str = Field(..., alias="userId")
    original_thought: str = Field(..., alias="originalThought")
    platforms: list[PlatformType] = Field(default_factory=list)
    additional_instructions: Optional[str] = Field(default=None, alias="additionalInstructions")
    model_role: Optional[str] = Field(default=None, alias="modelRole")
    search_description: Optional[str] = Field(default=None, alias="searchDescription")
    platform_prompts: dict[PlatformType, str] = Field(default_factory=dict, alias="platformPrompts")
    platform_configurations: list[PlatformConfiguration] = Field(default_factory=list, alias="platformConfigurations")
    version: int = Field(default=1)
    created_at: datetime = Field(default_factory=datetime.utcnow, alias="createdAt")

    # New fields for image refinement
    image_refinement_instructions: Optional[str] = Field(default=None, alias="imageRefinementInstructions")
    target_platform: Optional[PlatformType] = Field(default=None, alias="targetPlatform")

    class Config:
        populate_by_name = True

    @field_validator("platforms", "target_platform", mode="before")
    @classmethod
    def normalize_platforms(cls, value):
        if value is None:
            return value if not isinstance(cls.model_fields.get("platforms"), list) else []
        if isinstance(value, (str, PlatformType)):
            if isinstance(cls.model_fields.get("platforms"), list):
                value = [value]
            else:
                return value.lower() if isinstance(value, str) else value

        if isinstance(value, list):
            return [item.lower() if isinstance(item, str) else item for item in value]
        return value

    @field_validator("platform_prompts", mode="before")
    @classmethod
    def normalize_platform_prompts(cls, value):
        if not isinstance(value, dict):
            return value
        return {k.lower() if isinstance(k, str) else k: v for k, v in value.items()}

    @field_validator("created_at", mode="before")
    @classmethod
    def parse_created_at(cls, value):
        if value is None:
            return value
        if isinstance(value, str):
            try:
                normalized = value.replace('Z', '+00:00')
                return datetime.fromisoformat(normalized)
            except (ValueError, AttributeError):
                return value
        if isinstance(value, list) and len(value) >= 6:
            try:
                return datetime(*value[:6])
            except Exception:
                return value
        return value


class GeneratedImage(BaseModel):
    """Generated image representation."""

    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    image_base64: str = Field(..., alias="image_base64")
    image_format: str = Field(default="png", alias="image_format")
    prompt_used: str = Field(..., alias="prompt_used")
    width: int = Field(default=1024)
    height: int = Field(default=1024)
    tag: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow, alias="created_at")

    class Config:
        populate_by_name = True


class EnrichedContent(BaseModel):
    """AI-generated enriched content for a specific platform."""

    platform: PlatformType = Field(...)
    title: Optional[str] = Field(default=None)
    body: str = Field(...)
    hashtags: list[str] = Field(default_factory=list)
    call_to_action: Optional[str] = Field(default=None, alias="call_to_action")
    character_count: int = Field(default=0, alias="character_count")
    images: list[GeneratedImage] = Field(default_factory=list)

    def model_post_init(self, __context) -> None:
        self.character_count = len(self.body)

    class Config:
        populate_by_name = True


@dataclass
class AgentContext:
    """Full context for tracking agent conversation and processing state."""

    request_id: str
    user_id: str
    original_thought: str
    platforms: list[PlatformType]
    model_role: Optional[str] = None
    search_description: Optional[str] = None
    platform_prompts: dict[PlatformType, str] = field(default_factory=dict)
    platform_configurations: list[PlatformConfiguration] = field(default_factory=list)
    enriched_contents: list[EnrichedContent] = field(default_factory=list)
    generated_image: Optional[GeneratedImage] = None # Legacy
    conversation_history: list[dict] = field(default_factory=list)
    refinement_requests: list[str] = field(default_factory=list)
    current_version: int = 1
    status: RequestStatus = RequestStatus.PENDING
    error_message: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.utcnow)
    updated_at: datetime = field(default_factory=datetime.utcnow)

    def add_refinement(self, instruction: str) -> None:
        self.refinement_requests.append(instruction)
        self.current_version += 1
        self.updated_at = datetime.utcnow()

    def update_status(self, status: RequestStatus, error: Optional[str] = None) -> None:
        self.status = status
        self.error_message = error
        self.updated_at = datetime.utcnow()


class AgentResponse(BaseModel):
    """Response to send back via Kafka after processing."""

    request_id: str = Field(..., alias="request_id")
    user_id: str = Field(..., alias="user_id")
    status: RequestStatus = Field(...)
    enriched_contents: list[EnrichedContent] = Field(default_factory=list, alias="enriched_contents")
    generated_image: Optional[GeneratedImage] = Field(default=None, alias="generated_image")
    failed_platforms: list[PlatformType] = Field(default_factory=list, alias="failed_platforms")
    version: int = Field(default=1)
    error_message: Optional[str] = Field(default=None, alias="error_message")
    processed_at: datetime = Field(default_factory=datetime.utcnow, alias="processed_at")

    class Config:
        populate_by_name = True
        use_enum_values = True
