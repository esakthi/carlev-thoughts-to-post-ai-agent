import asyncio
from playwright.asyncio import async_playwright
import time
import os

async def run():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        # Create a context that bypasses auth by setting local storage
        context = await browser.new_context()
        page = await context.new_page()

        # Define mock data
        categories = [
            {"id": "1", "thoughtCategory": "Tech", "searchDescription": "Tech news", "modelRole": "Expert"}
        ]
        presets = [
            {"id": "p1", "platform": "LINKEDIN", "name": "Professional", "description": "Pro style", "prompt": "Be professional"},
            {"id": "p2", "platform": "FACEBOOK", "name": "Casual", "description": "Casual style", "prompt": "Be casual"},
            {"id": "p3", "platform": "INSTAGRAM", "name": "Creative", "description": "Creative style", "prompt": "Be creative"}
        ]

        # Route to mock API responses
        await page.route("**/api/thoughts/categories", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='[{"id": "1", "thoughtCategory": "Tech", "searchDescription": "Tech news", "modelRole": "Expert"}]'
        ))

        await page.route("**/api/admin/prompts**", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='[{"id": "p1", "platform": "LINKEDIN", "name": "Professional", "description": "Pro style", "prompt": "Be professional"}, {"id": "p2", "platform": "FACEBOOK", "name": "Casual", "description": "Casual style", "prompt": "Be casual"}, {"id": "p3", "platform": "INSTAGRAM", "name": "Creative", "description": "Creative style", "prompt": "Be creative"}]'
        ))

        # Navigate to set localStorage
        await page.goto("http://localhost:4200/login")
        await page.evaluate("() => localStorage.setItem('token', 'fake-jwt-token')")

        # Navigate to the actual page
        print("Navigating to /thoughts/create...")
        await page.goto("http://localhost:4200/thoughts/create")

        # Wait for content to load
        try:
            await page.wait_for_selector("table", timeout=10000)
        except Exception as e:
            print(f"Warning: Table not found: {e}")

        # Take a screenshot
        os.makedirs("/home/jules/verification", exist_ok=True)
        screenshot_path = "/home/jules/verification/thoughts_page_v3.png"
        await page.screenshot(path=screenshot_path, full_page=True)
        print(f"Screenshot saved to {screenshot_path}")

        await browser.close()

if __name__ == "__main__":
    asyncio.run(run())
