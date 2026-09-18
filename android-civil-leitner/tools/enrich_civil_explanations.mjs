import fs from 'node:fs/promises';
import crypto from 'node:crypto';
import { buildConceptExplanation } from './legal_conceptualizer.mjs';

const assets='android-civil-leitner/app/src/main/assets';
const seedPath=`${assets}/civil_seed.json`;
const manifestPath=`${assets}/civil_source_manifest.json`;

const seed=JSON.parse(await fs.readFile(seedPath,'utf8'));
const active=seed.filter(x=>x.topic!=='ماده منسوخ');

for(const item of seed){
  if(item.topic==='ماده منسوخ'){
    item.simpleExplanation='';
    item.analyticalPoint='';
    continue;
  }
  const c=buildConceptExplanation({
    number:item.articleNumber,
    text:item.officialText,
    law:'CIVIL',
    meta:{
      book:item.book||'',
      part:item.part||'',
      chapter:item.chapter||'',
      section:item.section||'',
    }
  });
  item.simpleExplanation=c.simpleExplanation;
  item.analyticalPoint=c.analyticalPoint;
  item.recallQuestion=c.recallQuestion;
  if(!Array.isArray(item.keywords) || item.keywords.length===0){
    item.keywords=[c.domain,c.ruleType];
  }
}

const explanations=active.map(x=>x.simpleExplanation.trim());
const analytical=active.map(x=>x.analyticalPoint.trim());
if(explanations.some(x=>x.length<90)) throw new Error('Civil conceptual explanation too short');
if(analytical.some(x=>x.length<80)) throw new Error('Civil analytical point too short');
if(new Set(explanations).size!==explanations.length) throw new Error('Duplicate civil conceptual explanation detected');
if(new Set(analytical).size!==analytical.length) throw new Error('Duplicate civil analytical point detected');

const raw=JSON.stringify(seed,null,2)+'\n';
await fs.writeFile(seedPath,raw,'utf8');

const manifest=JSON.parse(await fs.readFile(manifestPath,'utf8'));
manifest.activeConceptExplanationCount=active.length;
manifest.activeAnalyticalPointCount=active.length;
manifest.conceptExplanationPolicy='Each active civil article receives a text-derived, article-specific conceptual explanation and analytical point; repealed articles remain excluded from the active bank.';
manifest.seedSha256=crypto.createHash('sha256').update(raw).digest('hex');
await fs.writeFile(manifestPath,JSON.stringify(manifest,null,2)+'\n','utf8');

console.log(JSON.stringify({
  CIVIL_CONCEPT_GATE:'PASS',
  activeArticles:active.length,
  uniqueExplanations:new Set(explanations).size,
  uniqueAnalyticalPoints:new Set(analytical).size,
},null,2));
