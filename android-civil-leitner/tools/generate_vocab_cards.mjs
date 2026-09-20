import fs from 'node:fs/promises';

const OPENJAM_COMMIT = 'e05622665494f18d414190b5f738dc1eb4b414fa';
const RAW = 'https://raw.githubusercontent.com/amirj4m/openjam/' + OPENJAM_COMMIT;
const WORDS_URL = RAW + '/data/json/words_en.json';
const TRANSLATIONS_URL = RAW + '/data/json/translations_fa.json';
const BOOK_URLS = [
  RAW + '/books/gre/words.json',
  RAW + '/books/toefl/words.json',
  RAW + '/books/ielts/words.json',
  RAW + '/books/504-essential/words.json',
];
const OUT = 'app/src/main/assets';
const TARGET = 2000;

const examPriority = [
  'autonomy','autonomous','indispensable','beneficial','compromise','demolish','demolition',
  'distort','distorted','plausible','spontaneous','impose','diminish','longevity','inevitable',
  'tangible','endeavor','contract','obligation','liability','damages','remedy','breach','consent',
  'capacity','property','ownership','possession','inheritance','heir','estate','sale','lease',
  'agency','guarantee','mortgage','pledge','company','shareholder','director','bankruptcy',
  'insolvency','instrument','cheque','evidence','claim','defendant','plaintiff','jurisdiction',
  'statute'
];

const normalize = (value) => String(value ?? '')
  .trim()
  .toLowerCase()
  .replace(/[’']/g, "'");

async function fetchJson(url) {
  const response = await fetch(url, {
    headers: {'user-agent':'Dr-Tohid-Najafian-Course/0.3'}
  });
  if (!response.ok) throw new Error('Fetch failed ' + response.status + ': ' + url);
  return response.json();
}

const [wordsData, translationsData, ...books] = await Promise.all([
  fetchJson(WORDS_URL),
  fetchJson(TRANSLATIONS_URL),
  ...BOOK_URLS.map(fetchJson),
]);

if (!Array.isArray(wordsData) || !Array.isArray(translationsData)) {
  throw new Error('Openjam dataset shape is invalid');
}

const translationBySense = new Map();
for (const row of translationsData) {
  if (row?.language_code !== 'fa') continue;
  const meaning = String(row?.meaning ?? '').trim();
  if (!meaning) continue;
  const existing = translationBySense.get(row.sense_id) ?? [];
  if (!existing.includes(meaning)) existing.push(meaning);
  translationBySense.set(row.sense_id, existing);
}

const wordByEnglish = new Map();
for (const row of wordsData) {
  const english = normalize(row?.english);
  if (!english || wordByEnglish.has(english)) continue;
  const meanings = [];
  for (const sense of row?.senses ?? []) {
    for (const meaning of translationBySense.get(sense.id) ?? []) {
      if (!meanings.includes(meaning)) meanings.push(meaning);
    }
  }
  if (!meanings.length) continue;
  wordByEnglish.set(english, {
    english,
    level: row.level ?? '',
    frequencyRank: Number.isFinite(row.frequency_rank) ? row.frequency_rank : null,
    meanings,
  });
}

const ordered = [];
const seen = new Set();
function pushWord(word) {
  const english = normalize(word);
  if (!english || seen.has(english) || !wordByEnglish.has(english)) return;
  seen.add(english);
  ordered.push(english);
}

examPriority.forEach(pushWord);

for (const book of books) {
  const list = Array.isArray(book) ? book : book?.words;
  if (!Array.isArray(list)) continue;
  for (const item of list) pushWord(typeof item === 'string' ? item : item?.english);
}

[...wordByEnglish.values()]
  .sort((a,b) => (a.frequencyRank ?? Number.MAX_SAFE_INTEGER) - (b.frequencyRank ?? Number.MAX_SAFE_INTEGER))
  .forEach(row => pushWord(row.english));

if (ordered.length < TARGET) {
  throw new Error('ENGLISH_2000_UNIQUE_GATE failed: only ' + ordered.length + ' translated unique words');
}

const selected = ordered.slice(0, TARGET);
const cards = selected.map((english, index) => {
  const row = wordByEnglish.get(english);
  const meanings = row.meanings.slice(0, 4).join('، ');
  if (!meanings) throw new Error('Missing Persian meaning: ' + english);
  return {
    id: 'VOCAB:WORD:' + String(index + 1).padStart(4, '0'),
    domain: 'VOCAB',
    ordinal: index + 1,
    title: english,
    prompt: 'معنی «' + english + '» چیست؟',
    answer: meanings,
    explanation: row.level ? 'سطح تقریبی CEFR: ' + row.level : '',
    sourceName: 'Openjam vocabulary dataset (MIT)',
    sourceUrl: 'https://github.com/amirj4m/openjam/tree/' + OPENJAM_COMMIT,
    verificationStatus: 'OPEN_DATA_TRANSLATED'
  };
});

const unique = new Set(cards.map(x => x.title));
if (cards.length !== TARGET || unique.size !== TARGET) {
  throw new Error('ENGLISH_2000_UNIQUE_GATE failed');
}
if (cards.some(x => !String(x.answer).trim())) {
  throw new Error('ENGLISH_PERSIAN_MEANING_GATE failed');
}

await fs.mkdir(OUT, {recursive:true});
await fs.writeFile(OUT + '/vocab_cards.json', JSON.stringify(cards, null, 2), 'utf8');
await fs.writeFile(OUT + '/vocab_manifest.json', JSON.stringify({
  count: cards.length,
  target: TARGET,
  unique: unique.size,
  allCardsHavePersianMeaning: true,
  dataset: 'amirj4m/openjam',
  datasetCommit: OPENJAM_COMMIT,
  license: 'MIT',
  licenseUrl: 'https://github.com/amirj4m/openjam/blob/' + OPENJAM_COMMIT + '/LICENSE',
  prioritySources: ['GRE','TOEFL','IELTS','504 Essential Words','frequency rank'],
  generatedAt: new Date().toISOString()
}, null, 2), 'utf8');

console.log(JSON.stringify({
  ENGLISH_2000_UNIQUE_GATE: 'PASS',
  ENGLISH_PERSIAN_MEANING_GATE: 'PASS',
  count: cards.length,
  unique: unique.size,
}, null, 2));
