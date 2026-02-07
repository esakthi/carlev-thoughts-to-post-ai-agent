@echo off
REM Quick Test Scripts for Carlev API Service
REM These scripts test the /new/event endpoint on http://localhost:8081

echo.
echo ========================================
echo Carlev API - Event Testing Scripts
echo ========================================
echo.

REM Test 1: Simple Event
echo [TEST 1] Creating a simple event...
curl -X POST http://localhost:8081/new/event ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Test Event 1\",\"description\":\"Simple test event\",\"source\":\"Test\",\"url\":\"https://example.com\",\"category\":\"Testing\",\"publishedAt\":\"2026-01-16T13:17:20.059+05:30\"}"

echo.
echo.

REM Test 2: Event with Metadata
echo [TEST 2] Creating event with metadata...
curl -X POST http://localhost:8081/new/event ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"evt-test-001\",\"title\":\"Event with Metadata\",\"description\":\"Event containing metadata\",\"source\":\"Test API\",\"url\":\"https://test.example.com\",\"category\":\"Technology\",\"publishedAt\":\"2026-01-16T13:17:20.059+05:30\",\"metadata\":{\"author\":\"Test User\",\"priority\":\"high\"}}"

echo.
echo.

REM Test 3: Complete Event
echo [TEST 3] Creating a complete event with all fields...
curl -X POST http://localhost:8081/new/event ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"evt-complete-001\",\"title\":\"Complete Event Example\",\"description\":\"This is a complete event with all available fields\",\"source\":\"Complete Test\",\"url\":\"https://complete.example.com/article/123\",\"category\":\"Technology\",\"publishedAt\":\"2026-01-16T13:17:20.059+05:30\",\"metadata\":{\"author\":\"Complete Test\",\"source_id\":\"test-001\",\"confidence_score\":0.99,\"tags\":[\"test\",\"complete\"]},\"rawData\":{\"original_id\":\"src-123\",\"original_source\":\"test-provider\",\"extra_field\":\"extra_value\"}}"

echo.
echo ========================================
echo Tests completed. Check responses above.
echo ========================================
echo.

REM Test 4: Health Check (if available)
echo [OPTIONAL] Testing if server is running (health check)...
curl -i http://localhost:8081/actuator/health

pause
