import fs from 'node:fs/promises';

const OUT='android-civil-leitner/app/src/main/assets';
const WORDS='https://raw.githubusercontent.com/amirj4m/openjam/main/data/json/words_en.json';
const TRANS='https://raw.githubusercontent.com/amirj4m/openjam/main/data/json/translations_fa.json';
const SOURCE='https://github.com/amirj4m/openjam';

async function getJson(url){
  const r=await fetch(url,{headers:{'user-agent':'dr-tohid-phd-course-builder/1.0'}});
  if(!r.ok) throw new Error('Fetch failed '+r.status+' '+url);
  return r.json();
}

const [words, translations]=await Promise.all([getJson(WORDS),getJson(TRANS)]);
const trBySense=new Map();
for(const t of translations){
  if(t.language_code!=='fa') continue;
  const value=String(t.meaning||'').trim();
  if(!value) continue;
  if(!trBySense.has(t.sense_id)) trBySense.set(t.sense_id,[]);
  trBySense.get(t.sense_id).push(value);
}

const legalPriority=new Set([
  'law','legal','right','rights','contract','agreement','obligation','duty','liability','liable','property',
  'ownership','owner','possession','possess','sale','sell','buyer','seller','lease','tenant','landlord',
  'mortgage','pledge','guarantee','guarantor','agency','agent','principal','company','corporation','share',
  'shareholder','partner','partnership','commerce','commercial','merchant','trade','transaction','payment',
  'debt','debtor','creditor','claim','damages','damage','compensation','breach','valid','invalid','void',
  'consent','intention','capacity','minor','guardian','marriage','divorce','inheritance','heir','will',
  'estate','evidence','proof','witness','testimony','court','judge','judgment','action','dispute','remedy',
  'condition','term','notice','fraud','mistake','coercion','duress','negligence','fault','risk','cause'
]);

const candidates=words.map(w=>{
  const meanings=[];
  for(const s of (w.senses||[])){
    for(const m of (trBySense.get(s.id)||[])) if(!meanings.includes(m)) meanings.push(m);
  }
  return {...w,meanings:meanings.slice(0,4)};
}).filter(w=>w.english && w.meanings.length);

candidates.sort((a,b)=>{
  const ap=legalPriority.has(a.english.toLowerCase())?0:1;
  const bp=legalPriority.has(b.english.toLowerCase())?0:1;
  if(ap!==bp) return ap-bp;
  const af=a.frequency_rank ?? 99999999;
  const bf=b.frequency_rank ?? 99999999;
  return af-bf || a.english.localeCompare(b.english);
});

const seen=new Set();
const selected=[];
for(const w of candidates){
  const key=w.english.trim().toLowerCase();
  if(!/^[a-z][a-z '\-]*$/i.test(key) || seen.has(key)) continue;
  seen.add(key);
  selected.push(w);
  if(selected.length===2000) break;
}
if(selected.length!==2000) throw new Error('Expected 2000 English words; got '+selected.length);

const cards=selected.map((w,i)=>({
  id:'ENGLISH:'+String(i+1).padStart(4,'0'),
  domain:'ENGLISH',
  ordinal:i+1,
  title:w.english,
  prompt:w.english,
  answer:w.meanings.join('؛ '),
  explanation:'',
  sourceName:'Openjam — MIT multilingual vocabulary dataset',
  sourceUrl:SOURCE,
  verificationStatus:'OPENJAM_FA_TRANSLATION'
}));

await fs.mkdir(OUT,{recursive:true});
await fs.writeFile(OUT+'/vocab_cards.json',JSON.stringify(cards,null,2)+'\n','utf8');
await fs.writeFile(OUT+'/vocab_manifest.json',JSON.stringify({
  count:cards.length,
  uniqueEnglish:new Set(cards.map(x=>x.prompt.toLowerCase())).size,
  allCardsHavePersianMeaning:cards.every(x=>x.answer.trim().length>0),
  source:SOURCE,
  policy:'2000 unique high-frequency/general words with legal/private-law priority terms promoted when available.'
},null,2)+'\n','utf8');

console.log(JSON.stringify({ENGLISH_2000_GATE:'PASS',count:cards.length,first:cards[0].prompt,last:cards.at(-1).prompt},null,2));
