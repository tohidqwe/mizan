import { writeFile, mkdir } from 'node:fs/promises';
import { CIVIL_ARTICLES } from '../../src/data/civil.ts';

const expected = 1335;
const byNumber = new Map(CIVIL_ARTICLES.map((a) => [Number(a.n), a]));
const missing = [];
for (let n = 1; n <= expected; n += 1) if (!byNumber.has(n)) missing.push(n);
if (missing.length) throw new Error(`Civil Code dataset is incomplete. Missing: ${missing.join(', ')}`);
if (byNumber.size !== expected) throw new Error(`Expected exactly ${expected} numbered articles; got ${byNumber.size}`);

const rrkBaseLaw = {
  publicationRegister: '1877',
  publicationPage: '210',
  publicationVolume: '1',
  publicationSeries: '9',
  title: 'قانون مدنی',
  directVerification: false,
  note: 'Direct rrk.ir verification is required before VERIFIED status is permitted.'
};

const records = Array.from({ length: expected }, (_, i) => {
  const n = i + 1;
  const a = byNumber.get(n);
  const sourceHints = Array.isArray(a.sources) ? a.sources : [];
  const text = String(a.text ?? '').trim();
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
    source1: sourceHints[0] ?? 'سامانه ملی قوانین و مقررات',
    source2: 'روزنامه رسمی کشور — requires direct rrk.ir article/amendment verification',
    rrkProvenance: rrkBaseLaw,
    legalStatus: repealed ? 'REPEALED' : 'CURRENT_OR_AMENDED',
    verificationStatus: 'PENDING_RRK_DIRECT',
    verificationDate: ''
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
  verified: records.filter((x) => x.verificationStatus === 'VERIFIED').length,
  pendingRrkDirect: records.filter((x) => x.verificationStatus === 'PENDING_RRK_DIRECT').length,
  releaseGateOpen: records.every((x) => x.verificationStatus === 'VERIFIED' || x.legalStatus === 'REPEALED')
};
await writeFile(new URL('../app/src/main/assets/civil_dataset_audit.json', import.meta.url), JSON.stringify(report, null, 2), 'utf8');
console.log(JSON.stringify(report, null, 2));
if (report.releaseGateOpen) console.log('RELEASE_GATE_OPEN');
else console.log('RELEASE_GATE_BLOCKED: direct rrk.ir verification incomplete');
