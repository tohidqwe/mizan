import fs from 'node:fs/promises';

const sources = [
  ['TRADE_1311','https://www.solh.ir/regulation/1/75',600],
  ['TRADE_1311_ADLIO','https://adlio.ir/law/%D9%82%D8%A7%D9%86%D9%88%D9%86-%D8%AA%D8%AC%D8%A7%D8%B1%D8%AA-%D9%85%D8%B5%D9%88%D8%A8-1311-02-13/',600],
  ['AMEND_1347','https://lamtakam.com/law/parliament/96314/%D9%84%D8%A7%DB%8C%D8%AD%D9%87%2B%D9%82%D8%A7%D9%86%D9%88%D9%86%DB%8C%2B%D8%A7%D8%B5%D9%84%D8%A7%D8%AD%2B%D9%82%D8%B3%D9%85%D8%AA%DB%8C%2B%D8%A7%D8%B2%2B%D9%82%D8%A7%D9%86%D9%88%D9%86%2B%D8%AA%D8%AC%D8%A7%D8%B1%D8%AA',300],
];

function decode(s){return s.replace(/&nbsp;/g,' ').replace(/&zwnj;/g,'‌').replace(/&zwj;/g,'‍').replace(/&laquo;/g,'«').replace(/&raquo;/g,'»').replace(/&amp;/g,'&').replace(/&#(\d+);/g,(_,n)=>String.fromCodePoint(Number(n))).replace(/&#x([0-9a-f]+);/gi,(_,n)=>String.fromCodePoint(parseInt(n,16)));}
function visible(html){return decode(html.replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi,' ').replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi,' ').replace(/<(br|\/p|\/div|\/li|\/h\d)>/gi,'\n').replace(/<[^>]+>/g,' ')).replace(/\r/g,'').replace(/[ \t]+/g,' ').replace(/\n{3,}/g,'\n\n');}
const pd = '۰۱۲۳۴۵۶۷۸۹';
function latin(s){return s.replace(/[۰-۹]/g,c=>String(pd.indexOf(c)));}
function articleNumbers(text){
  const nums=[];
  const re=/ماده\s*[‌\u200c\-–—ـ:]*\s*([۰-۹0-9]{1,3})(?=\D)/g;
  for(const m of text.matchAll(re)) nums.push(Number(latin(m[1])));
  return [...new Set(nums.filter(n=>n>0 && n<=600))].sort((a,b)=>a-b);
}
for(const [name,url,expected] of sources){
  try{
    const r=await fetch(url,{headers:{'user-agent':'Mozilla/5.0'},redirect:'follow'});
    const html=await r.text();
    const text=visible(html);
    const nums=articleNumbers(text).filter(n=>n<=expected);
    const missing=[]; for(let n=1;n<=expected;n++) if(!nums.includes(n)) missing.push(n);
    console.log(JSON.stringify({name,status:r.status,html:html.length,text:text.length,unique:nums.length,min:nums[0],max:nums.at(-1),missingCount:missing.length,missing:missing.slice(0,30)}));
    await fs.mkdir('trade-probe',{recursive:true});
    await fs.writeFile(`trade-probe/${name}.txt`,text);
  }catch(e){console.log(JSON.stringify({name,error:String(e)}));}
}
