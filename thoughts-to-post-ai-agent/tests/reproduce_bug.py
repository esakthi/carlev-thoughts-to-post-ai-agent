import pytest
from datetime import datetime
from src.models import ThoughtRequest, PlatformType

def test_thought_request_uppercase_platform_prompts():
    """Test that ThoughtRequest can handle uppercase keys in platformPrompts."""
    data = {
        "requestId": "test-id",
        "userId": "user-123",
        "originalThought": "test thought",
        "platforms": ["LINKEDIN"],
        "platformPrompts": {
            "LINKEDIN": "test prompt"
        },
        "createdAt": "2026-02-16T21:06:23.963"
    }
    
    # This should not raise ValidationError after the fix
    request = ThoughtRequest(**data)
    
    assert PlatformType.LINKEDIN in request.platform_prompts
    assert request.platform_prompts[PlatformType.LINKEDIN] == "test prompt"

if __name__ == "__main__":
    # For manual running
    try:
        test_thought_request_uppercase_platform_prompts()
        print("Test passed!")
    except Exception as e:
        print(f"Test failed: {e}")
