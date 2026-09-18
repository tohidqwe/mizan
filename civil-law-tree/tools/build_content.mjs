import fs from 'node:fs/promises';
import crypto from 'node:crypto';

const RAW='civil-law-tree/generated/qavanin_civil_raw.json';
const ASSETS='civil-law-tree/android/app/src/main/assets';
const OUT='civil-law-tree/generated';
const raw=JSON.parse(await fs.readFile(RAW,'utf8'));
await fs.mkdir(ASSETS,{recursive:true});
await fs.mkdir(OUT,{recursive:true});

const courseNames={
  1:'مدنی ۱ — اشخاص و محجورین',
  2:'مدنی ۲ — اموال و مالکیت',
  3:'مدنی ۳ — قواعد عمومی قراردادها و تعهدات',
  4:'مدنی ۴ — مسئولیت مدنی و ضمان قهری',
  5:'مدنی ۵ — خانواده',
  6:'مدنی ۶ — عقود معین ۱',
  7:'مدنی ۷ — عقود معین ۲',
  8:'مدنی ۸ — شفعه، وصیت و ارث',
};

function courseFor(n){
  if(n<=10) return 1;
  if(n<=182) return 2;
  if(n<=300) return 3;
  if(n<=337) return 4;
  if(n<=517) return 6;
  if(n<=560) return 7;
  if(n<=570) return 6;
  if(n<=647) return 7;
  if(n<=653) return 6;
  if(n<=751) return 7;
  if(n<=770) return 6;
  if(n<=807) return 7;
  if(n<=955) return 8;
  if(n<=1033) return 1;
  if(n<=1206) return 5;
  if(n<=1256) return 1;
  return 3;
}

const normalize=s=>String(s||'')
  .replace(/ك/g,'ک').replace(/[يى]/g,'ی').replace(/ة/g,'ه')
  .replace(/\s+/g,' ').trim();

const topics=[
 ['انتشار و اجرای قانون',/روزنامه رسمی|انتشار|لازم الاجرا|ابلاغ/u],
 ['تابعیت و تعارض قوانین',/اتباع|تابعیت|خارجه|دولت متبوع/u],
 ['اموال غیرمنقول',/غیرمنقول|اراضی|ابنیه/u],
 ['اموال منقول',/منقول/u],
 ['مالکیت',/مالکیت|مالک|تصرف/u],
 ['حق انتفاع و وقف',/حق انتفاع|وقف|موقوف|عمری|رقبی/u],
 ['حق ارتفاق',/ارتفاق|حق عبور|مجرای آب/u],
 ['عقد و شرایط صحت معامله',/عقد|معامله|قصد طرفین|رضا|اکراه|اشتباه|اهلیت/u],
 ['شروط ضمن عقد',/شرط صفت|شرط فعل|شرط نتیجه|شروط ضمن عقد/u],
 ['آثار قرارداد',/متعهد|تعهد|وجه التزام|خسارت|الزام/u],
 ['سقوط تعهدات',/وفای به عهد|اقاله|ابراء|تهاتر|تبدیل تعهد|مالکیت ما فی الذمه/u],
 ['ایفای ناروا و اداره مال غیر',/دریافت.*استحقاق|ایفاء|اداره.*مال.*غیر/u],
 ['غصب',/غصب|غاصب|مغصوب/u],
 ['اتلاف و تسبیب',/اتلاف|تسبیب|سبب.*تلف/u],
 ['استیفا',/استیفا|عمل.*اجرت|مال دیگری.*اذن/u],
 ['بیع',/بیع|بایع|مبیع|ثمن|مشتری/u],
 ['خیارات',/خیار|غبن|عیب|تدلیس|تبعض صفقه|تأخیر ثمن/u],
 ['معاوضه',/معاوضه/u],
 ['اجاره',/اجاره|موجر|مستأجر|عین مستأجره|اجیر/u],
 ['مزارعه',/مزارعه|زارع/u],
 ['مساقات',/مساقات/u],
 ['مضاربه',/مضاربه/u],
 ['جعاله',/جعاله|جاعل/u],
 ['شرکت',/شرکت|شریک|اشاعه|مال مشترک/u],
 ['ودیعه',/ودیعه|مستودع/u],
 ['عاریه',/عاریه|مستعیر/u],
 ['قرض',/قرض|مقترض/u],
 ['وکالت',/وکالت|وکیل|موکل/u],
 ['ضمان',/ضمان|ضامن|مضمون عنه|مضمون له/u],
 ['حواله',/حواله|محیل|محتال|محال علیه/u],
 ['کفالت',/کفالت|کفیل|مکفول/u],
 ['صلح',/صلح|مصالح|متصالح/u],
 ['رهن',/رهن|راهن|مرتهن|مال مرهون/u],
 ['هبه',/هبه|واهب|متهب/u],
 ['شفعه',/شفعه|شفیع/u],
 ['وصیت',/وصیت|موصی|وصی|موصی له|موصی به/u],
 ['ارث',/ارث|وارث|مورث|ترکه|حجب|فرض/u],
 ['شخصیت و اهلیت',/اهلیت|شخصیت|حقوق مدنی/u],
 ['اقامتگاه',/اقامتگاه/u],
 ['غایب مفقودالاثر',/غایب|مفقودالاثر/u],
 ['نکاح',/نکاح|ازدواج|زوج|زوجه/u],
 ['مهر و نفقه',/مهر|مهریه|نفقه/u],
 ['انحلال نکاح و طلاق',/طلاق|فسخ نکاح|بذل مدت|عده|خلع|مبارات/u],
 ['نسب و حضانت',/نسب|ولد|طفل|حضانت/u],
 ['حجر و قیمومت',/محجور|صغیر|مجنون|سفیه|قیم|قیمومت|ولی قهری/u],
 ['اقرار',/اقرار|مقر/u],
 ['ادله اثبات و شهادت',/شهادت|شاهد|بینه|دلیل/u],
 ['قسم',/قسم|سوگند|حلف/u],
];

function topicFor(a){
  const text=normalize([a.section,a.chapter,a.part,a.book,a.officialText].join(' '));
  for(const [name,re] of topics) if(re.test(text)) return name;
  if(a.section) return normalize(a.section).slice(0,90);
  if(a.chapter) return normalize(a.chapter).slice(0,90);
  if(a.part) return normalize(a.part).slice(0,90);
  return 'قاعده عمومی حقوق مدنی';
}

function subtopicFor(a,topic){
  const structural=normalize(a.section||a.chapter||a.part||'');
  if(structural) return structural.replace(/^(مبحث|فصل|باب)\s+[^-–—]*[-–—]?\s*/u,'').trim().slice(0,100);
  if(a.articleNumber>=1257) return 'ادله اثبات دعوا — پیوند مشترک با دعاوی مدنی';
  return topic;
}

function ruleType(text){
  const t=normalize(text);
  if(/باطل|بطلان|غیر نافذ|نافذ/u.test(t)) return 'اعتبار و اثر حقوقی';
  if(/نمی تواند|نمی‌تواند|ممنوع|نباید/u.test(t)) return 'محدودیت یا ممنوعیت';
  if(/مکلف|موظف|باید|ملزم/u.test(t)) return 'تکلیف قانونی';
  if(/ضامن|مسئول/u.test(t)) return 'مسئولیت و ضمان';
  if(/حق دارد|می تواند|می‌تواند/u.test(t)) return 'حق یا اختیار';
  if(/محسوب|در حکم/u.test(t)) return 'وضعیت یا فرض قانونی';
  return 'قاعده حقوقی';
}

function sentences(text){
  return normalize(text).split(/(?<=[.؛؟])\s+/u).filter(Boolean);
}
function coreSentence(text){
  const ss=sentences(text);
  const scored=ss.map(s=>[s,(s.match(/باید|حق|ضامن|باطل|ممنوع|شرط|اگر|هرگاه|مگر|می تواند|نمی تواند/gu)||[]).length*10+Math.min(s.length,220)]);
  scored.sort((a,b)=>b[1]-a[1]);
  return (scored[0]?.[0]||normalize(text)).slice(0,420);
}
function matchList(text,re,max=4){
  const out=[];
  for(const m of normalize(text).matchAll(re)){
    const value=normalize(m[0]);
    if(value && !out.includes(value)) out.push(value);
    if(out.length>=max) break;
  }
  return out;
}
function conditions(text){
  return matchList(text,/(?:اگر|هرگاه|در صورتی که|مشروط بر اینکه|به شرط آنکه)[^؛.]{5,170}/gu,4);
}
function exceptions(text){
  return matchList(text,/(?:مگر|به استثنای|جز در صورتی که)[^؛.]{4,160}/gu,3);
}
function effects(text){
  const t=normalize(text);
  const out=[];
  if(/باطل|بطلان/u.test(t)) out.push('اثر مرتبط با بطلان یا بی‌اعتباری در خود ماده تصریح شده است.');
  if(/غیر نافذ/u.test(t)) out.push('اثر ماده با عدم نفوذ و نیاز به تنفیذ/رد مرتبط است.');
  if(/ضامن|مسئول/u.test(t)) out.push('ماده مسئولیت یا ضمان حقوقی ایجاد یا تعیین می‌کند.');
  if(/حق دارد|می تواند|می‌تواند/u.test(t)) out.push('ماده برای شخص معین حق یا اختیار ایجاد می‌کند.');
  if(/مکلف|موظف|باید/u.test(t)) out.push('ماده تکلیف قانونی ایجاد می‌کند.');
  return out.length?out:['اثر حقوقی باید از نتیجه صریح همین ماده استخراج شود.'];
}
function keyTerms(text){
  const stop=new Set('این آن است بود باشد می شود شده در از به با برای که و یا را بر اگر هرگاه مگر مورد موارد ماده قانون شخص اشخاص'.split(' '));
  const words=normalize(text).replace(/[.,،؛:()\[\]«»"؟]/g,' ').split(/\s+/).filter(w=>w.length>=4&&!stop.has(w));
  const f=new Map();
  words.forEach(w=>f.set(w,(f.get(w)||0)+1));
  return [...f.entries()].sort((a,b)=>b[1]-a[1]||b[0].length-a[0].length).slice(0,5).map(x=>x[0]);
}
function titleFor(a,topic){
  const t=normalize(a.officialText);
  const r=ruleType(t);
  if(a.articleNumber===190) return 'شرایط اساسی صحت معامله';
  if(topic==='بیع' && /تعریف|عبارت/u.test(t)) return 'تعریف و ارکان بیع';
  return `${topic} — ${r}`;
}

function buildNodes(a,topic,subtopic){
  const type=ruleType(a.officialText);
  const cs=conditions(a.officialText);
  const ex=exceptions(a.officialText);
  const eff=effects(a.officialText);
  const terms=keyTerms(a.officialText);
  const nodes=[
    {id:'root',label:`ماده ${a.articleKey}`,type:'ROOT',parentId:null},
    {id:'topic',label:topic,type:'TOPIC',parentId:'root'},
    {id:'rule',label:type,type:'RULE',parentId:'root'},
  ];
  if(subtopic && subtopic!==topic) nodes.push({id:'subtopic',label:subtopic,type:'SUBTOPIC',parentId:'topic'});
  if(cs.length) nodes.push({id:'condition',label:'شرط: '+cs[0].slice(0,70),type:'CONDITION',parentId:'rule'});
  if(ex.length) nodes.push({id:'exception',label:'استثنا: '+ex[0].slice(0,70),type:'EXCEPTION',parentId:'rule'});
  nodes.push({id:'effect',label:'اثر: '+eff[0].slice(0,75),type:'EFFECT',parentId:'rule'});
  if(terms.length) nodes.push({id:'memory',label:'کلید: '+terms.slice(0,3).join(' / '),type:'MEMORY',parentId:'root'});
  return nodes;
}

function explain(a,topic,subtopic){
  const type=ruleType(a.officialText);
  const core=coreSentence(a.officialText);
  const cs=conditions(a.officialText);
  const ex=exceptions(a.officialText);
  const terms=keyTerms(a.officialText);
  let simple=`این ماده در مبحث «${topic}» یک «${type}» را بیان می‌کند. هسته حکم به زبان آموزشی: ${core}`;
  if(cs.length) simple+=` شرط برجسته در متن: ${cs[0]}.`;
  if(ex.length) simple+=` استثنای صریح: ${ex[0]}.`;
  const academic=`برداشت آموزشی اولیه از متن رسمی ماده ${a.articleKey}: برای تحلیل، موضوع حکم، مخاطب، شرط تحقق و اثر حقوقی را از هم جدا کن. این تحلیل AI Draft است و تا Legal Review نباید به عنوان نظر قطعی استاد یا دکترین منتشر شود.`;
  const philosophy=`منطق آموزشی این ماده آن است که دانشجو جایگاه «${topic}» را در ساختار مدنی ببیند و حکم را به جای حفظ خطی، به رابطه «موضوع → شرط → اثر» تبدیل کند. این بخش تفسیر آموزشی اولیه است.`;
  const memoryCue=`سه قفل حافظه: ${terms.slice(0,3).join(' ← ') || topic}. ابتدا موضوع را به یاد بیاور، سپس شرط و در پایان اثر حقوقی را بازیابی کن.`;
  const trap=ex.length
    ? 'دام مفهومی: پاسخ‌های مطلق ممکن است استثنای صریح ماده را نادیده بگیرند.'
    : cs.length
      ? 'دام مفهومی: حذف شرط ماده می‌تواند نتیجه حقوقی را تغییر دهد؛ شرط را از حکم جدا نکن.'
      : 'دام مفهومی: میان متن رسمی ماده و برداشت تفسیری یا مثال آموزشی خلط نکن.';
  return {
    simpleExplanation:simple,
    academicAnalysis:academic,
    philosophy,
    elements:[`موضوع: ${topic}`,`نوع قاعده: ${type}`,...(terms.slice(0,3).map(x=>'کلیدواژه: '+x))],
    conditions:cs,
    effects:effects(a.officialText),
    exceptions:ex,
    memoryCue,
    examTrap:trap,
    activeRecall:[
      `بدون نگاه کردن، ماده ${a.articleKey} را در یک جمله مفهومی توضیح بده.`,
      `شرط و اثر اصلی ماده ${a.articleKey} چیست؟`,
      `این ماده در کدام شاخه از ${courseNames[courseFor(a.articleNumber)]} قرار می‌گیرد؟`
    ]
  };
}

const sourceUrl=raw.source.url;
const sourceLabel='سامانه ملی قوانین و مقررات جمهوری اسلامی ایران — Qavanin.ir';
const input=[...raw.main,...raw.supplemental];
const content=input.map(a=>{
  a.officialText=normalize(a.officialText);
  const courseId=courseFor(a.articleNumber);
  const topic=topicFor(a);
  const subtopic=subtopicFor(a,topic);
  const generated=explain(a,topic,subtopic);
  return {
    articleKey:a.articleKey,
    articleNumber:a.articleNumber,
    suffix:a.suffix||'',
    legalStatus:a.legalStatus,
    officialText:a.officialText,
    sourceUrl,
    sourceLabel,
    sourceProvenance:'OFFICIAL_QAVANIN',
    courseId,
    courseName:courseNames[courseId],
    topic,
    subtopic,
    title:titleFor(a,topic),
    approvalStatus:'AI_DRAFT',
    classificationStatus:'AI_DRAFT_NEEDS_LEGAL_REVIEW',
    version:1,
    createdAt:new Date().toISOString(),
    updatedAt:new Date().toISOString(),
    ...generated,
    mindNodes:buildNodes(a,topic,subtopic),
    relatedArticleKeys:[],
    scholarViews:[],
    caseLaw:[],
    citations:[{kind:'LAW_TEXT',label:sourceLabel,url:sourceUrl}],
  };
});

const byCourseTopic=new Map();
for(const x of content){
  const k=`${x.courseId}|${x.topic}`;
  if(!byCourseTopic.has(k)) byCourseTopic.set(k,[]);
  byCourseTopic.get(k).push(x);
}
const index=new Map(content.map((x,i)=>[x.articleKey,i]));
for(const x of content){
  const rel=[];
  const n=x.articleNumber;
  for(const key of [String(n-1),String(n+1)]){
    if(index.has(key)) rel.push(key);
  }
  const peers=byCourseTopic.get(`${x.courseId}|${x.topic}`)||[];
  const peer=peers.find(p=>p.articleKey!==x.articleKey && !rel.includes(p.articleKey));
  if(peer) rel.push(peer.articleKey);
  x.relatedArticleKeys=[...new Set(rel)].slice(0,4);
  x.mindNodes.push({id:'related',label:'ارتباط: '+(x.relatedArticleKeys.join('، ')||'در حال بررسی'),type:'RELATED',parentId:'root'});
}

const json=JSON.stringify(content,null,2)+'\n';
await fs.writeFile(`${ASSETS}/civil_content.json`,json,'utf8');

const manifest={
  product:'درخت قانون مدنی',
  mainArticleCount:content.filter(x=>!x.suffix).length,
  supplementalCount:content.filter(x=>x.suffix).length,
  totalRecords:content.length,
  courseCounts:Object.fromEntries(Object.entries(courseNames).map(([id])=>[id,content.filter(x=>x.courseId===Number(id)).length])),
  officialSource:sourceUrl,
  academicAttributionPolicy:'No professor attribution without a traceable citation and Legal Review.',
  releasePolicy:'Production surfaces only APPROVED derived content. Official statutory text remains independently sourced.',
  contentSha256:crypto.createHash('sha256').update(json).digest('hex'),
  generatedAt:new Date().toISOString()
};
await fs.writeFile(`${OUT}/content_manifest.json`,JSON.stringify(manifest,null,2)+'\n','utf8');
console.log(JSON.stringify({CONTENT_BUILD:'PASS',...manifest},null,2));
