import assert from "node:assert/strict";
import test from "node:test";
import { auditCivilContent, type VerifiedCivilArticle } from "./civil-verified-schema.ts";

function record(n: number): VerifiedCivilArticle {
  return {
    n,
    officialText: `متن ${n}`,
    status: "active",
    sources: [
      {
        authority: "qavanin",
        title: "سامانه ملی قوانین و مقررات",
        url: "https://qavanin.ir/Law/TreeText/178971",
        retrievedAt: "2026-09-17",
      },
      {
        authority: "majlis",
        title: "مرکز پژوهش‌های مجلس",
        url: "https://rc.majlis.ir/fa/law/show/97937",
        retrievedAt: "2026-09-17",
      },
    ],
    verification: { status: "verified", checkedAt: "2026-09-17" },
    recallPrompt: `حکم ماده ${n} چیست؟`,
    twoOptionQuestion: {
      id: `civil-${n}`,
      stem: `کدام گزاره درباره ماده ${n} درست است؟`,
      choices: ["الف", "ب"],
      answer: 0,
      rationale: "توضیح",
      doctrineRefs: [],
      officialExamItem: false,
    },
    keywords: [],
    relatedArticles: [],
  };
}

test("release gate fails when any base article is missing", () => {
  const result = auditCivilContent([record(1)]);
  assert.equal(result.ok, false);
  assert.ok(result.issues.some((x) => x.code === "MISSING_BASE_ARTICLE" && x.articleNo === 2));
});

test("release gate passes a fully verified 1..1335 spine", () => {
  const records = Array.from({ length: 1335 }, (_, i) => record(i + 1));
  const result = auditCivilContent(records);
  assert.equal(result.ok, true);
  assert.equal(result.baseArticleCount, 1335);
  assert.equal(result.verifiedBaseArticleCount, 1335);
});

test("named doctrine without a bibliographic reference is rejected", () => {
  const records = Array.from({ length: 1335 }, (_, i) => record(i + 1));
  records[189].twoOptionQuestion.rationale = "به نظر دکتر کاتوزیان این تفکیک مهم است.";
  const result = auditCivilContent(records);
  assert.ok(result.issues.some((x) => x.code === "UNSOURCED_DOCTRINE_ATTRIBUTION" && x.articleNo === 190));
});
