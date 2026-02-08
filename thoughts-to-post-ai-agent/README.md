# Thoughts-to-Post AI Agent

AI Agent for enriching user thoughts and generating social media content using LangChain and Ollama.

## Features

- **Content Enrichment**: Transforms raw thoughts/topics into polished social media content
- **Multi-Platform Support**: Generates platform-specific content (LinkedIn, Facebook, Instagram)
- **Image Generation**: Creates relevant images using Stable Diffusion or DALL-E
- **Checkpoint Memory**: Tracks conversation history for refinement requests
- **Kafka Integration**: Consumes requests and publishes responses via Kafka

## Prerequisites

- Python 3.11+
- Poetry
- Ollama with `llama3.2` model installed
- Kafka running on localhost:9092
- (Optional) Stable Diffusion or DALL-E API access

## Setup

1. Install dependencies:
   ```bash
   poetry install
   ```

2. Copy environment file:
   ```bash
   cp .env.example .env
   ```

3. Configure `.env` with your settings

4. Download Ollama model:
   ```bash
   ollama pull llama3.2
   ```

## Running

```bash
poetry run python src/main.py
```

## TODO

- [ ] Configure `.env` with actual values
- [ ] Ensure Ollama is running with required models
- [ ] Configure image generation (Stable Diffusion or DALL-E)
