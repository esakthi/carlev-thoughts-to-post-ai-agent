import pytest
from unittest.mock import MagicMock, patch
from src.agents.search_agent import SearchAgent

@pytest.fixture
def search_agent():
    with patch("src.agents.search_agent.DuckDuckGoSearchRun"):
        return SearchAgent()

@patch("src.agents.search_agent.ChatOllama")
def test_generate_search_string(mock_ollama, search_agent):
    # Setup mock
    mock_llm = MagicMock()
    mock_ollama.return_value = mock_llm

    # Mock the chain invocation
    # In LangChain 0.3, chain = prompt | llm | StrOutputParser()
    # We can mock the llm invoke or the whole chain
    mock_llm.invoke.return_value.content = "suggested search query"

    # We need to mock the parser as well if we mock the whole chain,
    # but it's easier to mock the LLM's response.
    # Wait, the SearchAgent calls chain.invoke()

    with patch("langchain_core.prompts.ChatPromptTemplate.from_template") as mock_prompt:
        mock_chain = MagicMock()
        mock_prompt.return_value.__or__.return_value.__or__.return_value = mock_chain
        mock_chain.invoke.return_value = "suggested search query"

        result = search_agent.generate_search_string("Tech", "AI trends")

        assert result == "suggested search query"
        mock_chain.invoke.assert_called_once()

def test_execute_search(search_agent):
    with patch.object(search_agent._search_tool, "invoke") as mock_invoke:
        mock_invoke.return_value = "search results from internet"

        result = search_agent.execute_search("my query")

        assert result == "search results from internet"
        mock_invoke.assert_called_once_with("my query")
