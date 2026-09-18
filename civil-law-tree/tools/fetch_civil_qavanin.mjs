import { chromium } from 'playwright';
import fs from 'node:fs/promises';

const OFFICIAL_URL = 'https://qavanin.ir/Law/TreeText/?IDS=12021850837713548188';
const PRINT_URL = 'https://qavanin.ir/Law/PrintText/?IDS=12021850837713548188&font=';
const OUT = 'civil-law-tree/generated';

const fa='۰۱۲۳۴۵۶۷۸۹', ar='٠١٢٣٤٥٦٧٨٩';
const latinDigits=s=>String(s)
  .replace(/[۰-۹]/g,c=>String(fa.indexOf(c)))
  .replace(/[٠-٩]/g,c=>String(ar.indexOf(c)));
const clean=s=>String(s||'')
  .replace(/مادۀ/g,'ماده')
  .replace(/مادهٔ/g,'ماده')
  .replace(/[\u200e\u200f\u202a-\u202e\u2066-\u2069]/g,'')
  .replace(/\s+/g,' ')
  .trim();

await fs.mkdir(OUT,{recursive:true});

const browser=await chromium.launch({headless:true});
const context=await browser.newContext({
  locale:'fa-IR',
  timezoneId:'Asia/Tehran',
  userAgent:'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/140 Safari/537.36'
});
const page=await context.newPage();
page.setDefaultTimeout(45000);

let loaded=false;
for(const url of [PRINT_URL,OFFICIAL_URL]){
  try{
    await page.goto(url,{waitUntil:'domcontentloaded',timeout:90000});
    for(let i=0;i<18;i++){
      await page.waitForTimeout(2000);
      const count=await page.locator('p.SecTex span.bold').count().catch(()=>0);
      const body=await page.locator('body').innerText().catch(()=>'');
      if(count>1300 && /قانون\s+مدن[يی]/u.test(body) && /ماد[ۀه]\s*1335/u.test(body)){
        loaded=true;
        break;
      }
    }
    if(loaded) break;
  }catch(e){
    await fs.writeFile(`${OUT}/last_fetch_error.txt`,String(e),'utf8');
  }
}

if(!loaded){
  await browser.close();
  throw new Error('Official Qavanin Civil Code page did not become complete');
}

const rows=await page.locator('p.SecTex').evaluateAll(nodes=>nodes.map(p=>{
  const label=p.querySelector('span.bold')?.textContent||'';
  return {label,full:p.textContent||''};
}));

const structure={book:'',part:'',chapter:'',section:''};
const parsed=[];
let current=null;

function finish(){
  if(!current) return;
  current.officialText=clean(current.parts.join(' '));
  delete current.parts;
  if(current.officialText) parsed.push(current);
  current=null;
}
function status(label){
  if(/منسوخ|حذف شده|فاقد اعتبار/u.test(label)) return 'REPEALED';
  if(/اصلاح[يی]|الحاق[يی]|جایگزین/u.test(label)) return 'ACTIVE_AMENDED';
  return 'ACTIVE';
}

for(const row of rows){
  const label=clean(latinDigits(row.label));
  const full=clean(latinDigits(row.full));
  const m=label.match(/^ماده\s*(\d{1,4})\s*(مکرر|مكرر)?\s*(.*)$/u);
  if(m){
    finish();
    const number=Number(m[1]);
    const suffix=m[2]?'مکرر':'';
    let text=full;
    if(text.startsWith(clean(latinDigits(row.label)))) text=text.slice(clean(latinDigits(row.label)).length).trim();
    text=text.replace(/^[-–—ـ:]\s*/u,'');
    current={
      articleNumber:number,
      suffix,
      articleKey:`${number}${suffix}`,
      legalStatus:status(label),
      label,
      book:structure.book,
      part:structure.part,
      chapter:structure.chapter,
      section:structure.section,
      parts:[text]
    };
    continue;
  }

  if(/^تبصره/u.test(label) && current){
    current.parts.push(full);
    continue;
  }

  if(/^(کتاب|كتاب)/u.test(label)){
    finish(); structure.book=full; structure.part=''; structure.chapter=''; structure.section='';
  }else if(/^باب/u.test(label)){
    finish(); structure.part=full; structure.chapter=''; structure.section='';
  }else if(/^فصل/u.test(label)){
    finish(); structure.chapter=full; structure.section='';
  }else if(/^(مبحث|قسمت)/u.test(label)){
    finish(); structure.section=full;
  }
}
finish();
await browser.close();

const byKey=new Map();
for(const item of parsed){
  if(item.articleNumber<1 || item.articleNumber>1335) continue;
  if(!byKey.has(item.articleKey)) byKey.set(item.articleKey,item);
}
const all=[...byKey.values()];
const main=all.filter(x=>!x.suffix).sort((a,b)=>a.articleNumber-b.articleNumber);
const supplemental=all.filter(x=>x.suffix).sort((a,b)=>a.articleNumber-b.articleNumber);

if(main.length!==1335) throw new Error(`Expected 1335 main articles, got ${main.length}`);
for(let n=1;n<=1335;n++){
  if(main[n-1]?.articleNumber!==n) throw new Error(`Civil numbering mismatch at ${n}`);
}
if(main.some(x=>!x.officialText.trim())) throw new Error('Blank official Civil Code text found');

const payload={
  source:{name:'سامانه ملی قوانین و مقررات جمهوری اسلامی ایران',url:OFFICIAL_URL,retrievedUrl:page.url()},
  fetchedAt:new Date().toISOString(),
  main,
  supplemental
};
await fs.writeFile(`${OUT}/qavanin_civil_raw.json`,JSON.stringify(payload,null,2)+'\n','utf8');
console.log(JSON.stringify({
  QAVANIN_FETCH:'PASS',
  main:main.length,
  supplemental:supplemental.length,
  first:main[0].articleNumber,
  last:main.at(-1).articleNumber
},null,2));
