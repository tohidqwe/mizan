import fs from 'node:fs/promises';

const OUT='android-civil-leitner/app/src/main/assets';
const FREQ_URL='https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/ar/ar_50k.txt';
const TRANSLATE='https://translate.googleapis.com/translate_a/single';

const curated=new Map([
 ['عقد','عقد؛ قرارداد'],['بيع','بیع؛ فروش'],['شراء','خرید'],['ثمن','ثمن؛ بهای معامله'],['مبيع','مبیع؛ مال فروخته‌شده'],
 ['إيجاب','ایجاب'],['قبول','قبول'],['تراضي','تراضی؛ رضایت دو طرف'],['رضا','رضا؛ رضایت'],['قصد','قصد'],
 ['أهلية','اهلیت'],['حجر','حَجر؛ ممنوعیت یا محدودیت تصرف'],['صبي','صغیر؛ کودک'],['بالغ','بالغ'],['رشيد','رشید'],
 ['مجنون','مجنون'],['سفيه','سفیه'],['إكراه','اکراه'],['اضطرار','اضطرار'],['غلط','اشتباه؛ خطا'],
 ['عوض','عوض'],['معاوضة','معاوضه'],['ملك','مِلک؛ مالکیت'],['ملكية','مالکیت'],['مال','مال'],
 ['منفعة','منفعت'],['عين','عین'],['دين','دِین؛ بدهی'],['ذمة','ذمه'],['التزام','تعهد'],
 ['وفاء','وفای به عهد؛ انجام تعهد'],['إبراء','ابراء'],['حوالة','حواله'],['ضمان','ضمان'],['كفالة','کفالت'],
 ['رهن','رهن'],['وديعة','ودیعه'],['عارية','عاریه'],['قرض','قرض'],['إجارة','اجاره'],
 ['جعالة','جعاله'],['صلح','صلح'],['شركة','شرکت'],['مضاربة','مضاربه'],['مزارعة','مزارعه'],
 ['مساقاة','مساقات'],['وكالة','وکالت'],['فضولي','فضولی'],['إجازة','اجازه؛ تنفیذ'],['رد','رد'],
 ['فسخ','فسخ'],['خيار','خیار؛ اختیار فسخ'],['مجلس','مجلس'],['حيوان','حیوان'],['غبن','غبن'],
 ['عيب','عیب'],['تدليس','تدلیس'],['شرط','شرط'],['صفة','صفت'],['فعل','فعل؛ عمل'],
 ['نتيجة','نتیجه'],['باطل','باطل'],['بطلان','بطلان'],['صحيح','صحیح'],['صحة','صحت'],
 ['نافذ','نافذ'],['نفاذ','نفوذ؛ اعتبار اجرایی'],['لازم','لازم'],['جائز','جایز'],['معلق','معلق'],
 ['منجز','منجّز'],['تعليق','تعلیق'],['تصرف','تصرف'],['تسليم','تسلیم'],['قبض','قبض'],
 ['إقباض','اقباض؛ تحویل دادن'],['تلف','تلف'],['إتلاف','اتلاف'],['تسبيب','تسبیب'],['غصب','غصب'],
 ['استيفاء','استیفاء'],['ضرر','ضرر'],['خسارة','خسارت'],['تعويض','جبران خسارت'],['مسؤولية','مسئولیت'],
 ['تقصير','تقصیر'],['سبب','سبب'],['مباشرة','مباشرت'],['متعهد','متعهد'],['دائن','طلبکار؛ دائن'],
 ['مدين','بدهکار؛ مدیون'],['حق','حق'],['حكم','حکم'],['قاعدة','قاعده'],['استثناء','استثنا'],
 ['عام','عام'],['خاص','خاص'],['إطلاق','اطلاق'],['تقييد','تقیید'],['مطلق','مطلق'],
 ['مقيد','مقید'],['شرعي','شرعی'],['مشروع','مشروع'],['محرم','حرام؛ ممنوع شرعی'],['مباح','مباح'],
 ['حرمة','حرمت'],['وجوب','وجوب'],['جواز','جواز'],['لزوم','لزوم'],['مالك','مالک'],
 ['مملوك','مملوک؛ متعلق به مالک'],['متعاقد','طرف عقد'],['متعاقدان','دو طرف عقد'],['عاقد','عقدکننده'],['وكيل','وکیل'],
 ['موكل','موکل'],['ضامن','ضامن'],['مضمون','مضمون'],['مضمون له','مضمون‌له'],['مضمون عنه','مضمون‌عنه'],
 ['محيل','محیل'],['محتال','محتال'],['محال عليه','محال‌علیه'],['كفيل','کفیل'],['مكفول','مکفول'],
 ['راهن','راهن'],['مرتهن','مرتهن'],['مستأجر','مستأجر'],['مؤجر','موجر'],['أجير','اجیر'],
 ['مستعير','مستعیر'],['مستودع','مستودع؛ امین در ودیعه'],['مقترض','مقترض'],['مقرض','قرض‌دهنده'],
 ['جاعل','جاعل'],['عامل','عامل'],['مزارع','مزارع'],['زارع','زارع'],['شريك','شریک'],
 ['سهم','سهم'],['ربح','سود'],['خسارة','زیان؛ خسارت'],['رأس المال','سرمایه'],['أجرة','اجرت'],
 ['وارث','وارث'],['إرث','ارث'],['تركة','ترکه'],['وصية','وصیت'],['موصي','موصی؛ وصیت‌کننده'],
 ['موصى له','موصی‌له'],['موصى به','موصی‌به'],['وصي','وصی'],['ثلث','ثلث؛ یک‌سوم'],['حجب','حجب در ارث'],
 ['مانع','مانع'],['شفعة','شفعه'],['شفيع','شفیع'],['نكاح','نکاح'],['طلاق','طلاق'],
 ['مهر','مهر'],['نفقة','نفقه'],['عدة','عده'],['نسب','نسب'],['حضانة','حضانت'],
 ['ولاية','ولایت'],['وصاية','وصایت'],['قيمومة','قیمومت'],['ولي','ولی'],['قيم','قیم'],
 ['إقرار','اقرار'],['شهادة','شهادت'],['بينة','بینه؛ دلیل شرعی'],['يمين','سوگند'],['قسم','سوگند'],
 ['دليل','دلیل'],['إثبات','اثبات'],['دعوى','دعوا'],['مدعي','مدعی'],['منكر','منکر'],
 ['قضاء','قضا؛ دادرسی'],['قاضي','قاضی'],['حاكم','حاکم'],['خصم','طرف دعوا؛ خصم'],['صلحاً','از راه صلح'],
 ['عرف','عرف'],['عادة','عادت'],['نص','نص؛ متن صریح'],['ظاهر','ظاهر'],['قرينة','قرینه'],
 ['أصل','اصل'],['فرع','فرع'],['موضوع','موضوع'],['أثر','اثر'],['آثار','آثار'],
 ['سببية','سببیت'],['إذن','اذن'],['مأذون','مأذون'],['منع','منع'],['منفعة محللة','منفعت حلال'],
 ['قدرة','توانایی'],['تسليم المبيع','تسلیم مبیع'],['جهالة','جهالت'],['غرر','غرر'],['معلوم','معلوم'],
 ['مجهول','مجهول'],['معين','معین'],['كلي','کلی'],['جزئي','جزئی'],['مثلي','مثلی'],
 ['قيمي','قیمی'],['منقول','منقول'],['عقار','مال غیرمنقول؛ عقار'],['مشاع','مشاع'],['مفروز','مفروز'],
 ['انتفاع','انتفاع'],['ارتفاق','ارتفاق'],['وقف','وقف'],['واقف','واقف'],['موقوف','موقوف'],
 ['حيازة','حیازت'],['إحياء','احیا'],['موات','موات'],['شروط','شرایط'],['أركان','ارکان']
]);

const normalize=s=>String(s??'').replace(/[ـًٌٍَُِّْ]/g,'').replace(/\s+/g,' ').trim();
const faDigits=s=>String(s??'').trim();

async function translateOne(word){
 const url=TRANSLATE+'?client=gtx&sl=ar&tl=fa&dt=t&q='+encodeURIComponent(word);
 const r=await fetch(url,{headers:{'user-agent':'Mozilla/5.0 Doctor-Tohid-PhD-Course'}});
 if(!r.ok) throw new Error('translation HTTP '+r.status+' for '+word);
 const j=await r.json();
 const text=(j?.[0]||[]).map(x=>x?.[0]||'').join('').trim();
 if(!text) throw new Error('blank translation for '+word);
 return text;
}

const response=await fetch(FREQ_URL,{headers:{'user-agent':'Doctor-Tohid-PhD-Course/1.0'}});
if(!response.ok) throw new Error('Arabic frequency source failed '+response.status);
const rows=(await response.text()).split(/\r?\n/);
const candidates=[];
const seen=new Set();
for(const line of rows){
 const word=normalize(line.replace(/\s+\d+\s*$/,''));
 if(!word || seen.has(word)) continue;
 if(word.length<2 || word.length>22) continue;
 if(!/^[\u0621-\u064A\u0671\u067E\u0686\u0698\u06A9\u06AF\u06CC\s]+$/u.test(word)) continue;
 if(/^(الله|اللعنة|الجحيم)$/u.test(word)) continue;
 seen.add(word);
 candidates.push(word);
 if(candidates.length>=1600) break;
}

const cards=[];
const used=new Set();
const push=(ar,fa,source)=>{
 ar=normalize(ar); fa=faDigits(fa);
 if(!ar||!fa||used.has(ar)||cards.length>=1000) return;
 used.add(ar);
 cards.push({
  id:`ARABIC:WORD:${String(cards.length+1).padStart(4,'0')}`,
  domain:'ARABIC',ordinal:cards.length+1,title:ar,prompt:ar,answer:fa,explanation:'',
  sourceName:source,
  sourceUrl: source.startsWith('Curated') ? 'internal://fiqh-legal-vocabulary' : FREQ_URL,
  verificationStatus:source.startsWith('Curated')?'CURATED_FIQH_LEGAL':'FREQUENCY_TRANSLATED'
 });
};

for(const [ar,fa] of curated) push(ar,fa,'Curated fiqh/private-law Arabic vocabulary');

let failures=0;
for(const word of candidates){
 if(cards.length>=1000) break;
 if(used.has(word)) continue;
 try{
  const translated=await translateOne(word);
  push(word,translated,'FrequencyWords Arabic + machine Persian translation');
  await new Promise(r=>setTimeout(r,18));
 }catch(e){
  failures++;
  if(failures>80) throw new Error('Too many Arabic translation failures: '+String(e));
 }
}

if(cards.length!==1000) throw new Error('Arabic vocab gate expected 1000 unique entries, got '+cards.length);
if(cards.some(x=>!x.title.trim()||!x.answer.trim())) throw new Error('Arabic vocab blank field');

await fs.mkdir(OUT,{recursive:true});
await fs.writeFile(`${OUT}/arabic_vocab_cards.json`,JSON.stringify(cards,null,2),'utf8');
await fs.writeFile(`${OUT}/arabic_vocab_manifest.json`,JSON.stringify({
 count:1000,
 curatedFiqhLegal:cards.filter(x=>x.verificationStatus==='CURATED_FIQH_LEGAL').length,
 frequencyTranslated:cards.filter(x=>x.verificationStatus==='FREQUENCY_TRANSLATED').length,
 uniqueArabic:new Set(cards.map(x=>x.title)).size,
 allHavePersianMeaning:cards.every(x=>x.answer.trim()),
 frequencySource:FREQ_URL,
 translationSource:'Google Translate public web endpoint for internal study build',
 generatedAt:new Date().toISOString()
},null,2),'utf8');

console.log(JSON.stringify({
 ARABIC_VOCAB_GATE:'PASS',
 count:cards.length,
 curated:cards.filter(x=>x.verificationStatus==='CURATED_FIQH_LEGAL').length,
 translated:cards.filter(x=>x.verificationStatus==='FREQUENCY_TRANSLATED').length
},null,2));
