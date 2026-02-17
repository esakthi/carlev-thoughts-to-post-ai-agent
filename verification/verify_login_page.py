from playwright.sync_api import Page, expect, sync_playwright

def test_login_page(page: Page):
    page.goto("http://localhost:4200/login")
    page.wait_for_selector("h2")
    expect(page.get_by_role("heading", name="Login")).to_be_visible()
    page.screenshot(path="verification/login_page.png")

    # Try to go to register
    page.get_by_role("link", name="Register here").click()
    page.wait_for_url("**/register")
    expect(page.get_by_role("heading", name="Register")).to_be_visible()
    page.screenshot(path="verification/register_page.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            test_login_page(page)
        finally:
            browser.close()
