import { chromium } from 'playwright';
import fs from 'node:fs/promises';

const out = 'qavanin-probe';
await fs.mkdir(out, { recursive: true });
const urls = [
  'https://qavanin.ir/Law/TreeText/?IDS=12145533825531226090',
  'https://qavanin.ir/Law/PrintText/?IDS=12145533825531226090&font=',
  'https://qavanin.ir/Law/TreeText/?IDS=12021850837713548188',
  'https://qavanin.ir/Law/PrintText/?IDS=12021850837713548188&font=',
];

const direct = [];
for (const url of urls) {
  try {
    const r = await fetch(url, { headers: { 'User-Agent': 'Mozilla/5.0' }, redirect: 'follow' });
    const body = await r.text();
    direct.push({ url, status:r.status, finalUrl:r.url, length:body.length, prefix:body.slice(0,300) });
    const key = url.includes('121455') ? 'trade' : 'civil';
    const mode = url.includes('PrintText') ? 'print' : 'tree';
    await fs.writeFile(`${out}/${key}-${mode}.html`, body);
  } catch (e) {
    direct.push({ url, error:String(e) });
  }
}
await fs.writeFile(`${out}/direct.json`, JSON.stringify(direct,null,2));
console.log('DIRECT=' + JSON.stringify(direct));

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ locale: 'fa-IR' });
page.setDefaultTimeout(30000);
for (const [i,url] of urls.entries()) {
  try {
    const resp = await page.goto(url, { waitUntil:'domcontentloaded', timeout:60000 });
    await page.waitForTimeout(1500);
    const body = await page.locator('body').innerText().catch(()=> '');
    console.log(`PAGE${i}=status:${resp?.status()} chars:${body.length} url:${page.url()}`);
    await fs.writeFile(`${out}/page-${i}.txt`, body);
  } catch (e) {
    console.log(`PAGE${i}=ERROR ${String(e)}`);
  }
}
await browser.close();
