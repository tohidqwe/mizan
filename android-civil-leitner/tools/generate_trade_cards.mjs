import fs from 'node:fs/promises';
import crypto from 'node:crypto';

const OUT='android-civil-leitner/app/src/main/assets';
const QAVANIN_TREE='https://qavanin.ir/Law/TreeText/?IDS=12145533825531226090';
const QAVANIN_PRINT='https://qavanin.ir/Law/PrintText/83457?font=';
const QAVANIN_PRINT_MIRROR='https://vakilfasihi.com/wp-content/uploads/2024/01/%D9%86%D8%B3%D8%AE%D9%87-%DA%86%D8%A7%D9%BE%DB%8C-%D9%82%D8%A7%D9%86%D9%88%D9%86-%D8%AA%D8%AC%D8%A7%D8%B1%D8%AA.pdf';

const fa='۰۱۲۳۴۵۶۷۸۹', ar='٠١٢٣٤٥٦٧٨٩';
const latin=s=>String(s)
 .replace(/[۰-۹]/g,c=>fa.indexOf(c))
 .replace(/[٠-٩]/g,c=>ar.indexOf(c))
 .replace(/[\u200e\u200f\u202a-\u202e\u2066-\u2069]/g,'');
const decode=s=>s
 .replace(/&nbsp;/gi,' ').replace(/&zwnj;/gi,'‌').replace(/&zwj;/gi,'‍')
 .replace(/&laquo;/gi,'«').replace(/&raquo;/gi,'»').replace(/&amp;/gi,'&')
 .replace(/&#(\d+);/g,(_,n)=>String.fromCodePoint(Number(n)))
 .replace(/&#x([0-9a-f]+);/gi,(_,n)=>String.fromCodePoint(parseInt(n,16)));
function visible(html){
 return latin(decode(html
  .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi,' ')
  .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi,' ')
  .replace(/<(br|\/p|\/div|\/li|\/h[1-6]|\/section|\/article)>/gi,'\n')
  .replace(/<[^>]+>/g,' ')))
  .replace(/\r/g,'').replace(/[ \t]+/g,' ').replace(/\n[ \t]+/g,'\n').replace(/\n{3,}/g,'\n\n');
}
const clean=s=>String(s||'').replace(/\s+/g,' ').trim();
async function getText(url){
 const r=await fetch(url,{headers:{'user-agent':'Mozilla/5.0 (Mizan PhD140 legal verifier)'},redirect:'follow'});
 if(!r.ok) throw new Error(`HTTP ${r.status} for ${url}`);
 return visible(await r.text());
}
function parseQavaninPrint(text){
 const normalized=latin(text);
 const labels=[];
 let offset=0;

 for(const line of normalized.split('\n')){
   const detection=line
     .replace(/\p{Cf}/gu,'')
     .replace(/\p{Z}+/gu,' ')
     .replace(/[ \t]+/g,' ')
     .trim();

   const original=detection.match(/ماده\s*[-–—ـ:]\s*(\d{1,3})(?:\s|$)/u);
   if(original){
     labels.push({kind:'ORIGINAL',n:Number(original[1]),start:offset,detected:detection});
   }else{
     const amendment=detection.match(/ماده\s*\(\s*(\d{1,3})/u);
     if(amendment && /(الحاق|اصلاح|منسوخ|حذف)/u.test(detection)){
       labels.push({kind:'AMEND',n:Number(amendment[1]),start:offset,detected:detection});
     }
   }
   offset += line.length + 1;
 }

 if(!labels.length) throw new Error('No Qavanin article headings parsed after Unicode-format normalization');

 const records=labels.map((x,i)=>{
   const end=i+1<labels.length?labels[i+1].start:normalized.length;
   let chunk=normalized.slice(x.start,end).trim();
   // Remove only the first visual heading from the stored answer; tolerate every Unicode separator/control.
   chunk=chunk.replace(/\p{Cf}/gu,'');
   if(x.kind==='ORIGINAL'){
     chunk=chunk.replace(new RegExp('^\\\\s*ماده\\\\s*[-–—ـ:]\\\\s*'+x.n+'\\\\s*[-–—ـ:.]?\\\\s*','u'),'');
   }else{
     chunk=chunk.replace(new RegExp('^\\\\s*ماده\\\\s*\\\\(\\\\s*'+x.n+'[^)]*\\\\)\\\\s*[-–—ـ:.]?\\\\s*','u'),'');
   }
   return {...x,text:clean(chunk)};
 }).filter(x=>x.text.length>=4);

 function exactSequence(kind,count){
   const pool=records.filter(x=>x.kind===kind);
   const chosen=[];
   let expected=1;
   for(const x of pool){
     if(x.n===expected){
       chosen.push({number:x.n,text:x.text});
       expected++;
       if(expected>count) break;
     }
   }
   if(chosen.length!==count){
     const nums=pool.slice(0,120).map(x=>x.n).join(',');
     const samples=labels.slice(0,20).map(x=>x.kind+':'+x.n).join(',');
     throw new Error(\`Qavanin \${kind} sequence failed: \${chosen.length}/\${count}; pool=\${nums}; allSamples=\${samples}\`);
   }
   return chosen;
 }

 return {
   original:exactSequence('ORIGINAL',600),
   amendment:exactSequence('AMEND',300),
   labelCount:labels.length,
 };
}

function legalStatus(collection,n){
 if(collection==='T1311' && n>=21 && n<=93) return 'REPEALED_BY_1347_AMENDMENT_RETAINED';
 if(collection==='T1311' && n===543) return 'DECLARED_INVALID_1403_RETAINED';
 if(collection==='L1347' && (n===51 || (n>=53 && n<=71))) return 'DECLARED_INVALID_1403_RETAINED';
 return 'CURRENT_OR_AMENDED';
}
function cueAnalysis(text,status){
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
 const retained=status!=='CURRENT_OR_AMENDED';
 return {
  simple: retained
   ? 'این ماده برای شناخت سیر تقنینی حذف نشده است؛ متن تاریخی را یاد بگیر اما در پاسخ به سؤالِ حکم جاری، وضعیت نسخ/بی‌اعتباری آن را حتماً ذکر کن.'
   : 'ابتدا موضوع، شخصِ مکلف/ذی‌حق، شرط و اثر حقوقی جمله را جدا کن؛ سپس متن ماده را با همین چهار جزء بازگو کن.',
  analytical: `کلیدهای آزمونی: ${cues.length?cues.join('، '):'موضوع + شرط + اثر حقوقی'}. دام رایج: حفظ عبارت بدون تشخیص قلمرو، شرط و ضمانت اجرا.`
 };
}
const sha=s=>crypto.createHash('sha256').update(s).digest('hex');

await fs.mkdir(OUT,{recursive:true});
const qtxtRaw=await fs.readFile('trade-qavanin-print.txt','utf8');
const qtext=latin(qtxtRaw);
const hasFooter=/Qavanin\.ir/i.test(qtext) && /83457/.test(qtext);
if(!hasFooter) throw new Error('Official Qavanin-generated print export provenance was not verified');
const qavaninPrintVerified=true;
const qavaninPrintHash=sha(qtxtRaw);

const parsed=parseQavaninPrint(qtext);
const legacy=parsed.original;
const amend=parsed.amendment;

if(!/امور\s+تجارتی|امور\s+تجار[يی]/.test(legacy[20].text)) {
 throw new Error('Original Article 21 spot-check failed');
}
if(!/مسئولیت\s+محدود/.test(legacy[93].text)) {
 throw new Error('Original Article 94 spot-check failed');
}
if(!/شرکت(?:های|‌های|ها)\s+دولتی|شرکتهای\s+دولتی/.test(amend[299].text)) {
 throw new Error('Amendment Article 300 spot-check failed');
}
const amendmentExtractionSource='Qavanin-generated print export '+QAVANIN_PRINT;

const cards=[];
for(const [collection,items] of [['T1311',legacy],['L1347',amend]]){
 for(const item of items){
  const status=legalStatus(collection,item.number);
  const a=cueAnalysis(item.text,status);
  const prefix=collection==='T1311'?'قانون تجارت ۱۳۱۱':'لایحه اصلاحی ۱۳۴۷';
  cards.push({
   id:`TRADE:${collection}:${String(item.number).padStart(3,'0')}`,
   domain:'TRADE',
   ordinal:cards.length+1,
   title:`${prefix} — ماده ${item.number}`,
   prompt:`حکم ماده ${item.number} ${prefix} چیست؟ موضوع، شرط و اثر آن را قبل از دیدن پاسخ بازگو کن.`,
   answer:item.text,
   explanation:`${a.simple}\n${a.analytical}\nوضعیت: ${status}`,
   sourceName:'سامانه ملی قوانین و مقررات (Qavanin.ir) — مرجع canonical؛ کنترل با نسخه چاپی تولیدشده توسط سامانه',
   sourceUrl:QAVANIN_TREE,
   verificationStatus:status
  });
 }
}
if(cards.length!==900) throw new Error(`Trade gate: expected 900 cards got ${cards.length}`);
const ids=new Set(cards.map(x=>x.id)); if(ids.size!==900) throw new Error('Trade duplicate IDs');
if(cards.some(x=>!x.answer.trim()||!x.explanation.trim())) throw new Error('Trade blank text/explanation');

const manifest={
 law:'قانون تجارت ۱۳۱۱ + لایحه قانونی اصلاح قسمتی از قانون تجارت ۱۳۴۷',
 canonicalQavaninTree:QAVANIN_TREE,
 qavaninPrintUrl:QAVANIN_PRINT,
 qavaninPrintMirrorUsedForMachineVerification:QAVANIN_PRINT_MIRROR,
 qavaninGeneratedPrintVerified,
 qavaninPrintTextSha256:qavaninPrintHash,
 originalCount:600,
 amendmentCount:300,
 totalUnits:900,
 originalLegacyArticlesRetained:'21-93',
 originalLegacyStatus:'REPEALED_BY_1347_AMENDMENT_RETAINED',
 invalidity1403Retained:{trade1311:[543],amendment1347:[51,'53-71']},
 extractionFallbacks:{
  historicalOriginalText:'Qavanin-generated print export '+QAVANIN_PRINT,
  amendmentText:amendmentExtractionSource,
  note:'Both the complete 600-article 1311 law (including historical repealed 21-93) and the complete 300-article 1347 amendment are extracted from the Qavanin-generated print export. Repealed provisions are retained and visibly labeled.'
 },
 generatedAt:new Date().toISOString(),
 cardSha256:sha(JSON.stringify(cards))
};
await fs.writeFile(`${OUT}/trade_cards.json`,JSON.stringify(cards,null,2),'utf8');
await fs.writeFile(`${OUT}/trade_source_manifest.json`,JSON.stringify(manifest,null,2),'utf8');
console.log(JSON.stringify({TRADE_GATE:'PASS',count:cards.length,qavaninPrintVerified,legacyRetained:73},null,2));
