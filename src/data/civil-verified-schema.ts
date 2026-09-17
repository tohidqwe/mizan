export type LegalAuthority = "qavanin" | "majlis" | "official-gazette";

export type CivilArticleStatus = "active" | "amended" | "repealed" | "deleted";

export type LegalSourceRef = {
  authority: LegalAuthority;
  title: string;
  url: string;
  retrievedAt: string;
  /** Gazette issue/registration or amendment-law identifier when available. */
  publicationId?: string;
  /** A stable digest of the normalized text captured from this source. */
  normalizedTextSha256?: string;
};

export type DoctrineRef = {
  author: string;
  work: string;
  edition?: string;
  volume?: string;
  page?: string;
  publisher?: string;
  year?: string;
};

export type TwoOptionQuestion = {
  id: string;
  stem: string;
  choices: [string, string];
  answer: 0 | 1;
  rationale: string;
  /** No professor may be named in rationale unless a traceable reference exists here. */
  doctrineRefs: DoctrineRef[];
  /** True only for verbatim/traceable exam items; otherwise this must be false. */
  officialExamItem: boolean;
  examSource?: {
    year: string;
    booklet: string;
    questionNo: number;
    sourceUrl: string;
  };
};

export type VerifiedCivilArticle = {
  /** Base Civil Code article number, always 1..1335. */
  n: number;
  /** For supplementary records such as «مکرر». Base 1..1335 still remains the release spine. */
  suffix?: "مکرر";
  heading?: string;
  officialText: string;
  status: CivilArticleStatus;
  statusNote?: string;
  sources: LegalSourceRef[];
  /** Set only after source-normalized text comparison succeeds. */
  verification: {
    status: "verified" | "conflict" | "pending";
    checkedAt?: string;
    normalizedTextSha256?: string;
  };
  recallPrompt: string;
  twoOptionQuestion: TwoOptionQuestion;
  keywords: string[];
  relatedArticles: number[];
};

export const CIVIL_AUTHORITY_SOURCES = {
  qavanin: {
    authority: "qavanin" as const,
    title: "سامانه ملی قوانین و مقررات جمهوری اسلامی ایران — قانون مدنی",
    url: "https://qavanin.ir/Law/TreeText/178971",
  },
  majlis: {
    authority: "majlis" as const,
    title: "مرکز پژوهش‌های مجلس شورای اسلامی — قانون مدنی",
    url: "https://rc.majlis.ir/fa/law/show/97937",
  },
  officialGazette: {
    authority: "official-gazette" as const,
    title: "روزنامه رسمی جمهوری اسلامی ایران",
    url: "https://rrk.ir/",
    publicationId: "1877",
  },
} as const;

export type ContentGateIssue = {
  code:
    | "MISSING_BASE_ARTICLE"
    | "DUPLICATE_BASE_ARTICLE"
    | "INVALID_NUMBER"
    | "UNVERIFIED_TEXT"
    | "INSUFFICIENT_AUTHORITIES"
    | "EMPTY_OFFICIAL_TEXT"
    | "EMPTY_RECALL_PROMPT"
    | "INVALID_TWO_OPTION_QUESTION"
    | "UNSOURCED_DOCTRINE_ATTRIBUTION";
  articleNo?: number;
  message: string;
};

export type ContentGateResult = {
  ok: boolean;
  baseArticleCount: number;
  verifiedBaseArticleCount: number;
  issues: ContentGateIssue[];
};

function uniqueAuthorities(article: VerifiedCivilArticle): Set<LegalAuthority> {
  return new Set(article.sources.map((s) => s.authority));
}

/**
 * The app must never call the Civil Code dataset complete unless this gate passes.
 * This protects against AI-invented legal text and fake exam/doctrine attribution.
 */
export function auditCivilContent(records: VerifiedCivilArticle[]): ContentGateResult {
  const issues: ContentGateIssue[] = [];
  const base = records.filter((r) => !r.suffix);
  const byNo = new Map<number, VerifiedCivilArticle[]>();

  for (const article of base) {
    if (!Number.isInteger(article.n) || article.n < 1 || article.n > 1335) {
      issues.push({
        code: "INVALID_NUMBER",
        articleNo: article.n,
        message: `شماره ماده خارج از بازه ۱ تا ۱۳۳۵ است: ${article.n}`,
      });
      continue;
    }
    const list = byNo.get(article.n) ?? [];
    list.push(article);
    byNo.set(article.n, list);
  }

  for (let n = 1; n <= 1335; n += 1) {
    const found = byNo.get(n) ?? [];
    if (found.length === 0) {
      issues.push({ code: "MISSING_BASE_ARTICLE", articleNo: n, message: `ماده ${n} موجود نیست.` });
      continue;
    }
    if (found.length > 1) {
      issues.push({ code: "DUPLICATE_BASE_ARTICLE", articleNo: n, message: `ماده ${n} تکراری است.` });
      continue;
    }

    const article = found[0];
    if (article.verification.status !== "verified") {
      issues.push({
        code: "UNVERIFIED_TEXT",
        articleNo: n,
        message: `متن ماده ${n} هنوز از دروازه تطبیق منابع رسمی عبور نکرده است.`,
      });
    }

    const authorities = uniqueAuthorities(article);
    if (authorities.size < 2 || (!authorities.has("qavanin") && !authorities.has("majlis"))) {
      issues.push({
        code: "INSUFFICIENT_AUTHORITIES",
        articleNo: n,
        message: `ماده ${n} حداقل دو منبع مستقل رسمی/تقنینی قابل ردیابی ندارد.`,
      });
    }

    if ((article.status === "active" || article.status === "amended") && !article.officialText.trim()) {
      issues.push({ code: "EMPTY_OFFICIAL_TEXT", articleNo: n, message: `متن رسمی ماده ${n} خالی است.` });
    }
    if ((article.status === "deleted" || article.status === "repealed") && !article.statusNote?.trim()) {
      issues.push({
        code: "EMPTY_OFFICIAL_TEXT",
        articleNo: n,
        message: `ماده ${n} حذف/نسخ شده ولی مستند وضعیت ندارد.`,
      });
    }

    if (!article.recallPrompt.trim()) {
      issues.push({ code: "EMPTY_RECALL_PROMPT", articleNo: n, message: `پرسش بازیابی ماده ${n} خالی است.` });
    }

    const q = article.twoOptionQuestion;
    if (!q.stem.trim() || q.choices.length !== 2 || !q.choices[0].trim() || !q.choices[1].trim() || ![0, 1].includes(q.answer)) {
      issues.push({
        code: "INVALID_TWO_OPTION_QUESTION",
        articleNo: n,
        message: `تست دوگزینه‌ای ماده ${n} معتبر نیست.`,
      });
    }

    // Doctrine may be absent; what is forbidden is pretending an attribution exists
    // without a traceable bibliographic reference. Generation/import code must enforce
    // this whenever a named scholar is mentioned.
    if (/کاتوزیان|امامی|شهیدی|صفایی|جعفری\s*لنگرودی/.test(q.rationale) && q.doctrineRefs.length === 0) {
      issues.push({
        code: "UNSOURCED_DOCTRINE_ATTRIBUTION",
        articleNo: n,
        message: `در توضیح ماده ${n} نام استاد آمده ولی ارجاع کتابشناختی ثبت نشده است.`,
      });
    }
  }

  const verifiedBaseArticleCount = base.filter((a) => a.verification.status === "verified").length;
  return {
    ok: issues.length === 0 && base.length === 1335,
    baseArticleCount: base.length,
    verifiedBaseArticleCount,
    issues,
  };
}
