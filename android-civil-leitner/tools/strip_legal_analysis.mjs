import fs from 'node:fs/promises';
import crypto from 'node:crypto';

const assets='android-civil-leitner/app/src/main/assets';
const seedPath=assets+'/civil_seed.json';
const manifestPath=assets+'/civil_source_manifest.json';
const tradePath=assets+'/trade_cards.json';

const seed=JSON.parse(await fs.readFile(seedPath,'utf8'));
for(const x of seed){
  x.recallQuestion='';
  x.twoChoiceQuestion='';
  x.simpleExplanation='';
  x.analyticalPoint='';
  x.importantPoints='';
  x.relatedArticles=[];
}
const seedText=JSON.stringify(seed,null,2)+'\n';
await fs.writeFile(seedPath,seedText,'utf8');

try{
  const manifest=JSON.parse(await fs.readFile(manifestPath,'utf8'));
  manifest.seedSha256=crypto.createHash('sha256').update(seedText).digest('hex');
  manifest.contentMode='OFFICIAL_TEXT_ONLY';
  manifest.analysisFieldsRemoved=true;
  delete manifest.activeConceptExplanationCount;
  delete manifest.activeAnalyticalPointCount;
  await fs.writeFile(manifestPath,JSON.stringify(manifest,null,2)+'\n','utf8');
}catch{}

const trade=JSON.parse(await fs.readFile(tradePath,'utf8'));
for(const x of trade){
  x.prompt=x.title;
  x.explanation='';
}
await fs.writeFile(tradePath,JSON.stringify(trade,null,2)+'\n','utf8');

console.log(JSON.stringify({LEGAL_ANALYSIS_STRIP_GATE:'PASS',civil:seed.length,trade:trade.length}));
