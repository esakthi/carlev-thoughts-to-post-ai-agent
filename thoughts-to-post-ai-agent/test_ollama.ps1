#!/usr/bin/env powershell
$ProgressPreference = 'SilentlyContinue'
$ErrorActionPreference = 'Continue'

Write-Host "Testing Ollama qwen3-vl:235b-cloud model..."
Write-Host "================================================"

# Test 1: Check if Ollama is running
Write-Host "`n[TEST 1] Checking Ollama server status..."
try {
    $test = Invoke-WebRequest -Uri 'http://localhost:11434' -UseBasicParsing -TimeoutSec 5
    Write-Host "✓ Ollama is running (Status: $($test.StatusCode))"
} catch {
    Write-Host "✗ Ollama server not responding: $_"
    exit 1
}

# Test 2: List models
Write-Host "`n[TEST 2] Listing installed models..."
try {
    $models = Invoke-WebRequest -Uri 'http://localhost:11434/api/tags' -UseBasicParsing -TimeoutSec 10
    $modelJson = $models.Content | ConvertFrom-Json
    Write-Host "✓ Models found: $($modelJson.models.Count)"
    $modelJson.models | ForEach-Object { Write-Host "  - $($_.name)" }
} catch {
    Write-Host "✗ Failed to list models: $_"
}

# Test 3: Text generation with stream=true (NDJSON)
Write-Host "`n[TEST 3] Testing text generation (streaming)..."
try {
    $body = '{"model":"qwen3-vl:235b-cloud","prompt":"What is AI?","stream":true}'
    Write-Host "Sending request (this may take 10-30 seconds)..."
    $startTime = Get-Date
    $response = Invoke-WebRequest -Uri 'http://localhost:11434/api/generate' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 120
    $elapsed = (Get-Date) - $startTime

    Write-Host "✓ Response received in $([Math]::Round($elapsed.TotalSeconds, 2)) seconds"
    Write-Host "Response length: $($response.Content.Length) bytes"

    # Save full response
    $response.Content | Out-File -FilePath "C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-thoughts-to-post-ai-agent\thoughts-to-post-ai-agent\ollama_response.txt" -Encoding UTF8
    Write-Host "✓ Full response saved to ollama_response.txt"

    # Parse NDJSON
    $lines = @($response.Content -split "`n" | Where-Object { $_ -and $_.Trim() })
    Write-Host "Response contains $($lines.Count) NDJSON lines"

    if ($lines.Count -gt 0) {
        Write-Host "First NDJSON object:"
        $firstLine = $lines[0] | ConvertFrom-Json
        Write-Host ($firstLine | ConvertTo-Json -Depth 2)
    }

    if ($lines.Count -gt 0) {
        Write-Host "Last NDJSON object:"
        $lastLine = $lines[-1] | ConvertFrom-Json
        Write-Host ($lastLine | ConvertTo-Json -Depth 2)
    }

} catch {
    Write-Host "✗ Text generation failed: $_"
}

# Test 4: Text generation with stream=false (single JSON)
Write-Host "`n[TEST 4] Testing text generation (single response)..."
try {
    $body = '{"model":"qwen3-vl:235b-cloud","prompt":"Hello","stream":false}'
    Write-Host "Sending request (this may take 10-30 seconds)..."
    $startTime = Get-Date
    $response = Invoke-WebRequest -Uri 'http://localhost:11434/api/generate' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 120
    $elapsed = (Get-Date) - $startTime

    Write-Host "✓ Response received in $([Math]::Round($elapsed.TotalSeconds, 2)) seconds"
    Write-Host "Response length: $($response.Content.Length) bytes"

    # Parse JSON
    $json = $response.Content | ConvertFrom-Json
    Write-Host "✓ Parsed JSON successfully"
    Write-Host "Response object properties:"
    $json | Get-Member -MemberType NoteProperty | ForEach-Object { Write-Host "  - $($_.Name)" }

    # Save response
    $response.Content | Out-File -FilePath "C:\Users\SAKTHIVADIVEL\Documents\git\carlev-106\carlev-thoughts-to-post-ai-agent\thoughts-to-post-ai-agent\ollama_single_response.txt" -Encoding UTF8
    Write-Host "✓ Full response saved to ollama_single_response.txt"

} catch {
    Write-Host "✗ Single response test failed: $_"
}

Write-Host "`n================================================"
Write-Host "Tests complete! Check the generated response files."

