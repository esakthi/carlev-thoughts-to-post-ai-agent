"""Search Agent for generating search criteria and performing internet searches."""

import logging
from typing import Optional

from langchain_ollama import ChatOllama
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_community.tools.ddg_search import DuckDuckGoSearchRun

from ..config import settings

logger = logging.getLogger(__name__)

CRITERIA_PROMPT = """You are a search expert. Given a thought category and a description of what the user is looking for,
generate a single, highly effective search string to find relevant information, news, or inspiration on the internet.

Category: {category}
Description: {description}

Respond ONLY with the search string, nothing else."""

class SearchAgent:
    """Agent for internet search related tasks."""

    def __init__(
        self,
        model_name: Optional[str] = None,
        base_url: Optional[str] = None,
    ):
        self.model_name = model_name or settings.ollama_model
        self.base_url = base_url or settings.ollama_base_url
        self._llm: Optional[ChatOllama] = None
        self._search_tool = DuckDuckGoSearchRun()

    def _get_llm(self) -> ChatOllama:
        if not self._llm:
            self._llm = ChatOllama(
                model=self.model_name,
                base_url=self.base_url,
                temperature=0.1,
            )
        return self._llm

    def generate_search_string(self, category: str, description: str) -> str:
        """Generate a search string from category and description."""
        llm = self._get_llm()
        prompt = ChatPromptTemplate.from_template(CRITERIA_PROMPT)
        chain = prompt | llm | StrOutputParser()

        logger.info(f"Generating search string for category='{category}', description='{description}'")
        search_string = chain.invoke({"category": category, "description": description})
        return search_string.strip().strip('"')

    def execute_search(self, search_string: str) -> str:
        """Execute an internet search."""
        logger.info(f"Executing internet search: '{search_string}'")
        try:
            results = self._search_tool.invoke(search_string)
            return results
        except Exception as e:
            logger.error(f"Search failed: {e}")
            return f"Search failed: {str(e)}"
