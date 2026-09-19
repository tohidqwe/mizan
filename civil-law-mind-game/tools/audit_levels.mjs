import fs from 'node:fs/promises';

const levels=JSON.parse(await fs.readFile('civil-law-mind-game/android/app/src/main/assets/madani3_levels.json','utf8'));
if(levels.length!==50) throw new Error('Expected 50 levels');
if(new Set(levels.map(x=>x.articleNumber)).size!==50) throw new Error('Duplicate article level');
for(let n=183;n<=232;n++){
  const x=levels.find(v=>v.articleNumber===n);
  if(!x) throw new Error('Missing level '+n);
  if(!x.officialText || x.officialText.length<12) throw new Error('Missing official text '+n);
  if(x.source!=='Qavanin.ir' || !x.sourceUrl.startsWith('https://qavanin.ir/')) throw new Error('Bad source '+n);
  if(!x.memoryLock || !x.concept || !x.instruction) throw new Error('Weak gameplay content '+n);
}
const mechanics=new Set(levels.map(x=>x.mechanic));
for(const m of ['DROP','LAB','CHAIN','LINK']) if(!mechanics.has(m)) throw new Error('Missing mechanic '+m);
const worlds=new Set(levels.map(x=>x.worldIndex));
if(worlds.size!==8) throw new Error('Expected 8 worlds');

console.log(JSON.stringify({
  GAME_AUDIT:'PASS',
  levels:levels.length,
  first:levels[0].articleNumber,
  last:levels.at(-1).articleNumber,
  worlds:worlds.size,
  mechanics:[...mechanics]
},null,2));
