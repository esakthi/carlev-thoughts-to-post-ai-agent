# Carlev API Service - PowerShell Testing Script
# Tests the /new/event endpoint on http://localhost:8081

$baseUrl = "http://localhost:8081/new/event"
$headers = @{
    "Content-Type" = "application/json"
}

Write-Host "========================================"
Write-Host "Carlev API - Event Testing Script"
Write-Host "========================================"
Write-Host ""

# Test 1: Simple Event
Write-Host "[TEST 1] Creating a simple event..."
$body1 = @{
    title = "Test Event 1"
    description = "Simple test event"
    source = "Test"
    url = "https://example.com"
    category = "Testing"
    publishedAt = "2026-01-16T13:17:20.059+05:30"
} | ConvertTo-Json

try {
    $response1 = Invoke-RestMethod -Uri $baseUrl -Method POST -Headers $headers -Body $body1
    Write-Host "✓ Success:" -ForegroundColor Green
    $response1 | ConvertTo-Json | Write-Host
} catch {
    Write-Host "✗ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host ""
Write-Host ""

# Test 2: Event with Metadata
Write-Host "[TEST 2] Creating event with metadata..."
$body2 = @{
    id = "evt-test-001"
    title = "Event with Metadata"
    description = "Event containing metadata"
    source = "Test API"
    url = "https://test.example.com"
    category = "Technology"
    publishedAt = "2026-01-16T13:17:20.059+05:30"
    metadata = @{
        author = "Test User"
        priority = "high"
        tags = @("test", "metadata")
    }
} | ConvertTo-Json -Depth 5

try {
    $response2 = Invoke-RestMethod -Uri $baseUrl -Method POST -Headers $headers -Body $body2
    Write-Host "✓ Success:" -ForegroundColor Green
    $response2 | ConvertTo-Json | Write-Host
} catch {
    Write-Host "✗ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host ""
Write-Host ""

# Test 3: Complete Event
Write-Host "[TEST 3] Creating a complete event with all fields..."
$body3 = @{
    id = "evt-complete-001"
    title = "Complete Event Example"
    description = "This is a complete event with all available fields"
    source = "Complete Test"
    url = "https://complete.example.com/article/123"
    category = "Technology"
    publishedAt = "2026-01-16T13:17:20.059+05:30"
    metadata = @{
        author = "Complete Test"
        source_id = "test-001"
        confidence_score = 0.99
        tags = @("test", "complete", "full")
    }
    rawData = @{
        original_id = "src-123"
        original_source = "test-provider"
        extra_field = "extra_value"
        processing_time_ms = 150
    }
} | ConvertTo-Json -Depth 5

try {
    $response3 = Invoke-RestMethod -Uri $baseUrl -Method POST -Headers $headers -Body $body3
    Write-Host "✓ Success:" -ForegroundColor Green
    $response3 | ConvertTo-Json | Write-Host
} catch {
    Write-Host "✗ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host ""
Write-Host "========================================"
Write-Host "Tests completed. Check responses above."
Write-Host "========================================"
Write-Host ""

# Optional: Check if server is running
Write-Host "[OPTIONAL] Checking if server is running..."
try {
    $health = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -Method GET -ErrorAction SilentlyContinue
    Write-Host "✓ Server is running (Status: $($health.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "✗ Server may not be running or health endpoint not available" -ForegroundColor Red
}
