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

const normalizeDisplayText = (s) => clean(String(s || '')
  .replace(/ك/g, 'ک')
  .replace(/[يى]/g, 'ی')
  .replace(/ة/g, 'ه')
  .replace(/ۀ/g, 'هٔ')
  .replace(/معامالت/g, 'معاملات')
  .replace(/تجارتي/g, 'تجارتی')
  .replace(/ميشود/g, 'می‌شود')
  .replace(/مي\s+شود/g, 'می‌شود')
  .replace(/ميباشد/g, 'می‌باشد')
  .replace(/مي\s+باشد/g, 'می‌باشد')
  .replace(/بموجب/g, 'به موجب')
  .replace(/بترتيب/g, 'به ترتیب')
  .replace(/بهيچوجه/g, 'به هیچ‌وجه')
  .replace(/بامور/g, 'به امور')
  .replace(/باعتبار/g, 'به اعتبار')
  .replace(/بمبلغ/g, 'به مبلغ')
  .replace(/بانك/g, 'بانک')
  .replace(/الاقل/g, 'لااقل')
  .replace(/\s+([،؛:.])/g, '$1')
  .replace(/([،؛:.])(?=\S)/g, '$1 '));

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
  return /(منسوخ|نسخ\s*شده|حذف\s*شده|حذف[یي]|باطل\s*شده|ابطال\s*شده|ملغ[یي]|فاقد\s*اعتبار)/u.test(label);
}

function classifyHeading(line){
  const x=clean(line.replace(/\p{Cf}/gu,'').replace(/\p{Z}+/gu,' '));

  // Qavanin PDF renders ordinary articles as: "ماده - 1 ..."
  const ordinary=x.match(/^‌?ماده\s*[-–—ـ:]?\s*(\d{1,3})(?:\s*[-–—ـ:]\s*)?(.*)$/u);
  if(ordinary){
    return {kind:'ORIGINAL',number:Number(ordinary[1]),statusLabel:'',rest:clean(ordinary[2]||''),heading:x};
  }

  // 1347 amendment is rendered as: "ماده (1 الحاقي -)1347/12/24 ..."
  const paren=x.match(/^‌?ماده\s*\(\s*(\d{1,3})\s*([^)]*)\)\s*(.*)$/u);
  if(paren){
    const inside=clean(paren[2]||'');
    const tail=clean(paren[3]||'');
    if(/الحاق|اصلاح|منسوخ|حذف|ابطال|اعتبار/u.test(inside+' '+tail)){
      const datePrefix=tail.match(/^(\d{4}[\/ˏ.-]\d{1,2}[\/ˏ.-]\d{1,2})\s*[-–—ـ:]?\s*(.*)$/u);
      const rest=datePrefix ? clean(datePrefix[2]||'') : tail;
      const date=datePrefix ? clean(datePrefix[1]||'') : '';
      return {
        kind:'AMEND',
        number:Number(paren[1]),
        statusLabel:clean([inside,date].filter(Boolean).join(' ')),
        rest,
        heading:x
      };
    }
  }
  return null;
}

function parseQavaninCurrent(raw){
  const lines=latin(raw).replace(/\r/g,'').split('\n');
  const records=[];
  let current=null;

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

    const heading=classifyHeading(line);
    if(heading){
      finish();
      current={...heading,parts:[heading.rest]};
      continue;
    }
    if(current) current.parts.push(line);
  }
  finish();

  const originalAll=records.filter(x=>x.kind==='ORIGINAL' && x.number>=1 && x.number<=600);
  const amendAll=records.filter(x=>x.kind==='AMEND' && x.number>=1 && x.number<=300);

  // Keep the first occurrence of each numbered provision in each legal collection.
  const unique=(items)=>{
    const m=new Map();
    for(const x of items) if(!m.has(x.number)) m.set(x.number,x);
    return [...m.values()].sort((a,b)=>a.number-b.number);
  };
  const originals=unique(originalAll);
  const amendments=unique(amendAll);

  if(originals[0]?.number!==1 || originals.at(-1)?.number!==600){
    throw new Error(`Qavanin original-law parse failed: first=${originals[0]?.number}; last=${originals.at(-1)?.number}; count=${originals.length}`);
  }
  if(amendments[0]?.number!==1 || amendments.at(-1)?.number!==300){
    throw new Error(`Qavanin 1347-amendment parse failed: first=${amendments[0]?.number}; last=${amendments.at(-1)?.number}; count=${amendments.length}`);
  }

  const originalNumbers=new Set(originals.map(x=>x.number));
  const amendmentNumbers=new Set(amendments.map(x=>x.number));
  const omittedOriginal=[]; for(let n=1;n<=600;n++) if(!originalNumbers.has(n)) omittedOriginal.push(n);
  const omittedAmendment=[]; for(let n=1;n<=300;n++) if(!amendmentNumbers.has(n)) omittedAmendment.push(n);

  const inactiveOriginal=originals.filter(x=>isExplicitlyInactive(x.heading+' '+x.statusLabel)).map(x=>x.number);
  const inactiveAmendment=amendments.filter(x=>isExplicitlyInactive(x.heading+' '+x.statusLabel)).map(x=>x.number);

  const activeOriginal=originals.filter(x=>!inactiveOriginal.includes(x.number));
  const activeAmendment=amendments.filter(x=>!inactiveAmendment.includes(x.number));

  // Historical Articles 21..93 must not re-enter merely because their old text appears in an appendix.
  // Qavanin consolidated publication replaces that company-law block with the 1347 amendment.
  const currentOriginal=activeOriginal.filter(x=>x.number<21 || x.number>93);

  if(currentOriginal.length<500) throw new Error(`Current original Trade Law corpus unexpectedly small: ${currentOriginal.length}`);
  if(activeAmendment.length<250) throw new Error(`Current 1347 amendment corpus unexpectedly small: ${activeAmendment.length}`);

  return {
    records,
    originals,
    amendments,
    currentOriginal,
    currentAmendment:activeAmendment,
    omittedOriginal,
    omittedAmendment,
    inactiveOriginal,
    inactiveAmendment
  };
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
  const provenance=statusLabel ? `برچسب رسمی Qavanin.ir: ${statusLabel}.` : 'این ماده در متن تنقیحی جاری Qavanin.ir درج شده است.';
  return {
    simple:'ماده را به چهار جزء بشکن: موضوع، شخص مکلف یا ذی‌حق، شرط تحقق و اثر/ضمانت اجرا. سپس همان حکم را با زبان ساده خودت بازگو کن.',
    analytical:`${provenance} کلیدهای آزمونی: ${cues.length?cues.join('، '):'موضوع + شرط + اثر حقوقی'}. دام رایج: حفظ لفظ بدون تشخیص قلمرو، استثنا و ضمانت اجرا.`
  };
}

await fs.mkdir(OUT,{recursive:true});
const raw=await fs.readFile('trade-qavanin-print.txt','utf8');
const normalized=latin(raw);
if(!/Qavanin\.ir/i.test(normalized) || !/83457/.test(normalized)){
  throw new Error('Qavanin-generated print-export provenance could not be verified');
}

const parsed=parseQavaninCurrent(raw);

const originalByNumber=new Map(parsed.currentOriginal.map(x=>[x.number,x]));
const amendmentByNumber=new Map(parsed.currentAmendment.map(x=>[x.number,x]));

for(const [n,needle] of [[1,'تاجر'],[20,'شركت'],[94,'محدود']]){
  const text=originalByNumber.get(n)?.text || '';
  if(!text.includes(needle)) throw new Error(`Qavanin original-law spot-check failed for Article ${n}: ${needle}`);
}
const article600 = originalByNumber.get(600)?.text || '';
if(article600.length < 12) throw new Error('Qavanin original-law Article 600 missing or too short');
for(const [n,needle] of [[1,'سهامي'],[300,'دولتي']]){
  const text=amendmentByNumber.get(n)?.text || '';
  if(!text.includes(needle)) throw new Error(`Qavanin 1347-amendment spot-check failed for Article ${n}: ${needle}`);
}

const cards=[];
function pushCard(collection,label,item){
  const displayText = normalizeDisplayText(item.text);
  const displayStatus = normalizeDisplayText(item.statusLabel);
  const a=cueAnalysis(displayText,displayStatus);
  cards.push({
    id:`TRADE:${collection}:${String(item.number).padStart(3,'0')}`,
    domain:'TRADE',
    ordinal:cards.length+1,
    title:`${label} — ماده ${item.number}${displayStatus?` (${displayStatus})`:''}`,
    prompt:`حکم جاری ماده ${item.number} ${label} چیست؟ موضوع، شرط و اثر آن را قبل از دیدن پاسخ بازگو کن.`,
    answer:displayText,
    explanation:`${a.simple}\n${a.analytical}`,
    sourceName:'سامانه ملی قوانین و مقررات (Qavanin.ir) — متن تنقیحی جاری',
    sourceUrl:QAVANIN_PRINT,
    verificationStatus:item.statusLabel ? 'QAVANIN_CURRENT_ANNOTATED' : 'QAVANIN_CURRENT'
  });
}
for(const item of parsed.currentOriginal) pushCard('T1311','قانون تجارت ۱۳۱۱',item);
for(const item of parsed.currentAmendment) pushCard('L1347','لایحه اصلاحی ۱۳۴۷',item);

const ids=new Set(cards.map(x=>x.id));
if(ids.size!==cards.length) throw new Error('Duplicate trade IDs after Qavanin filtering');
if(cards.some(x=>!x.answer.trim()||!x.explanation.trim())) throw new Error('Blank current trade card');
if(cards.some(x=>/[كيى]/u.test(x.answer))) throw new Error('Unnormalized Arabic glyph leaked into trade display text');
if(cards.some(x=>/معامالت/u.test(x.answer))) throw new Error('Known Qavanin PDF OCR artifact leaked into trade display text');

const manifest={
  law:'قانون تجارت ۱۳۱۱ + لایحه اصلاحی ۱۳۴۷ — فقط مقررات جاری',
  policy:'Only provisions present/current in the Qavanin.ir consolidated publication enter the study/review bank. Repealed original Articles 21-93 are not taught separately because the 1347 amendment replaces that company-law block.',
  canonicalQavaninTree:QAVANIN_TREE,
  qavaninPrintUrl:QAVANIN_PRINT,
  qavaninGeneratedPrintMirror:QAVANIN_PRINT_MIRROR,
  qavaninGeneratedPrintVerified:true,
  sourceOriginalCount:parsed.originals.length,
  sourceAmendmentCount:parsed.amendments.length,
  currentOriginalCount:parsed.currentOriginal.length,
  currentAmendmentCount:parsed.currentAmendment.length,
  activeCardCount:cards.length,
  historicalOriginalBlockExcluded:'21-93',
  omittedOriginalBySource:parsed.omittedOriginal,
  omittedAmendmentBySource:parsed.omittedAmendment,
  explicitlyInactiveOriginal:parsed.inactiveOriginal,
  explicitlyInactiveAmendment:parsed.inactiveAmendment,
  sourceTextSha256:sha(raw),
  cardSha256:sha(JSON.stringify(cards)),
  generatedAt:new Date().toISOString()
};

await fs.writeFile(`${OUT}/trade_cards.json`,JSON.stringify(cards,null,2),'utf8');
await fs.writeFile(`${OUT}/trade_source_manifest.json`,JSON.stringify(manifest,null,2),'utf8');
console.log(JSON.stringify({
  TRADE_GATE:'PASS',
  currentOriginal:parsed.currentOriginal.length,
  currentAmendment:parsed.currentAmendment.length,
  activeCards:cards.length,
  inactiveOriginal:parsed.inactiveOriginal,
  inactiveAmendment:parsed.inactiveAmendment
},null,2));
