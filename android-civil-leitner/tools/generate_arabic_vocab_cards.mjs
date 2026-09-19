import fs from 'node:fs/promises';

const OUT='android-civil-leitner/app/src/main/assets';
const ENDPOINTS=[
 'https://nebrasar.ir/api/getUpdates.php?variant=normal&lastUpdate=-1',
 'http://nebrasar.ir/api/getUpdates.php?variant=normal&lastUpdate=-1'
];

async function load(){
 let last='';
 for(const url of ENDPOINTS){
  try{
   const r=await fetch(url,{headers:{'user-agent':'Doctor-Tohid-PhD-Course/1.0'}});
   if(!r.ok){last='HTTP '+r.status;continue}
   const j=await r.json();
   if(Array.isArray(j.words)&&j.words.length>=1000) return {url,words:j.words};
   last='short payload';
  }catch(e){last=String(e)}
 }
 throw new Error('Arabic-Persian vocabulary source unavailable: '+last);
}

const {url,words}=await load();
const clean=s=>String(s??'').replace(/\s+/g,' ').trim();
const seen=new Set();
const selected=[];
for(const row of words){
 const ar=clean(row.Ar);
 const fa=clean(row.Fa);
 if(!ar||!fa||seen.has(ar)) continue;
 if(ar.length>55||fa.length>120) continue;
 seen.add(ar);
 selected.push({ar,fa,category:Number(row.CategoryID||0)});
 if(selected.length===1000) break;
}
if(selected.length!==1000) throw new Error('Arabic vocab gate expected 1000 unique entries, got '+selected.length);

const cards=selected.map((x,i)=>({
 id:`ARABIC:WORD:${String(i+1).padStart(4,'0')}`,
 domain:'ARABIC',
 ordinal:i+1,
 title:x.ar,
 prompt:x.ar,
 answer:x.fa,
 explanation:'',
 sourceName:'Arabic-to-Farsi vocabulary dataset used by pwa-persian-dictionary-2022',
 sourceUrl:url,
 verificationStatus:'AR_FA_DATASET'
}));

await fs.mkdir(OUT,{recursive:true});
await fs.writeFile(`${OUT}/arabic_vocab_cards.json`,JSON.stringify(cards,null,2),'utf8');
await fs.writeFile(`${OUT}/arabic_vocab_manifest.json`,JSON.stringify({
 count:1000,
 uniqueArabic:new Set(cards.map(x=>x.title)).size,
 allHavePersianMeaning:cards.every(x=>x.answer.trim()),
 source:url,
 note:'Internal study build; dataset licensing must be reviewed before commercial redistribution.',
 generatedAt:new Date().toISOString()
},null,2),'utf8');
console.log(JSON.stringify({ARABIC_VOCAB_GATE:'PASS',count:cards.length,source:url},null,2));
