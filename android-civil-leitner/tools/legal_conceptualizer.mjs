const faDigits = '۰۱۲۳۴۵۶۷۸۹';
const arDigits = '٠١٢٣٤٥٦٧٨٩';

export function normalizeLegalPersian(input = '') {
  return String(input)
    .replace(/[٠-٩]/g, c => faDigits[arDigits.indexOf(c)])
    .replace(/ك/g, 'ک')
    .replace(/[يى]/g, 'ی')
    .replace(/ۀ/g, 'هٔ')
    .replace(/ة/g, 'ه')
    .replace(/[\u200e\u200f\u202a-\u202e\u2066-\u2069]/g, '')
    .replace(/\s+/g, ' ')
    .replace(/\s+([،؛:.])/g, '$1')
    .trim();
}

const modernReplacements = [
  [/\bمی\s*باشد\b/g, 'است'],
  [/\bمیباشد\b/g, 'است'],
  [/\bمی\s*گردد\b/g, 'می‌شود'],
  [/\bمیگردد\b/g, 'می‌شود'],
  [/\bخواهد\s+بود\b/g, 'است'],
  [/\bمکلف\s+است\b/g, 'باید'],
  [/\bموظف\s+است\b/g, 'باید'],
  [/\bملزم\s+است\b/g, 'باید'],
  [/\bحق\s+خواهد\s+داشت\b/g, 'حق دارد'],
  [/\bمی\s*تواند\b/g, 'می‌تواند'],
  [/\bنمی\s*تواند\b/g, 'نمی‌تواند'],
  [/\bدر\s+حکم\b/g, 'از نظر قانون مانند'],
  [/\bمحسوب\s+می‌شود\b/g, 'به حساب می‌آید'],
  [/\bمحسوب\s+خواهد\s+شد\b/g, 'به حساب می‌آید'],
  [/\bنافذ\s+است\b/g, 'اعتبار حقوقی دارد'],
  [/\bباطل\s+است\b/g, 'اعتبار حقوقی ندارد'],
  [/\bغیر\s*نافذ\s+است\b/g, 'تا تنفیذ نشود اثر کامل حقوقی ندارد'],
  [/\bضامن\s+است\b/g, 'مسئول جبران است'],
  [/\bمنوط\s+است\b/g, 'وابسته است'],
  [/\bمشروط\s+است\b/g, 'به این شرط است'],
  [/\bمذکور\b/g, 'یادشده'],
  [/\bمزبور\b/g, 'یادشده'],
  [/\bنامبرده\b/g, 'آن شخص'],
  [/\bموصوف\b/g, 'یادشده'],
  [/\bحین\b/g, 'هنگام'],
  [/\bبموجب\b/g, 'به موجب'],
  [/\bبترتیب\b/g, 'به ترتیب'],
  [/\bبلافاصله\b/g, 'بی‌درنگ'],
  [/\bبعمل\s+آید\b/g, 'انجام شود'],
  [/\bبعمل\s+خواهد\s+آمد\b/g, 'انجام می‌شود'],
  [/\bنسبت\s+به\s+ماقبل\b/g, 'نسبت به گذشته'],
];

function modernize(text) {
  let out = normalizeLegalPersian(text);
  for (const [re, replacement] of modernReplacements) out = out.replace(re, replacement);
  return out.replace(/\s+/g, ' ').trim();
}

function clip(text, max = 310) {
  const s = String(text || '').trim();
  if (s.length <= max) return s;
  const cut = s.slice(0, max);
  const i = Math.max(cut.lastIndexOf('،'), cut.lastIndexOf('؛'), cut.lastIndexOf(' '));
  return (i > max * 0.6 ? cut.slice(0, i) : cut).trim() + '…';
}

const civilDomains = [
  ['انتشار و اجرای قوانین', /روزنامه رسمی|ابلاغ|انتشار قوانین|لازم[‌ ]?الاجرا|اثر قانون/],
  ['تابعیت و تعارض قوانین', /اتباع|تابعیت|خارجه|دولت متبوع|احوال شخصیه.*اتباع/],
  ['اموال و مالکیت', /مال|اموال|مالک|مالکیت|منقول|غیرمنقول|تصرف|حیازت|مباحات/],
  ['وقف و حق انتفاع', /وقف|موقوف|حق انتفاع|عمری|رقبی|حبس مطلق/],
  ['عقود و معاملات', /عقد|معامله|متعهد|تعهد|ایجاب|قبول|قصد طرفین|رضا|اکراه|اشتباه/],
  ['شرط ضمن عقد', /شرط|مشروط|شرط فعل|شرط صفت|شرط نتیجه/],
  ['بیع', /بیع|بایع|مبیع|ثمن|خیار|مشتری/],
  ['اجاره', /اجاره|موجر|مستأجر|عین مستأجره|اجیر/],
  ['مزارعه و مساقات', /مزارعه|مساقات|عامل|زارع/],
  ['مضاربه', /مضاربه|عامل.*سرمایه|مالک.*سرمایه/],
  ['جعاله', /جعاله|جاعل|عامل.*جعل/],
  ['شرکت', /شرکت|شریک|اشاعه|مال مشترک/],
  ['ودیعه و عاریه', /ودیعه|مستودع|ودیعه‌گذار|عاریه|مستعیر/],
  ['قرض', /قرض|مقترض|مقرض/],
  ['وکالت', /وکالت|وکیل|موکل/],
  ['ضمان', /ضمان|ضامن|مضمون[‌ ]?له|مضمون[‌ ]?عنه/],
  ['حواله', /حواله|محیل|محال[‌ ]?علیه|محتال/],
  ['کفالت', /کفالت|کفیل|مکفول/],
  ['رهن', /رهن|راهن|مرتهن|مال مرهون/],
  ['صلح', /صلح|مصالح|متصالح/],
  ['هبه', /هبه|واهب|متهب|عین موهوبه/],
  ['ضمان قهری و مسئولیت', /اتلاف|تسبیب|غصب|ضامن|مسئول جبران|استیفا/],
  ['نکاح و خانواده', /نکاح|زوج|زوجه|شوهر|زن|مهر|مهریه|نفقه|تمکین/],
  ['طلاق و انحلال نکاح', /طلاق|رجوع|عده|فسخ نکاح|بذل مدت/],
  ['اولاد و نسب', /ولد|اولاد|نسب|طفل|حضانت|ابوت|امومت/],
  ['ولایت، حجر و قیمومت', /محجور|حجر|صغیر|مجنون|سفیه|ولی قهری|قیم|قیمومت/],
  ['وصیت', /وصیت|موصی|موصی[‌ ]?له|موصی[‌ ]?به|وصی/],
  ['ارث', /ارث|وارث|ترکه|مورث|طبقه.*ارث|فرض|عصبه/],
  ['اقرار', /اقرار|مقر|مقرله|مقرّ/],
  ['ادله اثبات و شهادت', /شهادت|شاهد|بینه|دلیل|ادله اثبات/],
  ['قسم', /قسم|سوگند|حلف/],
];

const tradeDomains = [
  ['تاجر و معاملات تجارتی', /تاجر|تجارتی|تجاری|معاملات تجارتی/],
  ['دفاتر تجارتی', /دفتر روزنامه|دفتر کل|دفاتر تجارتی|دفتر دارایی/],
  ['ثبت تجارتی و اسم تجارتی', /ثبت تجارتی|اسم تجارتی|نام تجارتی/],
  ['دلالی', /دلال|دلالی/],
  ['حق‌العمل‌کاری', /حق[‌ -]?العمل|آمر/],
  ['حمل‌ونقل تجارتی', /حمل[‌ -]?و[‌ -]?نقل|متصدی حمل|مرسل[‌ -]?الیه/],
  ['برات', /برات|براتگیر|ظهرنویس|محال[‌ -]?علیه/],
  ['سفته و فته‌طلب', /سفته|فته[‌ -]?طلب/],
  ['چک', /چک/],
  ['ورشکستگی', /ورشکست|توقف|مدیر تصفیه|عضو ناظر|طلبکاران/],
  ['شرکت‌های تجارتی', /شرکت|شریک|سرمایه شرکت|سهم[‌ -]?الشرکه/],
  ['شرکت سهامی و سهام', /سهامی|سهام|ورقه سهم|پذیره[‌ -]?نویسی/],
  ['مجمع عمومی', /مجمع عمومی|مجمع مؤسس|مجمع عادی|مجمع فوق[‌ -]?العاده/],
  ['هیئت‌مدیره و مدیرعامل', /هیئت[‌ -]?مدیره|مدیرعامل|عضو هیئت/],
  ['بازرسی شرکت', /بازرس|بازرسان/],
  ['انحلال و تصفیه شرکت', /انحلال|تصفیه|مدیر تصفیه/],
];

function detectDomain(text, meta = {}, law = 'CIVIL') {
  const structural = normalizeLegalPersian([meta.section, meta.chapter, meta.part, meta.book].filter(Boolean).join(' '));
  const hay = `${structural} ${text}`;
  const list = law === 'TRADE' ? tradeDomains : civilDomains;
  for (const [name, re] of list) if (re.test(hay)) return name;
  if (structural) {
    return structural
      .replace(/^(کتاب|باب|فصل|مبحث|قسمت)\s+[^-–—]*[-–—]?\s*/u, '')
      .trim()
      .slice(0, 70) || (law === 'TRADE' ? 'حقوق تجارت' : 'حقوق مدنی');
  }
  return law === 'TRADE' ? 'حقوق تجارت' : 'حقوق مدنی';
}

function classifyRule(text) {
  const t = normalizeLegalPersian(text);
  const flags = {
    invalidity: /باطل|بطلان|غیر[‌ ]?نافذ|نافذ|اعتبار ندارد|بی[‌ ]?اعتبار/.test(t),
    obligation: /باید|مکلف|موظف|ملزم|الزام/.test(t),
    prohibition: /نمی[‌ ]?تواند|ممنوع|نباید|حق ندارد/.test(t),
    right: /حق دارد|می[‌ ]?تواند|مختار|مجاز است/.test(t),
    liability: /ضامن|مسئول|مسئولیت|جبران/.test(t),
    presumption: /محسوب|فرض می[‌ ]?شود|در حکم|تلقی/.test(t),
    condition: /اگر|هرگاه|در صورتی که|مشروط|به شرط|منوط/.test(t),
    exception: /مگر|جز در|به استثنای|الا /.test(t),
  };
  if (flags.invalidity) return ['اعتبار و اثر حقوقی', flags];
  if (flags.prohibition) return ['محدودیت یا ممنوعیت', flags];
  if (flags.obligation) return ['تکلیف قانونی', flags];
  if (flags.liability) return ['مسئولیت حقوقی', flags];
  if (flags.right) return ['حق یا اختیار', flags];
  if (flags.presumption) return ['وضعیت یا فرض قانونی', flags];
  return ['قاعده حقوقی', flags];
}

function splitClauses(text) {
  return modernize(text)
    .split(/(?<=[.؟؛])\s+|\n+/u)
    .map(s => s.trim())
    .filter(Boolean);
}

function pickCore(text) {
  const clauses = splitClauses(text);
  if (!clauses.length) return '';
  const score = s => {
    let n = Math.min(s.length, 240) / 60;
    if (/باید|مکلف|موظف|نمی‌تواند|حق دارد|می‌تواند|باطل|نافذ|ضامن|مسئول|محسوب|مگر|اگر|هرگاه|در صورتی که/.test(s)) n += 6;
    return n;
  };
  return clauses.slice().sort((a,b) => score(b)-score(a))[0];
}

function extractCondition(text) {
  const t = modernize(text);
  const m = t.match(/(?:اگر|هرگاه|در صورتی که|به شرط آنکه|مشروط بر اینکه)\s+([^؛.]{8,190})/u);
  return m ? clip(m[1], 180) : '';
}

function extractException(text) {
  const t = modernize(text);
  const m = t.match(/(?:مگر|جز در صورتی که|به استثنای)\s+([^؛.]{5,170})/u);
  return m ? clip(m[1], 160) : '';
}

function extractDeadlines(text) {
  const t = normalizeLegalPersian(text);
  const hits = [...t.matchAll(/(?:ظرف|مدت|حداکثر|حداقل|تا)\s+([^،؛.]{0,30}?\b(?:روز|ماه|سال|ساعت|هفته)\b)/gu)]
    .map(m => m[0].trim());
  return [...new Set(hits)].slice(0, 2);
}

const stopWords = new Set('این آن ماده قانون باید است می شود خواهد در از به با برای که و یا را بر اگر مگر هر هرگاه یک دو سه نزد طبق موجب صورت موارد مورد همان نیز خود طرف طرفین شخص اشخاص می تواند نمی تواند'.split(/\s+/));

function focusTerms(text) {
  const words = normalizeLegalPersian(text)
    .replace(/[.,،؛:()\[\]«»"؟]/g, ' ')
    .split(/\s+/)
    .map(w => w.replace(/^[^آ-ی]+|[^آ-ی‌]+$/g, ''))
    .filter(w => w.length >= 4 && !stopWords.has(w));
  const freq = new Map();
  for (const w of words) freq.set(w, (freq.get(w) || 0) + 1);
  return [...freq.entries()]
    .sort((a,b) => b[1]-a[1] || b[0].length-a[0].length)
    .slice(0, 4)
    .map(([w]) => w);
}

function ruleSpecificSentence(ruleType, flags) {
  if (ruleType === 'اعتبار و اثر حقوقی') return 'نکته اصلی این است که ماده مستقیماً درباره اعتبار یا اثر حقوقی رفتار صحبت می‌کند؛ پس در تست باید میان «صحیح»، «باطل» و «غیرنافذ» خلط نشود.';
  if (ruleType === 'محدودیت یا ممنوعیت') return 'این حکم مرز اختیار اشخاص را مشخص می‌کند؛ وجود یا نبودن شرط قانونی می‌تواند نتیجه را کاملاً عوض کند.';
  if (ruleType === 'تکلیف قانونی') return 'مخاطب حکم باید اقدام مقرر را انجام دهد؛ در تست، شخصِ مکلف، زمان و موضوع تکلیف را جداگانه پیدا کن.';
  if (ruleType === 'مسئولیت حقوقی') return 'محور ماده تعیین شخص مسئول و حدود مسئولیت اوست؛ رابطه میان رفتار، وضعیت مقرر در ماده و اثر جبرانی اهمیت دارد.';
  if (ruleType === 'حق یا اختیار') return 'ماده یک امکان حقوقی ایجاد می‌کند، نه الزام؛ در تست تفاوت «می‌تواند» با «باید» تعیین‌کننده است.';
  if (ruleType === 'وضعیت یا فرض قانونی') return 'قانون برای وضعیت موردنظر یک وصف یا فرض مشخص می‌سازد و آثار بعدی بر همان وصف بار می‌شود.';
  if (flags.condition) return 'حکم مطلق نیست و تحقق شرطِ مذکور در متن، پیش‌نیاز اجرای نتیجه حقوقی است.';
  return 'برای فهم ماده، موضوع، مخاطب و اثر حقوقی آن را از هم جدا کن و متن را به‌صورت یک قاعده واحد به خاطر بسپار.';
}

export function buildConceptExplanation({ number, text, law = 'CIVIL', meta = {} }) {
  const normalized = normalizeLegalPersian(text);
  const modern = modernize(normalized);
  const domain = detectDomain(modern, meta, law);
  const [ruleType, flags] = classifyRule(modern);
  const core = clip(pickCore(modern) || modern, 330);
  const condition = extractCondition(modern);
  const exception = extractException(modern);
  const deadlines = extractDeadlines(modern);
  const terms = focusTerms(modern);

  const leadVariants = [
    `مفهوم ماده ${number}: این حکم در حوزه «${domain}» یک ${ruleType} را بیان می‌کند.`,
    `ماده ${number} را باید در مبحث «${domain}» خواند؛ هسته آن یک ${ruleType} است.`,
    `کارکرد ماده ${number} در «${domain}» روشن‌کردن ${ruleType} است.`,
    `ماده ${number} در چارچوب «${domain}» مرز یک ${ruleType} را مشخص می‌کند.`,
  ];
  let simple = `${leadVariants[number % leadVariants.length]} به زبان ساده: ${core}`;
  if (condition) simple += ` شرط کلیدی: ${condition}.`;
  if (exception) simple += ` استثنای مهم: ${exception}.`;
  if (deadlines.length) simple += ` مهلت/زمان مهم: ${deadlines.join('؛ ')}.`;

  const structural = [meta.section, meta.chapter, meta.part].find(Boolean);
  let analytical = `نکته مفهومی ماده ${number}: ${ruleSpecificSentence(ruleType, flags)}`;
  if (exception) analytical += ' وجود استثنا یعنی پاسخ‌های مطلق در تست مشکوک‌اند و باید متن استثنا کنترل شود.';
  else if (condition) analytical += ' اگر شرط حذف شود یا تغییر کند، نتیجه ماده الزاماً همان نتیجه قبلی نیست.';
  if (terms.length) analytical += ` کلیدواژه‌های همین ماده: ${terms.join('، ')}.`;
  if (structural) analytical += ` جایگاه ماده در ساختار قانون: ${clip(normalizeLegalPersian(structural), 90)}.`;

  let recallQuestion;
  if (ruleType === 'تکلیف قانونی') recallQuestion = `در ماده ${number}، چه کسی باید چه کاری را در چه شرایطی انجام دهد؟`;
  else if (ruleType === 'محدودیت یا ممنوعیت') recallQuestion = `ماده ${number} چه کاری را محدود یا ممنوع می‌کند و مرز این ممنوعیت چیست؟`;
  else if (ruleType === 'حق یا اختیار') recallQuestion = `ماده ${number} چه حق یا اختیاری می‌دهد و اعمال آن به چه شرطی وابسته است؟`;
  else if (ruleType === 'مسئولیت حقوقی') recallQuestion = `طبق ماده ${number} مسئولیت بر عهده چه کسی است و اثر آن چیست؟`;
  else if (ruleType === 'اعتبار و اثر حقوقی') recallQuestion = `در ماده ${number} وضعیت اعتبار عمل حقوقی چیست و چه عاملی آن را تعیین می‌کند؟`;
  else recallQuestion = `قاعده اصلی ماده ${number} درباره «${domain}» چیست و اثر حقوقی آن چگونه بیان شده است؟`;

  return {
    simpleExplanation: simple.replace(/\.\./g, '.').trim(),
    analyticalPoint: analytical.trim(),
    recallQuestion,
    domain,
    ruleType,
  };
}
