import fs from 'node:fs/promises';

const NAWL_URL = 'https://raw.githubusercontent.com/DavidAyliffe/AnalyseIMSCC/master/WordLists/NAWL.txt';
const DICT_ROOT = 'https://raw.githubusercontent.com/VahidN/EnglishToPersianDictionaries/master/Dictionaries';
const OUT = 'android-civil-leitner/app/src/main/assets';

const legalSeed = new Map([
  ['contract','قرارداد'],['obligation','تعهد'],['liability','مسئولیت'],['damages','خسارت'],['remedy','ضمانت اجرا یا راهکار حقوقی'],
  ['breach','نقض تعهد'],['consent','رضایت'],['capacity','اهلیت'],['property','مال یا مالکیت'],['ownership','مالکیت'],
  ['possession','تصرف'],['inheritance','ارث'],['heir','وارث'],['estate','ترکه یا دارایی'],['sale','بیع یا فروش'],
  ['lease','اجاره'],['agency','وکالت یا نمایندگی'],['guarantee','ضمان یا تضمین'],['mortgage','رهن'],['pledge','وثیقه یا رهن'],
  ['company','شرکت'],['shareholder','سهامدار'],['director','مدیر'],['bankruptcy','ورشکستگی'],['insolvency','ناتوانی از پرداخت دیون'],
  ['negotiable','قابل انتقال یا قابل معامله'],['instrument','سند'],['bill','برات؛ یا لایحه بسته به متن'],['cheque','چک'],['promissory','تعهدی؛ در promissory note: سفته'],
  ['evidence','دلیل یا ادله'],['burden','بار؛ در burden of proof: بار اثبات'],['claim','ادعا یا خواسته'],['defendant','خوانده'],['plaintiff','خواهان'],
  ['jurisdiction','صلاحیت'],['statute','قانون مصوب']
]);

const examPriority = [
  'autonomy','autonomous','indispensable','beneficial','compromise','demolish','demolition',
  'distort','distorted','plausible','spontaneous','impose','diminish','longevity','inevitable','tangible','endeavor'
];

const clean = (s) => String(s ?? '').replace(/\s+/g,' ').trim();
const normalize = (s) => clean(s).toLowerCase().replace(/[’']/g,"'");
const firstLetter = (word) => (normalize(word).match(/[a-z]/)?.[0] || 'a').toUpperCase();

async function fetchJson(url) {
  const r = await fetch(url, {headers:{'user-agent':'Mizan-PhD140-Curriculum/1.0'}});
  if (!r.ok) throw new Error(`fetch failed ${r.status}: ${url}`);
  return r.json();
}

async function loadDictionary(name, letters) {
  const map = new Map();
  await Promise.all([...letters].map(async letter => {
    const data = await fetchJson(`${DICT_ROOT}/${name}/${letter}.json`);
    for (const item of data.Words || []) {
      const word = normalize(item.EnglishWord);
      if (!word || map.has(word)) continue;
      const meanings = (item.Meanings || []).map(clean).filter(Boolean);
      if (meanings.length) map.set(word, meanings.join('، '));
    }
  }));
  return map;
}

const response = await fetch(NAWL_URL);
if (!response.ok) throw new Error(`NAWL fetch failed: ${response.status}`);
const raw = await response.text();
const nawl = raw.split(/\r?\n/)
  .filter(line => line && !line.startsWith('#') && !/^\s/.test(line))
  .map(normalize)
  .filter(Boolean);
if (nawl.length !== 963) throw new Error(`Expected 963 NAWL lemmas, got ${nawl.length}`);

const ordered = [];
const seen = new Set();
const push = (word) => { const w=normalize(word); if(w && !seen.has(w)){seen.add(w);ordered.push(w);} };

// Exam recurrence first, then the complete NAWL, then legal English until exactly 1000 unique cards.
examPriority.forEach(push);
nawl.forEach(push);
for (const word of legalSeed.keys()) push(word);
if (ordered.length < 1000) throw new Error(`Only ${ordered.length} unique high-yield words available`);
const words = ordered.slice(0,1000);

const letters = new Set(words.map(firstLetter));
const [essential2, law] = await Promise.all([
  loadDictionary('essential-english-words-2', letters),
  loadDictionary('law', letters),
]);

let missing = words.filter(w => !legalSeed.has(w) && !essential2.has(w) && !law.has(w));
let generic = new Map();
if (missing.length) {
  generic = await loadDictionary('generic-1', new Set(missing.map(firstLetter)));
  missing = missing.filter(w => !generic.has(w));
}
if (missing.length) {
  throw new Error(`Vocabulary translation gate failed; missing Persian meanings for ${missing.length}: ${missing.slice(0,30).join(', ')}`);
}

const examSet = new Set(examPriority);
const cards = words.map((word,index) => {
  const meaning = legalSeed.get(word) || law.get(word) || essential2.get(word) || generic.get(word);
  const isExam = examSet.has(word);
  const isLegal = legalSeed.has(word) || law.has(word);
  return {
    id:`VOCAB:WORD:${String(index+1).padStart(4,'0')}`,
    domain:'VOCAB',
    ordinal:index+1,
    title:word,
    prompt:`معنی «${word}» چیست؟ بدون دیدن پاسخ، معنی و یک جمله کوتاه بگو.`,
    answer:meaning,
    explanation:isExam
      ? 'اولویت بسیار بالا: این واژه یا خانواده آن در تحلیل آزمون‌های دکتری سنوات مشاهده شده است؛ معنی را در بافت هم تمرین کن.'
      : isLegal
        ? 'واژه مهم حقوقی/دانشگاهی؛ علاوه بر معنی عمومی، کاربرد حقوقی آن را در یک جمله تمرین کن.'
        : 'واژه آکادمیک؛ پس از پاسخ یک هم‌خانواده یا collocation برای آن بساز.',
    sourceName:isExam
      ? 'Past PhD exam frequency layer + user primary English reference'
      : 'NAWL curriculum + Apache-2.0 EnglishToPersianDictionaries supplement',
    sourceUrl:isExam ? 'https://generalenglish.ir/' : 'https://github.com/VahidN/EnglishToPersianDictionaries',
    verificationStatus:isExam ? 'EXAM_PRIORITY_TRANSLATED' : 'OPEN_DATA_TRANSLATED'
  };
});

await fs.mkdir(OUT,{recursive:true});
await fs.writeFile(`${OUT}/vocab_cards.json`, JSON.stringify(cards,null,2), 'utf8');
await fs.writeFile(`${OUT}/vocab_manifest.json`, JSON.stringify({
  count:cards.length,
  target:1000,
  primaryCurriculumReference:'زبان عمومی دکتری زیر ذره‌بین — فایل ارائه‌شده توسط کاربر',
  nawlSource:NAWL_URL,
  persianMeaningSource:'VahidN/EnglishToPersianDictionaries',
  persianMeaningLicense:'Apache-2.0',
  examPriorityWords:examPriority,
  allCardsHavePersianMeaning:cards.every(x=>clean(x.answer).length>0),
  generatedAt:new Date().toISOString()
},null,2), 'utf8');

console.log(JSON.stringify({
  VOCAB_GATE:'PASS',
  count:cards.length,
  examPriority:cards.filter(x=>x.verificationStatus==='EXAM_PRIORITY_TRANSLATED').length,
  translated:cards.filter(x=>clean(x.answer)).length
},null,2));
