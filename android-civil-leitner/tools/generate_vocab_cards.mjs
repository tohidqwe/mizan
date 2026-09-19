import fs from 'node:fs/promises';

const NAWL_URL='https://raw.githubusercontent.com/DavidAyliffe/AnalyseIMSCC/master/WordLists/NAWL.txt';
const DICT_ROOT='https://raw.githubusercontent.com/VahidN/EnglishToPersianDictionaries/master/Dictionaries';
const OUT='android-civil-leitner/app/src/main/assets';
const LETTERS='ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

const legalSeed=new Map([
 ['contract','قرارداد'],['obligation','تعهد'],['liability','مسئولیت'],['damages','خسارت'],['remedy','ضمانت اجرا یا راهکار حقوقی'],
 ['breach','نقض تعهد'],['consent','رضایت'],['capacity','اهلیت'],['property','مال یا مالکیت'],['ownership','مالکیت'],
 ['possession','تصرف'],['inheritance','ارث'],['heir','وارث'],['estate','ترکه یا دارایی'],['sale','بیع یا فروش'],
 ['lease','اجاره'],['agency','وکالت یا نمایندگی'],['guarantee','ضمان یا تضمین'],['mortgage','رهن'],['pledge','وثیقه یا رهن'],
 ['company','شرکت'],['shareholder','سهامدار'],['director','مدیر'],['bankruptcy','ورشکستگی'],['insolvency','ناتوانی از پرداخت دیون'],
 ['evidence','دلیل یا ادله'],['claim','ادعا یا خواسته'],['defendant','خوانده'],['plaintiff','خواهان'],['jurisdiction','صلاحیت'],
 ['statute','قانون مصوب'],['void','باطل'],['voidable','قابل ابطال'],['valid','معتبر'],['enforceable','قابل اجرا'],['tort','مسئولیت مدنی یا شبه‌جرم'],
 ['negligence','تقصیر یا بی‌احتیاطی'],['assignment','انتقال حق'],['waiver','اسقاط حق'],['settlement','سازش'],['trust','اعتماد؛ در حقوق: تراست'],
 ['fiduciary','امانی؛ دارای تکلیف امانت‌داری'],['damages','خسارت'],['compensation','جبران خسارت'],['indemnity','جبران یا تضمین خسارت']
]);

const manual=new Map([
 ['distorted','تحریف‌شده؛ دگرگون‌شده'],['artwork','اثر هنری'],['chloride','کلرید'],['ex','سابق؛ پیشین'],
 ['founds','بنیان می‌گذارد؛ تأسیس می‌کند'],['headquarter','مقر؛ ستاد مرکزی'],['historically','از نظر تاریخی'],
 ['individually','به‌صورت فردی؛ جداگانه'],['interviewer','مصاحبه‌کننده'],['morphological','ریخت‌شناختی؛ صرفی'],
 ['multi','چند؛ چندگانه'],['philosophical','فلسفی'],['pre','پیش؛ پیش از'],['randomly','به‌صورت تصادفی'],
 ['tech','فناوری؛ فنی'],['trans','فرا؛ آن‌سوی؛ در ترکیبات به‌معنای عبور یا انتقال']
]);

const examPriority=['autonomy','autonomous','indispensable','beneficial','compromise','demolish','demolition','distort','distorted','plausible','spontaneous','impose','diminish','longevity','inevitable','tangible','endeavor'];
const clean=s=>String(s??'').replace(/\s+/g,' ').trim();
const normalize=s=>clean(s).toLowerCase().replace(/[’']/g,"'");

async function fetchJson(url){
 const r=await fetch(url,{headers:{'user-agent':'Doctor-Tohid-PhD-Course/1.0'}});
 if(!r.ok) throw new Error('fetch failed '+r.status+': '+url);
 const t=await r.text();
 return JSON.parse(t.replace(/^\uFEFF/,''));
}

async function loadDictionary(name){
 const map=new Map();
 for(const letter of LETTERS){
  try{
   const data=await fetchJson(`${DICT_ROOT}/${name}/${letter}.json`);
   for(const item of data.Words||[]){
    const w=normalize(item.EnglishWord);
    const meanings=(item.Meanings||[]).map(clean).filter(Boolean);
    if(w && meanings.length && !map.has(w)) map.set(w,meanings.join('، '));
   }
  }catch(e){
   if(!['X','Y','Z'].includes(letter)) console.warn('dictionary letter skipped',name,letter,String(e));
  }
 }
 return map;
}

const response=await fetch(NAWL_URL);
if(!response.ok) throw new Error('NAWL fetch failed: '+response.status);
const nawl=(await response.text()).split(/\r?\n/)
 .filter(line=>line && !line.startsWith('#') && !/^\s/.test(line))
 .map(normalize).filter(Boolean);
if(nawl.length<900) throw new Error('NAWL unexpectedly short');

const [essential2,law,generic]=await Promise.all([
 loadDictionary('essential-english-words-2'),
 loadDictionary('law'),
 loadDictionary('generic-1'),
]);

const ordered=[]; const seen=new Set();
const push=w=>{w=normalize(w);if(w && !seen.has(w)){seen.add(w);ordered.push(w)}};

examPriority.forEach(push);
nawl.forEach(push);
for(const w of legalSeed.keys()) push(w);

// Keep first 1000 compatible with the previous database IDs, then add 500 legal + 500 general/academic.
for(const w of law.keys()) {
 if(ordered.length>=1500) break;
 if(/^[a-z][a-z'-]{2,}$/.test(w)) push(w);
}
for(const w of essential2.keys()) {
 if(ordered.length>=2000) break;
 if(/^[a-z][a-z'-]{2,}$/.test(w)) push(w);
}
for(const w of generic.keys()) {
 if(ordered.length>=2000) break;
 if(/^[a-z][a-z'-]{2,}$/.test(w)) push(w);
}
if(ordered.length<2000) throw new Error('Only '+ordered.length+' unique English words available');
const words=ordered.slice(0,2000);

const missing=words.filter(w=>!(legalSeed.has(w)||manual.has(w)||law.has(w)||essential2.has(w)||generic.has(w)));
if(missing.length) throw new Error('Missing Persian meanings: '+missing.slice(0,30).join(', '));

const cards=words.map((word,index)=>{
 const meaning=legalSeed.get(word)||manual.get(word)||law.get(word)||essential2.get(word)||generic.get(word);
 const specialized=index>=1000 && (law.has(word)||legalSeed.has(word));
 return {
  id:`VOCAB:WORD:${String(index+1).padStart(4,'0')}`,
  domain:'VOCAB',
  ordinal:index+1,
  title:word,
  prompt:word,
  answer:meaning,
  explanation:'',
  sourceName:specialized?'EnglishToPersianDictionaries — law':'NAWL/EnglishToPersianDictionaries',
  sourceUrl:'https://github.com/VahidN/EnglishToPersianDictionaries',
  verificationStatus:'OPEN_DATA_TRANSLATED'
 };
});

await fs.mkdir(OUT,{recursive:true});
await fs.writeFile(`${OUT}/vocab_cards.json`,JSON.stringify(cards,null,2),'utf8');
await fs.writeFile(`${OUT}/vocab_manifest.json`,JSON.stringify({
 count:2000,
 generalAcademicTarget:1500,
 legalSpecializedTarget:500,
 persianMeaningSource:'VahidN/EnglishToPersianDictionaries',
 license:'Apache-2.0',
 allCardsHavePersianMeaning:cards.every(x=>x.answer.trim().length>0),
 generatedAt:new Date().toISOString()
},null,2),'utf8');
console.log(JSON.stringify({VOCAB_GATE:'PASS',count:cards.length,translated:cards.filter(x=>x.answer.trim()).length},null,2));
