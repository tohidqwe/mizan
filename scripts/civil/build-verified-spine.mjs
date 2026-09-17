import { createHash } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const IN = path.resolve("artifacts/civil-official-sources");
const OUT = path.resolve("artifacts/civil-verified-spine");
await mkdir(OUT, { recursive: true });

const faDigits = "۰۱۲۳۴۵۶۷۸۹";
function asciiDigits(s) {
  return s.replace(/[۰-۹]/g, (d) => String(faDigits.indexOf(d)));
}
function faChars(s) {
  return s.normalize("NFKC").replace(/ي/g, "ی").replace(/ك/g, "ک");
}
function normalizeText(s) {
  return asciiDigits(faChars(s))
    .replace(/\u200c/g, " ")
    .replace(/[“”«»]/g, '"')
    .replace(/[ـ]/g, "")
    .replace(/[\s\u00a0]+/g, " ")
    .replace(/\s+([،؛:,.])/g, "$1")
    .trim();
}
function compareText(s) {
  return normalizeText(s)
    .replace(/[\s"'،؛:,.!?()\-–—]/g, "")
    .toLowerCase();
}
function sha(s) {
  return createHash("sha256").update(normalizeText(s), "utf8").digest("hex");
}

/**
 * Extract article slices conservatively. The collector never picks one of multiple
 * conflicting occurrences silently. A source with duplicate current-looking
 * article markers is reported for manual resolution.
 */
function extract(text, sourceId) {
  const input = faChars(text).replace(/\r/g, "");
  const re = /(?:^|\n)\s*ماده\s*([۰-۹0-9]{1,4})(?:\s*[-–—:]?\s*(مکرر))?\s*(?:[-–—:]|\n)/gm;
  const markers = [...input.matchAll(re)];
  const found = new Map();
  const extras = [];
  for (let i = 0; i < markers.length; i += 1) {
    const m = markers[i];
    const n = Number(asciiDigits(m[1]));
    if (!Number.isInteger(n) || n < 1 || n > 1335) continue;
    const suffix = m[2] ? "مکرر" : undefined;
    const bodyStart = m.index + m[0].length;
    const bodyEnd = markers[i + 1]?.index ?? input.length;
    const body = input.slice(bodyStart, bodyEnd).trim();
    const row = { n, suffix, text: body, normalizedSha256: sha(body), sourceId };
    if (suffix) extras.push(row);
    else {
      const list = found.get(n) ?? [];
      list.push(row);
      found.set(n, list);
    }
  }
  return { found, extras };
}

const qavaninRaw = await readFile(path.join(IN, "qavanin.txt"), "utf8");
const majlisRaw = await readFile(path.join(IN, "majlis.txt"), "utf8");
const qavanin = extract(qavaninRaw, "qavanin");
const majlis = extract(majlisRaw, "majlis");

const report = {
  generatedAt: new Date().toISOString(),
  policy: "Exact legal wording is accepted only when the two official source slices normalize to the same text. No AI text completion is allowed.",
  qavaninBaseNumbers: qavanin.found.size,
  majlisBaseNumbers: majlis.found.size,
  verified: 0,
  missing: [],
  duplicates: [],
  conflicts: [],
};
const spine = [];

for (let n = 1; n <= 1335; n += 1) {
  const q = qavanin.found.get(n) ?? [];
  const m = majlis.found.get(n) ?? [];
  if (q.length !== 1 || m.length !== 1) {
    if (q.length === 0 || m.length === 0) report.missing.push({ n, qavanin: q.length, majlis: m.length });
    if (q.length > 1 || m.length > 1) report.duplicates.push({ n, qavanin: q.length, majlis: m.length });
    continue;
  }
  const same = compareText(q[0].text) === compareText(m[0].text);
  if (!same) {
    report.conflicts.push({
      n,
      qavaninSha256: q[0].normalizedSha256,
      majlisSha256: m[0].normalizedSha256,
      qavaninPreview: normalizeText(q[0].text).slice(0, 240),
      majlisPreview: normalizeText(m[0].text).slice(0, 240),
    });
    continue;
  }
  report.verified += 1;
  spine.push({
    n,
    officialText: normalizeText(q[0].text),
    normalizedTextSha256: q[0].normalizedSha256,
    verification: "qavanin+majlis",
  });
}

await writeFile(path.join(OUT, "spine-report.json"), JSON.stringify(report, null, 2), "utf8");
await writeFile(path.join(OUT, "civil-verified-spine.json"), JSON.stringify(spine, null, 2), "utf8");
console.log(JSON.stringify(report, null, 2));

if (report.verified !== 1335 || report.missing.length || report.duplicates.length || report.conflicts.length) {
  console.error("CIVIL_TEXT_GATE_CLOSED: the 1..1335 official-text spine is not a clean two-source match.");
  process.exitCode = 3;
}
