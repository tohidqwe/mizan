import { chromium } from 'playwright';
import crypto from 'node:crypto';
import fs from 'node:fs/promises';
import path from 'node:path';

const OFFICIAL_ID = '12021850837713548188';
const OFFICIAL_URL = `https://qavanin.ir/Law/TreeText/?IDS=${OFFICIAL_ID}`;
const PRINT_URL = `https://qavanin.ir/Law/PrintText/?IDS=${OFFICIAL_ID}&font=`;
const RRK_REFERENCE = 'روزنامه رسمی کشور: شماره ثبت 1877، چاپ 2، دوره 9، جلد 1، صفحه 210 (اصل قانون مدنی)';
const out = 'qavanin-official';
const assets = 'android-civil-leitner/app/src/main/assets';
await fs.mkdir(out, { recursive: true });
await fs.mkdir(assets, { recursive: true });

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  locale: 'fa-IR',
  timezoneId: 'Asia/Tehran',
  userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36',
});
const page = await context.newPage();
page.setDefaultTimeout(30000);

let loaded = false;
for (const url of [PRINT_URL, OFFICIAL_URL]) {
  try {
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 });
    for (let i = 0; i < 12; i++) {
      await page.waitForTimeout(2500);
      const count = await page.locator('p.SecTex span.bold').count().catch(() => 0);
      const body = await page.locator('body').innerText().catch(() => '');
      if (count > 1300 && /قانون\s+مدن[يی]/.test(body) && /ماد[ۀه]\s*1335/.test(body)) {
        loaded = true;
        break;
      }
    }
    if (loaded) break;
  } catch (e) {
    await fs.writeFile(`${out}/error.txt`, String(e));
  }
}

const html = await page.content();
const body = await page.locator('body').innerText().catch(() => '');
await fs.writeFile(`${out}/final-url.txt`, page.url());
await fs.writeFile(`${out}/official.html`, html);
await fs.writeFile(`${out}/official.txt`, body);

if (!loaded) {
  await fs.writeFile(`${out}/meta.json`, JSON.stringify({ loaded: false, url: page.url(), length: body.length }, null, 2));
  await browser.close();
  process.exit(2);
}

const rawItems = await page.locator('p.SecTex').evaluateAll((nodes) => nodes.map((p) => {
  const labelNode = p.querySelector('span.bold');
  return {
    label: (labelNode?.textContent || '').replace(/\s+/g, ' ').trim(),
    text: (p.textContent || '').replace(/\s+/g, ' ').trim(),
  };
}));

const sha256 = (s) => crypto.createHash('sha256').update(s, 'utf8').digest('hex');
const todayTehran = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Tehran', year: 'numeric', month: '2-digit', day: '2-digit',
}).format(new Date());

const structure = { book: '', part: '', chapter: '', section: '' };
const allArticles = [];
let current = null;

function cleanLabel(s) {
  return s.replace(/مادۀ/g, 'ماده').replace(/\u200c/g, '‌').trim();
}
function statusFromLabel(label) {
  if (/منسوخ[هه]/.test(label)) return 'REPEALED';
  if (/اصلاح[يی]|جایگزین|الحاق[يی]/.test(label)) return 'ACTIVE_AMENDED';
  return 'ACTIVE';
}
function stripLeadingLabel(full, label) {
  let s = full;
  if (s.startsWith(label)) s = s.slice(label.length);
  return s.replace(/^\s*[-–—ـ]\s*/, '').trim();
}
function finalizeCurrent() {
  if (!current) return;
  current.officialText = current.parts.filter(Boolean).join('\n').trim();
  delete current.parts;
  current.contentHash = sha256(current.officialText);
  allArticles.push(current);
  current = null;
}

for (const item of rawItems) {
  const label = cleanLabel(item.label);
  const fullText = item.text.trim();
  const articleMatch = label.match(/^ماده\s*(\d{1,4})\s*(مكرر|مکرر)?\s*(.*)$/);

  if (articleMatch) {
    finalizeCurrent();
    const number = Number(articleMatch[1]);
    const suffix = articleMatch[2] ? 'مکرر' : '';
    const legalStatus = statusFromLabel(label);
    const mainText = stripLeadingLabel(fullText, item.label);
    current = {
      articleNumber: number,
      suffix,
      label,
      legalStatus,
      book: structure.book,
      part: structure.part,
      chapter: structure.chapter,
      section: structure.section,
      parts: [mainText],
    };
    continue;
  }

  if (/^تبصره/.test(label) && current) {
    current.parts.push(fullText);
    continue;
  }

  // Structural headings belong to the next articles, not to the previous article text.
  if (/^(كتاب|کتاب)/.test(label)) {
    finalizeCurrent();
    structure.book = fullText;
    structure.part = '';
    structure.chapter = '';
    structure.section = '';
  } else if (/^باب/.test(label)) {
    finalizeCurrent();
    structure.part = fullText;
    structure.chapter = '';
    structure.section = '';
  } else if (/^فصل/.test(label)) {
    finalizeCurrent();
    structure.chapter = fullText;
    structure.section = '';
  } else if (/^(مبحث|قسمت)/.test(label)) {
    finalizeCurrent();
    structure.section = fullText;
  } else if (!label && /^(جلد|فقره|اول\s+-|دوم\s+-|سوم\s+-|چهارم\s+-|پنجم\s+-|ششم\s+-|هفتم\s+-|هشتم\s+-|نهم\s+-|دهم\s+-)/.test(fullText)) {
    finalizeCurrent();
    if (/^جلد/.test(fullText)) {
      structure.book = fullText;
      structure.part = '';
      structure.chapter = '';
      structure.section = '';
    } else {
      structure.section = fullText;
    }
  }
}
finalizeCurrent();

const mains = allArticles.filter((a) => !a.suffix).sort((a, b) => a.articleNumber - b.articleNumber);
const supplemental = allArticles.filter((a) => a.suffix).sort((a, b) => a.articleNumber - b.articleNumber);
const expected = Array.from({ length: 1335 }, (_, i) => i + 1);
const actual = mains.map((a) => a.articleNumber);

if (mains.length !== 1335 || actual.some((n, i) => n !== expected[i])) {
  const missing = expected.filter((n) => !actual.includes(n));
  const duplicates = actual.filter((n, i) => actual.indexOf(n) !== i);
  throw new Error(`Main article validation failed: count=${mains.length}; missing=${missing.join(',')}; duplicates=${duplicates.join(',')}`);
}
if (supplemental.length !== 4 || supplemental.map((a) => `${a.articleNumber}${a.suffix}`).join(',') !== '218مکرر,881مکرر,1313مکرر,1328مکرر') {
  throw new Error(`Unexpected supplemental provisions: ${supplemental.map((a) => `${a.articleNumber}${a.suffix}`).join(',')}`);
}
if (mains.some((a) => !a.officialText)) throw new Error('At least one main article has empty official text');

const spot = new Map(mains.map((a) => [a.articleNumber, a]));
const mustContain = [
  [1, 'روزنامه رسمي'],
  [2, 'پانزده روز'],
  [190, 'قصد طرفين'],
  [946, 'اموال غيرمنقول'],
  [1041, '13 سال'],
  [1335, 'توسل به قسم'],
];
for (const [n, needle] of mustContain) {
  if (!spot.get(n)?.officialText.includes(needle)) throw new Error(`Spot check failed for article ${n}: ${needle}`);
}
for (const n of [947, 1306, 1307, 1308, 1310, 1311]) {
  if (spot.get(n)?.legalStatus !== 'REPEALED') throw new Error(`Expected article ${n} to be marked REPEALED`);
}

const toSeed = (a) => ({
  articleNumber: a.articleNumber,
  officialText: a.officialText,
  book: a.book || '',
  part: a.part || '',
  chapter: a.chapter || '',
  section: a.section || '',
  topic: a.legalStatus === 'REPEALED' ? 'ماده منسوخ' : a.legalStatus === 'ACTIVE_AMENDED' ? 'متن اصلاحی/الحاقی معتبر' : '',
  keywords: [],
  recallQuestion: `حکم و متن ماده ${a.articleNumber} قانون مدنی چیست؟`,
  twoChoiceQuestion: '',
  simpleExplanation: '',
  analyticalPoint: '',
  importantPoints: `وضعیت منبع رسمی: ${a.legalStatus}; برچسب منبع: ${a.label}`,
  relatedArticles: [],
  source1: `سامانه ملی قوانین و مقررات جمهوری اسلامی ایران - ${OFFICIAL_URL}`,
  source2: RRK_REFERENCE,
  verificationStatus: 'VERIFIED_OFFICIAL',
  verificationDate: todayTehran,
  sourceHash: a.contentHash,
});

const seed = mains.map(toSeed);
const supplementalSeed = supplemental.map((a) => ({
  articleKey: `${a.articleNumber}${a.suffix}`,
  articleNumber: a.articleNumber,
  suffix: a.suffix,
  officialText: a.officialText,
  legalStatus: a.legalStatus,
  book: a.book || '',
  part: a.part || '',
  chapter: a.chapter || '',
  section: a.section || '',
  source1: `سامانه ملی قوانین و مقررات جمهوری اسلامی ایران - ${OFFICIAL_URL}`,
  source2: RRK_REFERENCE,
  verificationStatus: 'VERIFIED_OFFICIAL',
  verificationDate: todayTehran,
  sourceHash: a.contentHash,
}));

const sourceBodyHash = sha256(body);
const seedJson = JSON.stringify(seed, null, 2) + '\n';
const supplementalJson = JSON.stringify(supplementalSeed, null, 2) + '\n';
const manifest = {
  law: 'قانون مدنی',
  officialConsolidatedSource: OFFICIAL_URL,
  officialSourceId: OFFICIAL_ID,
  officialGazetteReference: RRK_REFERENCE,
  officialGazetteLiveFetch: false,
  officialGazetteLiveFetchNote: 'rrk.ir did not respond to the CI/web retrieval attempts; no claim of live RRK page verification is made.',
  verificationDateTehran: todayTehran,
  retrievedUrl: page.url(),
  mainArticleCount: seed.length,
  firstArticleNumber: seed[0].articleNumber,
  lastArticleNumber: seed.at(-1).articleNumber,
  supplementalCount: supplementalSeed.length,
  supplementalKeys: supplementalSeed.map((a) => a.articleKey),
  repealedMainArticles: seed.filter((a) => a.topic === 'ماده منسوخ').map((a) => a.articleNumber),
  sourceBodySha256: sourceBodyHash,
  seedSha256: sha256(seedJson),
};

await fs.writeFile(path.join(assets, 'civil_seed.json'), seedJson);
await fs.writeFile(path.join(assets, 'civil_supplemental.json'), supplementalJson);
await fs.writeFile(path.join(assets, 'civil_source_manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
await fs.writeFile(`${out}/meta.json`, JSON.stringify({ loaded: true, ...manifest }, null, 2));
await fs.writeFile(`${out}/validated_seed.json`, seedJson);

await browser.close();
console.log(JSON.stringify(manifest, null, 2));
