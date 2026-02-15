import os
from playwright.sync_api import sync_playwright, expect

def verify_linkedin_auth_prompt():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        try:
            # Go to the app
            page.goto("http://localhost:4200/")

            # Wait for the page to load
            page.wait_for_selector("app-thought-input")

            # Take a screenshot before checking alert
            os.makedirs("/home/jules/verification", exist_ok=True)

            # Check if LinkedIn is selected by default (it should be)
            linkedin_option = page.locator(".platform-option.selected").filter(has_text="LinkedIn")
            expect(linkedin_option).to_be_visible()

            # Check for the auth alert
            auth_alert = page.locator(".auth-alert")
            expect(auth_alert).to_be_visible()
            expect(auth_alert).to_contain_text("LinkedIn Access Required")

            # Check if the submit button is disabled
            submit_btn = page.locator("button.submit-btn")
            # Wait a bit for the check to complete
            page.wait_for_timeout(1000)
            expect(submit_btn).to_be_disabled()

            # Take a screenshot
            page.screenshot(path="/home/jules/verification/linkedin_auth_prompt.png")

            print("Successfully verified LinkedIn auth prompt visibility.")
        except Exception as e:
            print(f"Verification failed: {e}")
            page.screenshot(path="/home/jules/verification/failed_verification.png")
            raise e
        finally:
            browser.close()

if __name__ == "__main__":
    verify_linkedin_auth_prompt()
