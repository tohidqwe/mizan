import { chromium } from 'playwright';
import fs from 'node:fs/promises';

const out = 'rrk-probe';
await fs.mkdir(out, { recursive: true });
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ locale: 'fa-IR' });
page.setDefaultTimeout(30000);

const url = 'https://rrk.ir/ords/r/rrs/rrs-front/ghavanin-moghararat';
await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 120000 });
await page.waitForTimeout(3000);

const meta = await page.evaluate(() => ({
  title: document.title,
  url: location.href,
  inputs: [...document.querySelectorAll('input')].map(e => ({ id: e.id, name: e.name, type: e.type, placeholder: e.placeholder })),
  buttons: [...document.querySelectorAll('button,input[type=button],input[type=submit],a.t-Button')].map(e => ({ id: e.id, text: (e.innerText || e.value || '').trim() })).filter(x => x.text || x.id),
}));
await fs.writeFile(`${out}/meta.json`, JSON.stringify(meta, null, 2));
await fs.writeFile(`${out}/initial.html`, await page.content());
await page.screenshot({ path: `${out}/initial.png`, fullPage: true });

const titleInput = page.locator('#P67_TITLE');
if (await titleInput.count()) {
  await titleInput.fill('قانون مدنی');
  const searchButton = page.getByRole('button', { name: /جست|اعمال|Search/i }).first();
  if (await searchButton.count()) {
    await searchButton.click();
  } else {
    await titleInput.press('Enter');
  }
  await page.waitForTimeout(6000);
  await fs.writeFile(`${out}/results.html`, await page.content());
  await page.screenshot({ path: `${out}/results.png`, fullPage: true });

  const links = await page.locator('a').evaluateAll(as => as.map(a => ({
    text: (a.textContent || '').replace(/\s+/g, ' ').trim(),
    href: a.href,
  })).filter(x => x.text.includes('قانون مدنی') || x.href.includes('p122_code') || x.href.includes('/law')));
  await fs.writeFile(`${out}/links.json`, JSON.stringify(links, null, 2));

  const exact = page.getByRole('link', { name: /^قانون مدنی$/ }).first();
  if (await exact.count()) {
    await exact.click();
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(4000);
    await fs.writeFile(`${out}/detail.html`, await page.content());
    await fs.writeFile(`${out}/detail.txt`, await page.locator('body').innerText());
    await page.screenshot({ path: `${out}/detail.png`, fullPage: true });
  }
}

await browser.close();
