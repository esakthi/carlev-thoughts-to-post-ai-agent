"""Data classes for the AI Agent using Pydantic for validation."""

import logging
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
    COMPLETED = "completed"
    FAILED = "failed"


class ThoughtRequest(BaseModel):
    """Input request from Kafka containing the thought to enrich.

    Note:
        The upstream Kafka message uses camelCase keys and uppercase platform names.
        Field aliases and validators are used here to adapt that payload into our
        internal snake_case + enum-based model.
    """

    # Map camelCase JSON keys from Kafka to our internal snake_case fields
    request_id: str = Field(
        ...,
        alias="requestId",
        description="Unique request identifier",
    )
    user_id: str = Field(
        ...,
        alias="userId",
        description="ID of the user making the request",
    )
    original_thought: str = Field(
        ...,
        alias="originalThought",
        description="The original thought/topic from user",
    )
    platforms: list[PlatformType] = Field(
        default_factory=list,
        alias="platforms",
        description="Selected social media platforms",
    )
    additional_instructions: Optional[str] = Field(
        default=None,
        alias="additionalInstructions",
        description="Additional instructions for enrichment",
    )
    system_prompt: Optional[str] = Field(
        default=None,
        alias="systemPrompt",
        description="Dynamic system prompt from the category",
    )
    category_description: Optional[str] = Field(
        default=None,
        alias="categoryDescription",
        description="Description of the thought category",
    )
    platform_prompts: dict[PlatformType, str] = Field(
        default_factory=dict,
        alias="platformPrompts",
        description="Platform-specific prompts from the database",
    )
    version: int = Field(
        default=1,
        alias="version",
        description="Request version for refinements",
    )
    created_at: datetime = Field(
        default_factory=datetime.utcnow,
        alias="createdAt",
        description="Request creation time",
    )

    # Allow population using either field names or aliases
    class Config:
        allow_population_by_field_name = True
        populate_by_name = True

    @field_validator("platforms", mode="before")
    @classmethod
    def normalize_platforms(cls, value):
        """Normalize platform names from incoming payload.

        Accepts values like ["LINKEDIN"] and converts them to lowercase so they
        match the PlatformType enum values ("linkedin", "facebook", "instagram").
        """
        if value is None:
            return []

        # Allow single string or enum as well as list
        if isinstance(value, (str, PlatformType)):
            value = [value]

        normalized: list[str | PlatformType] = []
        for item in value:
            if isinstance(item, PlatformType):
                normalized.append(item)
            elif isinstance(item, str):
                normalized.append(item.lower())
            else:
                normalized.append(str(item).lower())

        return normalized

    @field_validator("created_at", mode="before")
    @classmethod
    def parse_created_at(cls, value):
        """Handle createdAt coming in as ISO-8601 string or array format.
        
        The Kafka producer now sends dates as ISO-8601 strings (e.g., "2026-02-09T21:19:05.975").
        This validator handles both the new ISO-8601 format and the legacy array format
        for backward compatibility.
        """
        if value is None:
            return value

        # Handle ISO-8601 string format (new format from Java producer)
        if isinstance(value, str):
            try:
                # Normalize 'Z' suffix to '+00:00' for UTC timezone
                normalized = value.replace('Z', '+00:00')
                # Handle formats like:
                # - "2026-02-09T21:19:05.975" (with microseconds)
                # - "2026-02-09T21:19:05" (without microseconds)
                # - "2026-02-09T21:19:05.975Z" (with Z suffix)
                # - "2026-02-09T21:19:05+00:00" (with timezone)
                return datetime.fromisoformat(normalized)
            except (ValueError, AttributeError) as e:
                # If parsing fails, log and let Pydantic handle it
                logger = logging.getLogger(__name__)
                logger.warning(f"Failed to parse ISO-8601 date '{value}': {e}")
                return value

        # Handle legacy array format [year, month, day, hour, min, sec, ...] for backward compatibility
        if isinstance(value, list) and len(value) >= 6:
            try:
                # Ignore sub-second component if present
                return datetime(*value[:6])
            except Exception:
                # Fallback to default parsing if this fails
                return value

        # If it's already a datetime, return as-is
        if isinstance(value, datetime):
            return value

        # Let Pydantic handle other formats
        return value


class EnrichedContent(BaseModel):
    """AI-generated enriched content for a specific platform."""

    platform: PlatformType = Field(..., description="Target social media platform")
    title: Optional[str] = Field(default=None, description="Post title (if applicable)")
    body: str = Field(..., description="Main post content")
    hashtags: list[str] = Field(default_factory=list, description="Suggested hashtags")
    call_to_action: Optional[str] = Field(default=None, description="Call to action text")
    character_count: int = Field(default=0, description="Character count of the body")

    def model_post_init(self, __context) -> None:
        """Calculate character count after initialization."""
        self.character_count = len(self.body)


class GeneratedImage(BaseModel):
    """Generated image representation."""

    image_base64: str = Field(..., description="Base64 encoded image data")
    image_format: str = Field(default="png", description="Image format (png, jpg)")
    prompt_used: str = Field(..., description="The prompt used to generate the image")
    width: int = Field(default=1024, description="Image width in pixels")
    height: int = Field(default=1024, description="Image height in pixels")


@dataclass
class AgentContext:
    """Full context for tracking agent conversation and processing state."""

    request_id: str
    user_id: str
    original_thought: str
    platforms: list[PlatformType]
    system_prompt: Optional[str] = None
    category_description: Optional[str] = None
    platform_prompts: dict[PlatformType, str] = field(default_factory=dict)
    enriched_contents: list[EnrichedContent] = field(default_factory=list)
    generated_image: Optional[GeneratedImage] = None
    conversation_history: list[dict] = field(default_factory=list)
    refinement_requests: list[str] = field(default_factory=list)
    current_version: int = 1
    status: RequestStatus = RequestStatus.PENDING
    error_message: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.utcnow)
    updated_at: datetime = field(default_factory=datetime.utcnow)

    def add_refinement(self, instruction: str) -> None:
        """Add a refinement request to the context."""
        self.refinement_requests.append(instruction)
        self.current_version += 1
        self.updated_at = datetime.utcnow()

    def update_status(self, status: RequestStatus, error: Optional[str] = None) -> None:
        """Update the processing status."""
        self.status = status
        self.error_message = error
        self.updated_at = datetime.utcnow()


class AgentResponse(BaseModel):
    """Response to send back via Kafka after processing."""

    request_id: str = Field(..., description="Original request identifier")
    user_id: str = Field(..., description="User ID from the original request")
    status: RequestStatus = Field(..., description="Processing status")
    enriched_contents: list[EnrichedContent] = Field(
        default_factory=list, description="Enriched content for each platform"
    )
    generated_image: Optional[GeneratedImage] = Field(
        default=None, description="Generated image if available"
    )
    version: int = Field(default=1, description="Response version")
    error_message: Optional[str] = Field(default=None, description="Error message if failed")
    processed_at: datetime = Field(default_factory=datetime.utcnow)

    class Config:
        use_enum_values = True
