import fs from 'node:fs/promises';
import crypto from 'node:crypto';

const OUT='android-civil-leitner/app/src/main/assets';
const QAVANIN_TREE='https://qavanin.ir/Law/TreeText/?IDS=12145533825531226090';
const QAVANIN_PRINT='https://qavanin.ir/Law/PrintText/83457?font=';
const QAVANIN_PRINT_MIRROR='https://vakilfasihi.com/wp-content/uploads/2024/01/%D9%86%D8%B3%D8%AE%D9%87-%DA%86%D8%A7%D9%BE%DB%8C-%D9%82%D8%A7%D9%86%D9%88%D9%86-%D8%AA%D8%AC%D8%A7%D8%B1%D8%AA.pdf';

const fa='۰۱۲۳۴۵۶۷۸۹', ar='٠١٢٣٤٥٦٧٨٩';
const latin=s=>String(s)
  .replace(/[۰-۹]/g,c=>String(fa.indexOf(c)))
  .replace(/[٠-٩]/g,c=>String(ar.indexOf(c)))
  .replace(/[\u200e\u200f\u202a-\u202e\u2066-\u2069]/g,'');
const clean=s=>String(s||'').replace(/\s+/g,' ').trim();
const sha=s=>crypto.createHash('sha256').update(s).digest('hex');

function isFooter(line){
  const x=clean(line);
  return !x ||
    /^\d+\/\d+$/.test(x) ||
    /معاونت تدوین،?\s*تنقیح و انتشار قوانین و مقررات/.test(x) ||
    /Qavanin\.ir/i.test(x) ||
    /qavanin\.ir\/Law\/PrintText\/83457/i.test(x);
}

function isExplicitlyInactive(label){
  return /(منسوخ|نسخ\s*شده|حذف\s*شده|حذف[یي]|باطل\s*شده|ابطال\s*شده|ملغ[یي])/u.test(label);
}

function parseCurrentConsolidatedLaw(raw){
  const text=latin(raw).replace(/\r/g,'');
  const lines=text.split('\n');
  const records=[];
  let current=null;
  let lastNumber=0;
  let started=false;

  const finish=()=>{
    if(!current) return;
    current.text=clean(current.parts.filter(Boolean).join(' '));
    delete current.parts;
    if(current.text.length>=4) records.push(current);
    current=null;
  };

  for(const rawLine of lines){
    const line=clean(rawLine.replace(/\p{Cf}/gu,'').replace(/\p{Z}+/gu,' '));
    if(isFooter(line)) continue;

    const m=line.match(/^ماده\s*(\d{1,3})(?:\s*\(([^)]*)\))?\s*[-–—ـ:]?\s*(.*)$/u);
    if(m){
      const n=Number(m[1]);
      const statusLabel=clean(m[2]||'');
      const rest=clean(m[3]||'');

      if(!started){
        if(n!==1) continue;
        started=true;
      }

      if(n>lastNumber && n<=600){
        finish();
        current={number:n,statusLabel,heading:line,parts:[rest]};
        lastNumber=n;
        if(n===600) continue;
        continue;
      }
    }

    if(current) current.parts.push(line);
  }
  finish();

  if(!records.length || records[0].number!==1 || records.at(-1).number!==600){
    throw new Error(`Qavanin consolidated trade parse failed: first=${records[0]?.number}; last=${records.at(-1)?.number}; count=${records.length}`);
  }

  const seen=new Set(records.map(x=>x.number));
  const omitted=[];
  for(let n=1;n<=600;n++) if(!seen.has(n)) omitted.push(n);

  const explicitInactive=records.filter(x=>isExplicitlyInactive(x.statusLabel+' '+x.heading)).map(x=>x.number);
  const active=records.filter(x=>!explicitInactive.includes(x.number));

  if(records.length<550) throw new Error(`Qavanin source appears truncated: only ${records.length} article headings detected`);
  if(active.length<500) throw new Error(`Too few current trade articles after official-status filtering: ${active.length}`);

  return {records,active,omitted,explicitInactive};
}

function cueAnalysis(text,statusLabel){
  const cues=[];
  if(/مسئولیت\s+تضامنی|متضامناً|متضامن/.test(text)) cues.push('مسئولیت تضامنی');
  if(/باطل|بطلان/.test(text)) cues.push('بطلان');
  if(/حق\s+دارد|حق\s+خواه/.test(text)) cues.push('حق یا اختیار');
  if(/مکلف|باید|موظف/.test(text)) cues.push('تکلیف قانونی');
  if(/ممنوع|نمی\s*توان|نباید/.test(text)) cues.push('ممنوعیت یا محدودیت');
  if(/ورشکست/.test(text)) cues.push('ورشکستگی');
  if(/برات|فته|سفته|چک/.test(text)) cues.push('اسناد تجاری');
  if(/شرکت|سهام|شریک|مجمع|مدیره/.test(text)) cues.push('حقوق شرکت‌ها');
  if(/مرور\s*زمان/.test(text)) cues.push('مرور زمان');

  const provenance=statusLabel ? `وضعیت رسمی در Qavanin.ir: ${statusLabel}.` : 'این ماده در متن تنقیحی جاری Qavanin.ir درج شده است.';
  return {
    simple:'ماده را به چهار جزء بشکن: «موضوع»، «شخص مکلف یا ذی‌حق»، «شرط تحقق» و «اثر/ضمانت اجرا». سپس حکم را با زبان ساده خودت بازگو کن.',
    analytical:`${provenance} کلیدهای آزمونی: ${cues.length?cues.join('، '):'موضوع + شرط + اثر حقوقی'}. دام رایج: حفظ لفظ بدون تشخیص قلمرو و استثنا.`
  };
}

await fs.mkdir(OUT,{recursive:true});
const raw=await fs.readFile('trade-qavanin-print.txt','utf8');
const normalized=latin(raw);
if(!/Qavanin\.ir/i.test(normalized) || !/83457/.test(normalized)){
  throw new Error('Qavanin-generated print-export provenance could not be verified');
}

const parsed=parseCurrentConsolidatedLaw(raw);
const byNumber=new Map(parsed.active.map(x=>[x.number,x]));

for(const [n,needle] of [[21,'سهامی'],[82,'سهامی'],[600,'قوانین']]){
  const text=byNumber.get(n)?.text || '';
  if(!text.includes(needle)) throw new Error(`Qavanin trade spot-check failed for current article ${n}: ${needle}`);
}

const cards=parsed.active.map((item,index)=>{
  const a=cueAnalysis(item.text,item.statusLabel);
  return {
    id:`TRADE:CURRENT:${String(item.number).padStart(3,'0')}`,
    domain:'TRADE',
    ordinal:index+1,
    title:`قانون تجارت تنقیحی — ماده ${item.number}${item.statusLabel?` (${item.statusLabel})`:''}`,
    prompt:`حکم جاری ماده ${item.number} قانون تجارت چیست؟ موضوع، شرط و اثر آن را قبل از دیدن پاسخ بازگو کن.`,
    answer:item.text,
    explanation:`${a.simple}\n${a.analytical}`,
    sourceName:'سامانه ملی قوانین و مقررات (Qavanin.ir) — متن تنقیحی جاری قانون تجارت',
    sourceUrl:QAVANIN_PRINT,
    verificationStatus:item.statusLabel ? 'QAVANIN_CURRENT_ANNOTATED' : 'QAVANIN_CURRENT'
  };
});

const ids=new Set(cards.map(x=>x.id));
if(ids.size!==cards.length) throw new Error('Duplicate trade IDs after Qavanin filtering');
if(cards.some(x=>!x.answer.trim()||!x.explanation.trim())) throw new Error('Blank current trade card');

const manifest={
  law:'قانون تجارت - متن تنقیحی جاری',
  policy:'Only provisions present and not explicitly inactive in the current Qavanin.ir consolidated text enter the study/review bank.',
  canonicalQavaninTree:QAVANIN_TREE,
  qavaninPrintUrl:QAVANIN_PRINT,
  qavaninGeneratedPrintMirror:QAVANIN_PRINT_MIRROR,
  qavaninGeneratedPrintVerified:true,
  sourceHeadingCount:parsed.records.length,
  activeCardCount:cards.length,
  omittedByOfficialConsolidatedText:parsed.omitted,
  explicitlyInactiveInOfficialSource:parsed.explicitInactive,
  amendmentAnnotationsCount:parsed.records.filter(x=>x.statusLabel).length,
  firstSourceArticle:parsed.records[0].number,
  lastSourceArticle:parsed.records.at(-1).number,
  sourceTextSha256:sha(raw),
  cardSha256:sha(JSON.stringify(cards)),
  generatedAt:new Date().toISOString()
};

await fs.writeFile(`${OUT}/trade_cards.json`,JSON.stringify(cards,null,2),'utf8');
await fs.writeFile(`${OUT}/trade_source_manifest.json`,JSON.stringify(manifest,null,2),'utf8');
console.log(JSON.stringify({
  TRADE_GATE:'PASS',
  sourceHeadings:parsed.records.length,
  activeCards:cards.length,
  omitted:parsed.omitted,
  explicitlyInactive:parsed.explicitInactive
},null,2));
