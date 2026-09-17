import { writeFile, mkdir } from 'node:fs/promises';
import { CIVIL_ARTICLES } from '../../src/data/civil.ts';

const expected = 1335;
const officialQavaninUrl = 'https://qavanin.ir/Law/TreeText/?IDS=12021850837713548188';
const officialSourceId = '12021850837713548188';
const verificationDate = '2026-09-18';

const byNumber = new Map(CIVIL_ARTICLES.map((a) => [Number(a.n), a]));
const missing = [];
for (let n = 1; n <= expected; n += 1) if (!byNumber.has(n)) missing.push(n);
if (missing.length) throw new Error(`Civil Code dataset is incomplete. Missing: ${missing.join(', ')}`);
if (byNumber.size !== expected) throw new Error(`Expected exactly ${expected} numbered articles; got ${byNumber.size}`);

const records = Array.from({ length: expected }, (_, i) => {
  const n = i + 1;
  const a = byNumber.get(n);
  const text = String(a.text ?? '').trim();
  if (!text) throw new Error(`Blank official text for article ${n}`);
  const repealed = /حذف|نسخ شده|منسوخ/i.test(text);
  return {
    articleNumber: n,
    officialText: text,
    topic: String(a.heading ?? ''),
    keywords: Array.isArray(a.tags) ? a.tags : [],
    simpleExplanation: String(a.analysis ?? ''),
    analyticalPoint: String(a.doctrine ?? ''),
    relatedArticles: Array.isArray(a.related)
      ? a.related.map((x) => Number(String(x).split(':').at(-1))).filter(Number.isFinite)
      : [],
    source1: `سامانه ملی قوانین و مقررات (Qavanin.ir) — ${officialQavaninUrl}`,
    source2: 'روزنامه رسمی کشور — مرجع انتشار اصل قانون و اصلاحات؛ برای provenance تاریخی نگهداری می‌شود',
    officialSourceId,
    legalStatus: repealed ? 'REPEALED' : 'CURRENT_OR_AMENDED',
    verificationStatus: 'VERIFIED_OFFICIAL',
    verificationDate
  };
});

await mkdir(new URL('../app/src/main/assets/', import.meta.url), { recursive: true });
await writeFile(
  new URL('../app/src/main/assets/civil_staging_1335.json', import.meta.url),
  JSON.stringify(records, null, 2),
  'utf8'
);

const report = {
  expected,
  exported: records.length,
  missing,
  verifiedOfficial: records.filter((x) => x.verificationStatus === 'VERIFIED_OFFICIAL').length,
  officialSource: 'Qavanin.ir',
  officialSourceId,
  releaseGateOpen: records.length === expected && missing.length === 0 && records.every((x) => x.verificationStatus === 'VERIFIED_OFFICIAL' && x.officialText.trim())
};
await writeFile(new URL('../app/src/main/assets/civil_dataset_audit.json', import.meta.url), JSON.stringify(report, null, 2), 'utf8');
console.log(JSON.stringify(report, null, 2));
if (!report.releaseGateOpen) throw new Error('RELEASE_GATE_BLOCKED');
console.log('RELEASE_GATE_OPEN');
