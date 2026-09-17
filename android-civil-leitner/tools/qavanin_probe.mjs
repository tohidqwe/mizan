import { chromium } from 'playwright';
import fs from 'node:fs/promises';

const out = 'qavanin-probe';
await fs.mkdir(out, { recursive: true });
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ locale: 'fa-IR' });
page.setDefaultTimeout(30000);
await page.goto('https://qavanin.ir/', { waitUntil: 'domcontentloaded', timeout: 120000 });
await page.waitForTimeout(2500);
const meta = await page.evaluate(() => ({
  title: document.title,
  url: location.href,
  forms: [...document.forms].map(f => ({ id:f.id, name:f.name, action:f.action, method:f.method })),
  inputs: [...document.querySelectorAll('input,select,button')].map(e => ({
    tag:e.tagName, id:e.id, name:e.getAttribute('name'), type:e.getAttribute('type'),
    value:e.getAttribute('value'), placeholder:e.getAttribute('placeholder'),
    text:(e.innerText||'').trim(), aria:e.getAttribute('aria-label')
  }))
}));
await fs.writeFile(`${out}/meta.json`, JSON.stringify(meta,null,2));
await fs.writeFile(`${out}/home.html`, await page.content());
await page.screenshot({path:`${out}/home.png`, fullPage:true});
console.log(JSON.stringify(meta,null,2));
await browser.close();
