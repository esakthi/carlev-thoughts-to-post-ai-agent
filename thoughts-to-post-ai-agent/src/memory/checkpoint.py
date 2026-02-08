"""Checkpoint Memory for tracking agent context and history."""

import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Optional
from dataclasses import asdict

from langgraph.checkpoint.memory import MemorySaver

from ..models import AgentContext, ThoughtRequest, EnrichedContent, GeneratedImage, RequestStatus

logger = logging.getLogger(__name__)


class CheckpointMemory:
    """Manages checkpoint memory for tracking agent processing state.

    Uses LangGraph's MemorySaver for in-memory checkpointing with optional
    file-based persistence for durability.
    """

    def __init__(self, persist_dir: Optional[str] = None):
        """Initialize checkpoint memory.

        Args:
            persist_dir: Optional directory for persisting checkpoints to disk
        """
        self._memory_saver = MemorySaver()
        self._contexts: dict[str, AgentContext] = {}
        self._persist_dir = Path(persist_dir) if persist_dir else None

        if self._persist_dir:
            self._persist_dir.mkdir(parents=True, exist_ok=True)
            self._load_persisted_contexts()

    def _load_persisted_contexts(self) -> None:
        """Load previously persisted contexts from disk."""
        if not self._persist_dir:
            return

        for file_path in self._persist_dir.glob("*.json"):
            try:
                with open(file_path, "r") as f:
                    data = json.load(f)
                    # Reconstruct AgentContext from persisted data
                    context = self._dict_to_context(data)
                    self._contexts[context.request_id] = context
                    logger.info(f"Loaded persisted context: {context.request_id}")
            except Exception as e:
                logger.warning(f"Failed to load persisted context {file_path}: {e}")

    def _persist_context(self, context: AgentContext) -> None:
        """Persist a context to disk."""
        if not self._persist_dir:
            return

        file_path = self._persist_dir / f"{context.request_id}.json"
        try:
            data = self._context_to_dict(context)
            with open(file_path, "w") as f:
                json.dump(data, f, indent=2, default=str)
        except Exception as e:
            logger.warning(f"Failed to persist context {context.request_id}: {e}")

    def _context_to_dict(self, context: AgentContext) -> dict:
        """Convert AgentContext to a serializable dictionary."""
        data = asdict(context)
        # Convert enums and complex types
        data["platforms"] = [p.value for p in context.platforms]
        data["status"] = context.status.value
        data["enriched_contents"] = [c.model_dump() for c in context.enriched_contents]
        if context.generated_image:
            data["generated_image"] = context.generated_image.model_dump()
        data["created_at"] = context.created_at.isoformat()
        data["updated_at"] = context.updated_at.isoformat()
        return data

    def _dict_to_context(self, data: dict) -> AgentContext:
        """Convert a dictionary back to AgentContext."""
        from ..models import PlatformType

        # Reconstruct complex types
        platforms = [PlatformType(p) for p in data.get("platforms", [])]
        status = RequestStatus(data.get("status", "pending"))
        enriched_contents = [
            EnrichedContent(**c) for c in data.get("enriched_contents", [])
        ]
        generated_image = None
        if data.get("generated_image"):
            generated_image = GeneratedImage(**data["generated_image"])

        return AgentContext(
            request_id=data["request_id"],
            user_id=data["user_id"],
            original_thought=data["original_thought"],
            platforms=platforms,
            enriched_contents=enriched_contents,
            generated_image=generated_image,
            conversation_history=data.get("conversation_history", []),
            refinement_requests=data.get("refinement_requests", []),
            current_version=data.get("current_version", 1),
            status=status,
            error_message=data.get("error_message"),
            created_at=datetime.fromisoformat(data["created_at"]),
            updated_at=datetime.fromisoformat(data["updated_at"]),
        )

    def create_context(self, request: ThoughtRequest) -> AgentContext:
        """Create a new context from a thought request.

        Args:
            request: The incoming thought request

        Returns:
            New AgentContext for tracking the request
        """
        context = AgentContext(
            request_id=request.request_id,
            user_id=request.user_id,
            original_thought=request.original_thought,
            platforms=request.platforms,
            current_version=request.version,
        )

        self._contexts[request.request_id] = context
        self._persist_context(context)

        logger.info(f"Created context for request: {request.request_id}")
        return context

    def get_context(self, request_id: str) -> Optional[AgentContext]:
        """Get an existing context by request ID.

        Args:
            request_id: The request ID to look up

        Returns:
            AgentContext if found, None otherwise
        """
        return self._contexts.get(request_id)

    def update_context(
        self,
        request_id: str,
        enriched_contents: Optional[list[EnrichedContent]] = None,
        generated_image: Optional[GeneratedImage] = None,
        status: Optional[RequestStatus] = None,
        error_message: Optional[str] = None,
    ) -> Optional[AgentContext]:
        """Update an existing context.

        Args:
            request_id: The request ID to update
            enriched_contents: New enriched contents to set
            generated_image: New generated image to set
            status: New status to set
            error_message: Error message if status is FAILED

        Returns:
            Updated AgentContext if found, None otherwise
        """
        context = self._contexts.get(request_id)
        if not context:
            logger.warning(f"Context not found: {request_id}")
            return None

        if enriched_contents is not None:
            context.enriched_contents = enriched_contents

        if generated_image is not None:
            context.generated_image = generated_image

        if status is not None:
            context.update_status(status, error_message)

        context.updated_at = datetime.utcnow()
        self._persist_context(context)

        logger.info(f"Updated context: {request_id}, status: {context.status}")
        return context

    def add_refinement(
        self, request_id: str, instruction: str
    ) -> Optional[AgentContext]:
        """Add a refinement request to a context.

        Args:
            request_id: The request ID to refine
            instruction: The refinement instruction

        Returns:
            Updated AgentContext if found, None otherwise
        """
        context = self._contexts.get(request_id)
        if not context:
            logger.warning(f"Context not found for refinement: {request_id}")
            return None

        context.add_refinement(instruction)
        self._persist_context(context)

        logger.info(
            f"Added refinement to {request_id}, version: {context.current_version}"
        )
        return context

    def delete_context(self, request_id: str) -> bool:
        """Delete a context.

        Args:
            request_id: The request ID to delete

        Returns:
            True if deleted, False if not found
        """
        if request_id not in self._contexts:
            return False

        del self._contexts[request_id]

        if self._persist_dir:
            file_path = self._persist_dir / f"{request_id}.json"
            if file_path.exists():
                file_path.unlink()

        logger.info(f"Deleted context: {request_id}")
        return True

    def list_active_contexts(self) -> list[AgentContext]:
        """List all contexts that are not completed or failed."""
        return [
            ctx
            for ctx in self._contexts.values()
            if ctx.status not in (RequestStatus.COMPLETED, RequestStatus.FAILED)
        ]
