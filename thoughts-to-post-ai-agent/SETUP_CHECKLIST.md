# ✅ Ollama Setup - Complete Checklist

## Pre-Setup (Already Done ✓)

- [x] Ollama installed on Windows 11
- [x] Ollama desktop app running at http://localhost:11434
- [x] Model `qwen3-vl:235b-cloud` installed
- [x] Verified model is available via API

## Configuration Files (Already Done ✓)

- [x] `.env` created with all settings
- [x] `src/config.py` updated with new defaults
- [x] `pyproject.toml` updated with `requests` dependency

## Documentation Files (Already Done ✓)

- [x] `OLLAMA_SETUP_GUIDE.md` - Complete guide with examples
- [x] `QUICK_REFERENCE.md` - One-page cheat sheet
- [x] `OLLAMA_USAGE_EXAMPLES.py` - Practical code examples
- [x] `test_ollama.ps1` - PowerShell test script

## Verification Steps (Do These Next)

### Step 1: Verify Ollama Server is Running
```powershell
Invoke-WebRequest -Uri 'http://localhost:11434' -UseBasicParsing
```
✓ Expected: HTTP 200 status code

### Step 2: Install Python Dependencies
```powershell
cd "C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-thoughts-to-post-ai-agent\thoughts-to-post-ai-agent"
poetry install
# or if you prefer pip:
# pip install -r requirements.txt
```
✓ Expected: All dependencies installed including `requests`

### Step 3: Test Ollama Connection from Python
```powershell
python -c "from src.config import settings; print(f'Base URL: {settings.ollama_base_url}'); print(f'Model: {settings.ollama_model}')"
```
✓ Expected: Prints your configuration values

### Step 4: Run Text Generation Example
Run the provided example:
```powershell
python OLLAMA_USAGE_EXAMPLES.py
```
✓ Expected: Shows model availability and generates sample text

### Step 5: Test Your Agents
Once verified, implement calls in:
- `src/agents/content_enrichment_agent.py` - Use `settings.ollama_model` for text
- `src/agents/image_generation_agent.py` - Use `settings.ollama_image_model` for images

---

## Configuration Reference

### Environment Variables (from `.env`)
```
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3-vl:235b-cloud
OLLAMA_IMAGE_MODEL=qwen3-vl:235b-cloud
IMAGE_GENERATOR_TYPE=ollama
```

### Python Access
```python
from src.config import settings

# All settings loaded automatically from .env
settings.ollama_base_url        # "http://localhost:11434"
settings.ollama_model           # "qwen3-vl:235b-cloud"
settings.ollama_image_model     # "qwen3-vl:235b-cloud"
settings.image_generator_type   # "ollama"
```

---

## API Endpoints Available

### 1. Text Generation
**Endpoint:** `POST http://localhost:11434/api/generate`
```json
{
  "model": "qwen3-vl:235b-cloud",
  "prompt": "Your prompt here",
  "stream": false,
  "temperature": 0.7
}
```

### 2. List Models
**Endpoint:** `GET http://localhost:11434/api/tags`
**Response:** JSON array of available models

### 3. Pull New Models
**Endpoint:** `POST http://localhost:11434/api/pull`
```json
{
  "name": "model-name"
}
```

---

## Code Integration Examples

### Content Enrichment Agent
```python
from src.config import settings
import requests

def enrich_content(thought: str) -> str:
    url = f"{settings.ollama_base_url}/api/generate"
    payload = {
        "model": settings.ollama_model,
        "prompt": f"Enhance this thought: {thought}",
        "stream": False
    }
    response = requests.post(url, json=payload, timeout=120)
    return response.json().get("response", "")
```

### Image Generation Agent
```python
from src.config import settings
import requests

def analyze_image(image_path: str, question: str) -> str:
    url = f"{settings.ollama_base_url}/api/generate"
    # Read image and convert to base64
    with open(image_path, "rb") as f:
        image_data = base64.b64encode(f.read()).decode()
    
    payload = {
        "model": settings.ollama_image_model,
        "prompt": f"[Image: {image_data}]\n{question}",
        "stream": False
    }
    response = requests.post(url, json=payload, timeout=120)
    return response.json().get("response", "")
```

---

## Troubleshooting Quick Fixes

| Issue | Check | Fix |
|-------|-------|-----|
| `ConnectionError` | Is Ollama running? | Start Ollama desktop app |
| `timeout` | Is model loaded? | Increase `-TimeoutSec` to 120+ |
| `Model not found` | Is model installed? | Check via `http://localhost:11434/api/tags` |
| `settings not loading` | Is `.env` in project root? | Move `.env` to the correct location |
| `requests not found` | Is poetry lock updated? | Run `poetry install` |

---

## What Each File Does

### Configuration
- **`.env`** - Your environment variables (loaded by config.py)
- **`src/config.py`** - Settings class that reads from `.env`
- **`pyproject.toml`** - Project dependencies including `requests`

### Documentation
- **`OLLAMA_SETUP_GUIDE.md`** - Detailed troubleshooting and setup
- **`QUICK_REFERENCE.md`** - Quick command lookup
- **`OLLAMA_USAGE_EXAMPLES.py`** - 6 practical code examples

### Testing
- **`test_ollama.ps1`** - PowerShell test script for verification

---

## Ready to Code? Here's What to Do Next

1. **Verify Setup**
   ```powershell
   python OLLAMA_USAGE_EXAMPLES.py
   ```

2. **Open Your Agent Files**
   - `src/agents/content_enrichment_agent.py`
   - `src/agents/image_generation_agent.py`

3. **Use the Settings**
   ```python
   from src.config import settings
   # Now use settings in your agent logic
   ```

4. **Test Your Implementation**
   - Create sample inputs
   - Call Ollama via the settings
   - Verify responses

5. **Integrate with Kafka** (if using)
   - Update `src/kafka/consumer.py`
   - Update `src/kafka/producer.py`
   - Or use local file queue for development

---

## Performance Expectations

- **Model Type**: Multimodal Vision-Language (qwen3-vl:235b-cloud)
- **Model Size**: 235 billion parameters
- **First Request**: 30-60 seconds (warm-up)
- **Typical Request**: 10-30 seconds
- **Best For**: High-quality text and image understanding
- **RAM Usage**: 8GB+ recommended

---

## Model Capabilities

### ✓ Text Generation
- Essay writing
- Content enrichment
- Social media copywriting
- Thought expansion

### ✓ Image Understanding
- Image description
- Visual question answering
- Content analysis
- Caption generation

### ✓ Vision-Language Tasks
- Combining text and images
- Cross-modal understanding
- Context-aware generation

---

## Support & Resources

### Official Documentation
- Ollama: https://ollama.ai
- Qwen Model: https://github.com/QwenLM/Qwen

### When Things Break
1. Check `OLLAMA_SETUP_GUIDE.md` - Troubleshooting section
2. Run `test_ollama.ps1` - Diagnostic script
3. Verify Ollama is running: `http://localhost:11434`
4. Check model is installed: `http://localhost:11434/api/tags`

---

## Files Summary

```
Your Project Root
├── .env                          ← Environment config (IMPORTANT)
├── .env.example                  ← Reference example
├── OLLAMA_SETUP_GUIDE.md        ← Detailed guide
├── QUICK_REFERENCE.md           ← Quick lookup
├── OLLAMA_USAGE_EXAMPLES.py     ← Code examples
├── test_ollama.ps1              ← Test script
├── pyproject.toml               ← Updated with requests
├── src/
│   ├── config.py                ← Updated defaults
│   └── agents/
│       ├── content_enrichment_agent.py
│       └── image_generation_agent.py
└── ...
```

---

## ✨ You Are Ready!

- ✅ Ollama installed and verified
- ✅ Configuration complete
- ✅ Dependencies updated
- ✅ Examples provided
- ✅ Documentation created

**Next: Run the verification steps above, then start coding your agents!**

---

**Created**: February 8, 2026
**Status**: ✅ Complete
**Next Action**: Verify setup with `python OLLAMA_USAGE_EXAMPLES.py`

