import pytest
from unittest.mock import MagicMock, patch
from src.agents.content_enrichment_agent import ContentEnrichmentAgent
from src.models.dataclasses import ThoughtRequest, PlatformType

@pytest.fixture
def agent():
    return ContentEnrichmentAgent(model_name="test-model", base_url="http://test:11434")

@patch("src.agents.content_enrichment_agent.ChatOllama")
def test_get_prompt_template_uses_dynamic_prompts(mock_ollama, agent):
    # Arrange
    request = ThoughtRequest(
        requestId="req1",
        userId="user1",
        originalThought="Test thought",
        platforms=["linkedin"],
        modelRole="Dynamic System Prompt",
        searchDescription="Dynamic Category Description",
        platformPrompts={"linkedin": "Dynamic LinkedIn Prompt"}
    )

    # Act
    prompt_template = agent._get_prompt_template(PlatformType.LINKEDIN, request)

    # Assert
    # The prompt template should have 3 messages: system, history placeholder, and human
    messages = prompt_template.messages
    assert len(messages) == 3

    system_message = messages[0].prompt.template
    assert "Dynamic System Prompt" in system_message
    assert "Dynamic Category Description" in system_message
    assert "Dynamic LinkedIn Prompt" in system_message

@patch("src.agents.content_enrichment_agent.ChatOllama")
def test_get_prompt_template_falls_back_to_defaults(mock_ollama, agent):
    # Arrange
    request = ThoughtRequest(
        requestId="req1",
        userId="user1",
        originalThought="Test thought",
        platforms=["linkedin"]
        # No dynamic prompts
    )

    # Act
    prompt_template = agent._get_prompt_template(PlatformType.LINKEDIN, request)

    # Assert
    messages = prompt_template.messages
    system_message = messages[0].prompt.template
    assert "You are an AI content enrichment specialist" in system_message # from BASE_SYSTEM_PROMPT
    assert "expert LinkedIn content creator" in system_message # from PLATFORM_PROMPTS
