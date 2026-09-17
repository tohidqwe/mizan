import { chromium } from 'playwright';
import fs from 'node:fs/promises';

const out = 'qavanin-official';
await fs.mkdir(out, { recursive: true });
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  locale: 'fa-IR',
  timezoneId: 'Asia/Tehran',
  userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36',
});
const page = await context.newPage();
page.setDefaultTimeout(30000);

const urls = [
  'https://qavanin.ir/Law/PrintText/?IDS=12021850837713548188&font=',
  'https://qavanin.ir/Law/TreeText/?IDS=12021850837713548188',
];

let ok = false;
for (const url of urls) {
  try {
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 });
    for (let i = 0; i < 8; i++) {
      await page.waitForTimeout(2500);
      const text = await page.locator('body').innerText().catch(() => '');
      if (text.includes('قانون مدنی') && /ماده\s*[۱1]/.test(text)) {
        ok = true;
        break;
      }
    }
    if (ok) break;
  } catch (e) {
    await fs.writeFile(`${out}/error.txt`, String(e));
  }
}

await fs.writeFile(`${out}/final-url.txt`, page.url());
await fs.writeFile(`${out}/official.html`, await page.content());
const body = await page.locator('body').innerText().catch(() => '');
await fs.writeFile(`${out}/official.txt`, body);
await page.screenshot({ path: `${out}/official.png`, fullPage: true }).catch(() => {});
await fs.writeFile(`${out}/meta.json`, JSON.stringify({
  ok,
  length: body.length,
  hasArticle1: /ماده\s*[۱1]/.test(body),
  hasArticle1335: /ماده\s*(۱۳۳۵|1335)/.test(body),
  url: page.url(),
}, null, 2));

await browser.close();
if (!ok) process.exit(2);
