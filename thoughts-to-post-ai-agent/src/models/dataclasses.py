"""Data classes for the AI Agent using Pydantic for validation."""

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field


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
    """Input request from Kafka containing the thought to enrich."""

    request_id: str = Field(..., description="Unique request identifier")
    user_id: str = Field(..., description="ID of the user making the request")
    original_thought: str = Field(..., description="The original thought/topic from user")
    platforms: list[PlatformType] = Field(
        default_factory=list, description="Selected social media platforms"
    )
    additional_instructions: Optional[str] = Field(
        default=None, description="Additional instructions for enrichment"
    )
    version: int = Field(default=1, description="Request version for refinements")
    created_at: datetime = Field(default_factory=datetime.utcnow)


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
