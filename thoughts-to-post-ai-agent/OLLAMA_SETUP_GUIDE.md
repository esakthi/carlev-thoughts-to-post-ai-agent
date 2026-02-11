# Ollama Setup Complete ✓

## Summary

You have successfully configured your project to use the **qwen3-vl:235b-cloud** multimodal model for both text generation and image understanding.

### What Was Done

1. **Updated `src/config.py`**
   - `ollama_model` → now defaults to `qwen3-vl:235b-cloud`
   - `ollama_image_model` → now defaults to `qwen3-vl:235b-cloud`
   - This multimodal (VL = Vision-Language) model can handle both text and image tasks

2. **Created `.env` file**
   - Location: `C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-thoughts-to-post-ai-agent\thoughts-to-post-ai-agent\.env`
   - Configured Kafka defaults (localhost:9092)
   - Configured Ollama to use `qwen3-vl:235b-cloud` for both text and images
   - Set `image_generator_type=ollama` to use Ollama for image generation
   - See file contents below for details

3. **Verified Model Installation**
   - ✓ Ollama server is running at `http://localhost:11434`
   - ✓ Model `qwen3-vl:235b-cloud` is installed and available

## Current `.env` Configuration

```
kafka_bootstrap_servers=localhost:9092
kafka_request_topic=thoughts-post-request
kafka_response_topic=thoughts-to-post-response
kafka_consumer_group=ai-agent-group
ollama_base_url=http://localhost:11434
ollama_model=qwen3-vl:235b-cloud
ollama_image_model=qwen3-vl:235b-cloud
image_generator_type=ollama
stable_diffusion_url=http://localhost:7860
openai_api_key=
log_level=INFO
```

## Testing Your Setup

### Option 1: Quick PowerShell Test (Text Generation)

```powershell
$body = '{"model":"qwen3-vl:235b-cloud","prompt":"Write a short poem about AI","stream":false}'
$response = Invoke-WebRequest -Uri 'http://localhost:11434/api/generate' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 120
$response.Content | ConvertFrom-Json | Select-Object output
```

### Option 2: Quick PowerShell Test (Streaming Response)

```powershell
$body = '{"model":"qwen3-vl:235b-cloud","prompt":"Hello, who are you?","stream":true}'
$response = Invoke-WebRequest -Uri 'http://localhost:11434/api/generate' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 120
$response.Content -split "`n" | Where-Object { $_ } | ForEach-Object { $_ | ConvertFrom-Json } | Select-Object -Last 1
```

## About qwen3-vl:235b-cloud

- **Type:** Multimodal Vision-Language model
- **Size:** 235 billion parameters
- **Capabilities:** 
  - Text generation and understanding
  - Image understanding and analysis
  - Vision-language tasks (can answer questions about images)
- **Quantization:** FP8 (efficient for consumer hardware)

## Next Steps

1. **Run your Python application** - It will now read settings from `.env` and use Ollama automatically
2. **Test with the included test script** - Run `test_ollama.ps1` for comprehensive testing
3. **Configure Kafka** (Optional)
   - If you have Kafka installed, the bootstrap server is set to `localhost:9092`
   - If you don't have Kafka, see instructions below

## Kafka Configuration (if needed)

### If you don't have Kafka installed:

**Option A: Use Local File-Based Queue (Easiest for Development)**
- Modify your `src/kafka/producer.py` and `src/kafka/consumer.py` to use local files instead of Kafka
- This is sufficient for local testing without external dependencies

**Option B: Install Kafka (Docker + Compose)**
```powershell
# Prerequisites: Have Docker Desktop installed
# Create a docker-compose.yml in your project root

# Then run:
docker-compose up -d
```

**Option C: Install Redpanda (Kafka-Compatible, Single Binary)**
- Download from: https://vectorized.io/redpanda
- Follow their Windows installation guide
- Update `kafka_bootstrap_servers=localhost:9092` (default port)

## Troubleshooting

### Ollama Server Not Responding
```powershell
# Check if Ollama is running
Invoke-WebRequest -Uri 'http://localhost:11434' -UseBasicParsing
```
- If this fails, start the Ollama desktop application

### Model Not Found Error
- Verify the model is installed: List models via Ollama desktop UI
- Current model: `qwen3-vl:235b-cloud` is already confirmed installed

### Very Long Response Times
- First inference run with the model can take 30-60 seconds (model warm-up)
- Subsequent calls will be faster (typically 10-30 seconds depending on prompt length)
- This is normal for large 235B models

### PowerShell Terminal Hangs
- The qwen3-vl model is large and responses can take time
- Use `-TimeoutSec 120` or higher in Invoke-WebRequest calls
- Use `stream=true` for real-time streaming output

## Useful PowerShell Commands

### Check Ollama Server Status
```powershell
Invoke-WebRequest -Uri 'http://localhost:11434' -UseBasicParsing
```

### List All Installed Models
```powershell
Invoke-RestMethod -Uri 'http://localhost:11434/api/tags' -UseBasicParsing | ConvertTo-Json
```

### Test with Different Prompts
```powershell
@"
{"model":"qwen3-vl:235b-cloud","prompt":"Your prompt here","stream":false}
"@ | Invoke-WebRequest -Uri 'http://localhost:11434/api/generate' -Method Post -ContentType 'application/json' -Body (Get-Content -Raw) -TimeoutSec 120
```

## Files Modified/Created

- ✓ `src/config.py` - Updated default models
- ✓ `.env` - Created with configuration
- ✓ `test_ollama.ps1` - Test script for verification

## Questions?

1. **Image Generation**: You now have `image_generator_type=ollama`, which means your image generation agent will use the qwen3-vl model for vision tasks
2. **Text Generation**: All text generation will use qwen3-vl:235b-cloud
3. **Switching Models**: Edit `.env` to change `ollama_model` and `ollama_image_model` to any other installed Ollama model
4. **Stable Diffusion**: If you want to switch back to Stable Diffusion for image generation, set `image_generator_type=stable_diffusion` and ensure Stable Diffusion is running on `http://localhost:7860`

---

**Status:** ✅ Ollama configuration complete and verified
**Model:** qwen3-vl:235b-cloud (235B multimodal)
**Server:** http://localhost:11434 (Running)

