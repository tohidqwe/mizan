import { chromium } from 'playwright';
import fs from 'node:fs/promises';

const OFFICIAL_URL='https://qavanin.ir/Law/TreeText/?IDS=12021850837713548188';
const PRINT_URL='https://qavanin.ir/Law/PrintText/?IDS=12021850837713548188&font=';
const OUT='civil-law-mind-game/generated';
await fs.mkdir(OUT,{recursive:true});

const fa='۰۱۲۳۴۵۶۷۸۹', ar='٠١٢٣٤٥٦٧٨٩';
const latin=s=>String(s)
  .replace(/[۰-۹]/g,c=>String(fa.indexOf(c)))
  .replace(/[٠-٩]/g,c=>String(ar.indexOf(c)));
const clean=s=>String(s||'')
  .replace(/[\u200e\u200f\u202a-\u202e\u2066-\u2069]/g,'')
  .replace(/\s+/g,' ')
  .trim();

const browser=await chromium.launch({headless:true});
const ctx=await browser.newContext({
  locale:'fa-IR',
  timezoneId:'Asia/Tehran',
  userAgent:'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/140 Safari/537.36'
});
const page=await ctx.newPage();
page.setDefaultTimeout(45000);

let loaded=false;
for(const url of [PRINT_URL,OFFICIAL_URL]){
  try{
    await page.goto(url,{waitUntil:'domcontentloaded',timeout:90000});
    for(let i=0;i<18;i++){
      await page.waitForTimeout(1800);
      const count=await page.locator('p.SecTex span.bold').count().catch(()=>0);
      const body=await page.locator('body').innerText().catch(()=>'');
      if(count>1300 && /ماد[ۀه]\s*183/u.test(body) && /ماد[ۀه]\s*232/u.test(body)){
        loaded=true; break;
      }
    }
    if(loaded) break;
  }catch{}
}
if(!loaded){
  await browser.close();
  throw new Error('Qavanin Civil Code page did not load completely');
}

const rows=await page.locator('p.SecTex').evaluateAll(nodes=>nodes.map(p=>({
  label:p.querySelector('span.bold')?.textContent||'',
  full:p.textContent||''
})));
await browser.close();

const parsed=[];
let current=null;
const finish=()=>{
  if(!current) return;
  current.officialText=clean(current.parts.join(' '));
  delete current.parts;
  if(current.officialText) parsed.push(current);
  current=null;
};
for(const row of rows){
  const label=clean(latin(row.label)).replace(/مادۀ/g,'ماده').replace(/مادهٔ/g,'ماده');
  const full=clean(latin(row.full)).replace(/مادۀ/g,'ماده').replace(/مادهٔ/g,'ماده');
  const m=label.match(/^ماده\s*(\d{1,4})\s*(مکرر|مكرر)?/u);
  if(m){
    finish();
    const n=Number(m[1]);
    const suffix=m[2]?'مکرر':'';
    let text=full;
    if(text.startsWith(label)) text=text.slice(label.length).trim();
    text=text.replace(/^[-–—ـ:]\s*/u,'');
    current={articleNumber:n,suffix,parts:[text]};
    continue;
  }
  if(/^تبصره/u.test(label) && current) current.parts.push(full);
}
finish();

const levels=parsed
  .filter(x=>x.articleNumber>=183 && x.articleNumber<=232 && !x.suffix)
  .sort((a,b)=>a.articleNumber-b.articleNumber);

if(levels.length!==50) throw new Error('Expected 50 main articles from 183 to 232, got '+levels.length);
for(let n=183;n<=232;n++){
  if(!levels.some(x=>x.articleNumber===n)) throw new Error('Missing official article '+n);
}
if(levels.some(x=>x.officialText.length<12)) throw new Error('Blank/short official article text');

await fs.writeFile(`${OUT}/madani3_official.json`,JSON.stringify({
  source:'Qavanin.ir',
  url:OFFICIAL_URL,
  fetchedAt:new Date().toISOString(),
  articles:levels,
},null,2)+'\n','utf8');

console.log(JSON.stringify({QAVANIN_MADANI3_GATE:'PASS',count:levels.length,first:183,last:232},null,2));
