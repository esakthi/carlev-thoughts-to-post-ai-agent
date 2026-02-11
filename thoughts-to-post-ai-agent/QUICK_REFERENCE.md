# Quick Reference - Ollama Setup

## ✅ What's Been Done

| Item | Status | Details |
|------|--------|---------|
| Model Selected | ✅ | qwen3-vl:235b-cloud (multimodal) |
| config.py Updated | ✅ | Both `ollama_model` and `ollama_image_model` set to qwen3-vl:235b-cloud |
| .env Created | ✅ | All settings configured in project root |
| Image Generator Type | ✅ | Set to `ollama` in .env |
| Ollama Server Check | ✅ | Running at http://localhost:11434 |
| Model Installed | ✅ | qwen3-vl:235b-cloud confirmed available |

---

## 🚀 Start Using Your App Now

### Method 1: Import & Use Settings in Python

```python
from src.config import settings

# Text generation
print(f"Using model: {settings.ollama_model}")
print(f"Ollama URL: {settings.ollama_base_url}")
print(f"Image model: {settings.ollama_image_model}")

# Use these in your agents
# settings.ollama_base_url → for API calls
# settings.ollama_model → for text requests
# settings.ollama_image_model → for vision requests
```

### Method 2: Test from PowerShell (One-Liner)

```powershell
$b='{"model":"qwen3-vl:235b-cloud","prompt":"Hi","stream":false}'; Invoke-WebRequest -Uri 'http://localhost:11434/api/generate' -Method Post -ContentType 'application/json' -Body $b -TimeoutSec 120 | Select-Object -ExpandProperty Content | ConvertFrom-Json
```

### Method 3: Run the Test Script

```powershell
cd <project-root>
powershell -ExecutionPolicy Bypass -File test_ollama.ps1
```

---

## 📋 Environment Variables Summary

Your `.env` file now has:

```
ollama_model=qwen3-vl:235b-cloud          ← Text generation
ollama_image_model=qwen3-vl:235b-cloud    ← Image understanding
image_generator_type=ollama                ← Use Ollama for images
ollama_base_url=http://localhost:11434    ← Ollama API endpoint
```

---

## ⏱️ Performance Notes

- **First request:** 30-60 seconds (model warm-up on first inference)
- **Subsequent requests:** 10-30 seconds (depends on prompt length)
- **Timeout setting:** Use `-TimeoutSec 120` in PowerShell for safety
- **Model size:** 235B parameters = slower but higher quality

---

## 🔄 Can I Use Different Models?

**Yes!** To switch models:

### Option 1: Change in `.env` (Recommended)
```
ollama_model=llama2                    # Change to llama2
ollama_image_model=llava               # Change to llava
```

### Option 2: Change in `src/config.py` defaults
```python
ollama_model: str = Field(default="llama2", ...)
ollama_image_model: str = Field(default="llava", ...)
```

**To see available models:**
```powershell
Invoke-RestMethod -Uri 'http://localhost:11434/api/tags' -UseBasicParsing | ConvertFrom-Json | Select-Object -ExpandProperty models | Select-Object name
```

---

## 🖼️ Image Generation Options

Currently set to: `image_generator_type=ollama`

### Option A: Use Ollama for Images (Current)
```
image_generator_type=ollama
ollama_image_model=qwen3-vl:235b-cloud
```

### Option B: Switch to Stable Diffusion
```
image_generator_type=stable_diffusion
stable_diffusion_url=http://localhost:7860
```
(Requires Stable Diffusion running locally)

### Option C: Use OpenAI DALL-E
```
image_generator_type=dalle
openai_api_key=sk-your-key-here
```

---

## 🐳 Kafka Setup (Optional for Local Dev)

### If you DON'T have Kafka:

**Easiest:** Modify producer/consumer to use files instead
```python
# In src/kafka/producer.py
# Instead of sending to Kafka, write to: ./local_queue/request.json
```

**Better:** Use Docker + docker-compose
```bash
docker-compose up -d  # Run a local Kafka
```

**Best:** Use Redpanda (Kafka-compatible)
```
https://vectorized.io/redpanda/
```

---

## 🐛 Troubleshooting One-Liners

```powershell
# Is Ollama running?
Invoke-WebRequest http://localhost:11434 -UseBasicParsing

# What models are installed?
Invoke-RestMethod http://localhost:11434/api/tags -UseBasicParsing

# Can I reach the model?
$b='{"model":"qwen3-vl:235b-cloud","prompt":"test"}'; Invoke-WebRequest http://localhost:11434/api/generate -Method Post -ContentType 'application/json' -Body $b -TimeoutSec 120
```

---

## 📂 Files You Now Have

```
project-root/
├── .env                          ← Your configuration (added)
├── .env.example                  ← Example config (original)
├── OLLAMA_SETUP_GUIDE.md        ← Detailed guide (added)
├── QUICK_REFERENCE.md           ← This file (added)
├── test_ollama.ps1              ← Test script (added)
├── src/
│   ├── config.py                ← Updated with qwen3-vl models
│   ├── main.py
│   ├── agents/
│   │   ├── content_enrichment_agent.py
│   │   └── image_generation_agent.py
│   └── kafka/
│       ├── consumer.py
│       └── producer.py
└── pyproject.toml
```

---

## ✨ You're Ready!

Your Ollama setup is complete. Your Python app will now:

1. Load settings from `.env` on startup
2. Use `qwen3-vl:235b-cloud` for text generation
3. Use `qwen3-vl:235b-cloud` for image understanding
4. Call Ollama at `http://localhost:11434/api/generate`

Start using it by importing `settings` from `src.config`:

```python
from src.config import settings

# All your config values are here:
settings.ollama_base_url        # "http://localhost:11434"
settings.ollama_model           # "qwen3-vl:235b-cloud"
settings.ollama_image_model     # "qwen3-vl:235b-cloud"
settings.image_generator_type   # "ollama"
```

---

**Last Updated:** Feb 8, 2026
**Status:** ✅ Ready to use
**Next Step:** Implement your agents using these settings!

