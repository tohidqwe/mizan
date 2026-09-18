import fs from 'node:fs';
const text = fs.readFileSync('src/data/commerce.ts','utf8');
const nums = [...text.matchAll(/\bn:\s*(\d+)\s*,/g)].map(m=>Number(m[1]));
const unique = [...new Set(nums)].sort((a,b)=>a-b);
const missing = [];
for (let i=1;i<=600;i++) if(!unique.includes(i)) missing.push(i);
console.log(JSON.stringify({entries:nums.length,unique:unique.length,min:unique[0],max:unique.at(-1),missingCount:missing.length,missing:missing.slice(0,100)},null,2));
