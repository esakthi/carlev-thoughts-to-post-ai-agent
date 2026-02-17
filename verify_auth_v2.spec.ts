import { test, expect } from '@playwright/test';

test('verify login page v2', async ({ page }) => {
  await page.goto('http://localhost:4200/login');
  await page.waitForTimeout(2000);
  await page.screenshot({ path: 'login_v2.png' });
  await expect(page.locator('h2')).toContainText('Login');
});

test('verify register page v2', async ({ page }) => {
  await page.goto('http://localhost:4200/register');
  await page.waitForTimeout(2000);
  await page.screenshot({ path: 'register_v2.png' });
  await expect(page.locator('h2')).toContainText('Register');
});
