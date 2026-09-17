import fs from 'node:fs/promises';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);

const NAWL_URL = 'https://raw.githubusercontent.com/DavidAyliffe/AnalyseIMSCC/master/WordLists/NAWL.txt';
const extraLegal = [
  ['contract','قرارداد'],['obligation','تعهد'],['liability','مسئولیت'],['damages','خسارت'],['remedy','راهکار یا ضمانت اجرای حقوقی'],
  ['breach','نقض تعهد'],['consent','رضایت'],['capacity','اهلیت'],['property','مال یا مالکیت'],['ownership','مالکیت'],
  ['possession','تصرف'],['inheritance','ارث'],['heir','وارث'],['estate','ترکه یا دارایی'],['sale','بیع یا فروش'],
  ['lease','اجاره'],['agency','وکالت یا نمایندگی'],['guarantee','ضمان یا تضمین'],['mortgage','رهن'],['pledge','وثیقه یا رهن'],
  ['company','شرکت'],['shareholder','سهامدار'],['director','مدیر'],['bankruptcy','ورشکستگی'],['insolvency','ناتوانی از پرداخت دیون'],
  ['negotiable','قابل انتقال / قابل معامله'],['instrument','سند'],['bill','برات / لایحه، بسته به متن'],['cheque','چک'],['promissory','تعهدی؛ در promissory note: سفته'],
  ['evidence','دلیل / ادله'],['burden','بار؛ در burden of proof: بار اثبات'],['claim','ادعا / خواسته'],['defendant','خوانده'],['plaintiff','خواهان'],
  ['jurisdiction','صلاحیت'],['statute','قانون مصوب']
];
const curated = new Map(extraLegal);

function flattenMeaning(value){
  if(value == null) return '';
  if(typeof value === 'string') return value;
  if(Array.isArray(value)) return value.map(flattenMeaning).filter(Boolean).join('، ');
  if(typeof value === 'object') return Object.values(value).map(flattenMeaning).filter(Boolean).join('، ');
  return String(value);
}
function clean(s){return String(s||'').replace(/\s+/g,' ').trim().slice(0,700);}

const response = await fetch(NAWL_URL);
if(!response.ok) throw new Error(`NAWL fetch failed: ${response.status}`);
const raw = await response.text();
const lemmas = raw.split(/\r?\n/).filter(line => line && !line.startsWith('#') && !/^\s/.test(line)).map(x=>x.trim().toLowerCase());
if(lemmas.length !== 963) throw new Error(`Expected 963 NAWL lemmas, got ${lemmas.length}`);
const words = [...lemmas];
for(const [word] of extraLegal) if(!words.includes(word)) words.push(word);
if(words.length < 1000) {
  const supplement = ['appeal','judgment','court','lawful','unlawful','valid','void','voidable','revocation','assignment','creditor','debtor','debt','settlement','arbitration','waiver','rescission','compensation'];
  for(const w of supplement) if(!words.includes(w) && words.length < 1000) words.push(w);
}
if(words.length !== 1000) throw new Error(`Vocabulary gate failed: expected 1000 unique words, got ${words.length}`);

let dictionary = {};
try { dictionary = require('english-persian-dictionary'); } catch (e) { console.warn('Dictionary package unavailable; curated/fallback meanings will be used:', e.message); }
let translated = 0;
const cards = words.map((word,index)=>{
  let meaning = curated.get(word) || clean(flattenMeaning(dictionary[word]));
  if(meaning) translated++;
  else meaning = 'معنی را از متن استنباط کن، سپس معادل درست فارسی را در یادداشت کارت ثبت کن.';
  return {
    id:`VOCAB:${index+1}`, domain:'VOCAB', ordinal:index+1, title:word,
    prompt:`معنی «${word}» چیست؟ ابتدا بدون دیدن پاسخ، معنی و یک کاربرد احتمالی را بگو.`,
    answer:meaning,
    explanation:'پس از پاسخ، یک جمله کوتاه دانشگاهی یا حقوقی با این واژه بساز. اگر اشتباه بود «نیاز به مرور» را فعال نگه دار.',
    sourceName:index < 963 ? 'New Academic Word List (NAWL) — 963 lemmas' : 'Curated high-yield legal English supplement',
    sourceUrl:index < 963 ? NAWL_URL : '',
    verificationStatus:'CURATED_OPEN_DATA'
  };
});
await fs.mkdir('android-civil-leitner/app/src/main/assets',{recursive:true});
await fs.writeFile('android-civil-leitner/app/src/main/assets/vocab_cards.json', JSON.stringify(cards,null,2));
await fs.writeFile('android-civil-leitner/app/src/main/assets/vocab_manifest.json', JSON.stringify({count:cards.length,nawl:963,supplement:37,translatedOrCurated:translated,source:NAWL_URL,dictionaryPackage:'english-persian-dictionary@1.1.0 Apache-2.0'},null,2));
console.log(JSON.stringify({VOCAB_GATE:'PASS',count:cards.length,translatedOrCurated:translated,fallback:cards.length-translated},null,2));
