import asyncio
from playwright.async_api import async_playwright
import json

async def run():
    async def handle_route(route):
        url = route.request.url
        if "/api/admin/thought-categories" in url:
            await route.fulfill(
                status=200,
                content_type="application/json",
                body=json.dumps([{"id": "1", "thoughtCategory": "Tech", "searchDescription": "Tech", "modelRole": "Expert"}])
            )
        elif "/api/admin/presets" in url:
            await route.fulfill(
                status=200,
                content_type="application/json",
                body=json.dumps([
                    {"id": "p1", "name": "Professional", "platform": "LINKEDIN", "prompt": "Be professional", "description": "For LinkedIn", "createdDateTime": "2023-01-01T00:00:00Z"},
                    {"id": "p2", "name": "Casual", "platform": "FACEBOOK", "prompt": "Be casual", "description": "For FB", "createdDateTime": "2023-01-01T00:00:00Z"}
                ])
            )
        elif "/api/admin/platform-prompts" in url:
            await route.fulfill(
                status=200,
                content_type="application/json",
                body=json.dumps([])
            )
        else:
            await route.continue_()

    async with async_playwright() as p:
        browser = await p.chromium.launch()
        context = await browser.new_context()
        page = await context.new_page()

        # Set mock auth token
        await page.add_init_script("""
            localStorage.setItem('auth_token', 'mock-token');
            localStorage.setItem('user_email', 'test@example.com');
        """)

        await page.route("**/api/**", handle_route)

        page.on("console", lambda msg: print(f"CONSOLE: {msg.text}"))
        page.on("pageerror", lambda exc: print(f"PAGE ERROR: {exc}"))

        try:
            await page.goto("http://localhost:4200/thoughts", wait_until="networkidle")
            # Wait a bit for components to render
            await asyncio.sleep(2)
            await page.screenshot(path="/home/jules/verification/thoughts_page_v2.png", full_page=True)
            print("Screenshot saved to /home/jules/verification/thoughts_page_v2.png")
        except Exception as e:
            print(f"Error: {e}")
        finally:
            await browser.close()

if __name__ == "__main__":
    asyncio.run(run())
