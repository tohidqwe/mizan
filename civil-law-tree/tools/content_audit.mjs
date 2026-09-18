import fs from 'node:fs/promises';

const content=JSON.parse(await fs.readFile('civil-law-tree/android/app/src/main/assets/civil_content.json','utf8'));
const manifest=JSON.parse(await fs.readFile('civil-law-tree/generated/content_manifest.json','utf8'));
const main=content.filter(x=>!x.suffix);
const supplements=content.filter(x=>x.suffix);

if(main.length!==1335) throw new Error('Main article count must be exactly 1335');
if(new Set(content.map(x=>x.articleKey)).size!==content.length) throw new Error('Duplicate articleKey');
for(let n=1;n<=1335;n++){
  if(!main.some(x=>x.articleNumber===n)) throw new Error('Missing main article '+n);
}
for(const x of content){
  if(!x.officialText?.trim()) throw new Error('Blank official text '+x.articleKey);
  if(!String(x.sourceUrl).startsWith('https://qavanin.ir/')) throw new Error('Non-official primary source '+x.articleKey);
  if(!(x.courseId>=1&&x.courseId<=8)) throw new Error('Course mapping missing '+x.articleKey);
  if(!x.topic?.trim()||!x.title?.trim()) throw new Error('Topic/title missing '+x.articleKey);
  if(!Array.isArray(x.mindNodes)||x.mindNodes.length<5) throw new Error('Mind tree too small '+x.articleKey);
  if(x.approvalStatus!=='AI_DRAFT' && x.approvalStatus!=='APPROVED') throw new Error('Invalid approval state');
  if(x.scholarViews?.some(v=>!v.citation?.source)) throw new Error('Uncited scholar attribution '+x.articleKey);
}
for(let id=1;id<=8;id++){
  if(!content.some(x=>x.courseId===id)) throw new Error('Empty Civil course '+id);
}
if(!content.some(x=>x.articleKey==='190')) throw new Error('Article 190 sample missing');
if(manifest.mainArticleCount!==1335) throw new Error('Manifest mismatch');

console.log(JSON.stringify({
  CONTENT_AUDIT:'PASS',
  main:main.length,
  supplemental:supplements.length,
  total:content.length,
  courseCounts:manifest.courseCounts,
  professorAttributions:content.reduce((n,x)=>n+(x.scholarViews?.length||0),0),
  publicationState:'AI_DRAFT_NEEDS_LEGAL_REVIEW'
},null,2));
