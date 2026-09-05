export type LawCategory =
  | "constitutional"
  | "civil"
  | "commercial"
  | "procedure"
  | "criminal"
  | "public"
  | "labor"
  | "family"
  | "property"
  | "financial"
  | "ip"
  | "special";

export const CATEGORY_LABEL: Record<LawCategory, string> = {
  constitutional: "اساسی",
  civil: "مدنی",
  commercial: "تجارت",
  procedure: "آیین دادرسی",
  criminal: "کیفری",
  public: "عمومی و اداری",
  labor: "کار و تأمین اجتماعی",
  family: "خانواده",
  property: "ثبت و املاک",
  financial: "مالی و مالیاتی",
  ip: "مالکیت فکری",
  special: "قوانین خاص",
};

export type Law = {
  id: string;
  title: string;
  shortTitle: string;
  year: string;
  category: LawCategory;
  articleCount: number;
  summary: string;
  sources: string[];
  chapters: { title: string; from: number; to: number }[];
};

export type Article = {
  lawId: string;
  n: number;
  heading?: string;
  text: string;
  analysis: string;
  doctrine: string;
  related: string[];
  sources: string[];
  tags: string[];
};

export type ExamSubject = "civil" | "commercial" | "fiqh" | "procedure" | "pil";

export const SUBJECT_LABEL: Record<ExamSubject, string> = {
  civil: "حقوق مدنی",
  commercial: "حقوق تجارت",
  fiqh: "متون فقه معاملات",
  procedure: "آیین دادرسی مدنی",
  pil: "حقوق بین‌الملل خصوصی",
};

export type ExamSource = {
  bookletUrl?: string;
  keyUrl?: string;
  bookletCode?: string;
  questionNo?: number;
};

export type ExamQuestion = {
  id: string;
  year: string;
  official: boolean;
  /**
   * فقط وقتی true است که متن سؤال، سال/شماره و کلید با منبع آزمون تطبیق داده شده باشد.
   * official=true به تنهایی برای نمایش برچسب «سنجش ـ تأییدشده» کافی نیست.
   */
  verified?: boolean;
  source?: ExamSource;
  subject: ExamSubject;
  topic: string;
  stem: string;
  choices: [string, string, string, string];
  answer: 0 | 1 | 2 | 3;
  explanation: string;
  articles: string[];
  /** دام یا نکته‌ای که طراح با آن گزینه‌های نزدیک را از هم جدا می‌کند. */
  trap?: string;
};

export type ExamConfidence = "sure" | "between" | "guess";

export type ExamAttempt = {
  id: string;
  questionId: string;
  subject: ExamSubject;
  date: string;
  at: number;
  pick: number | null;
  correct: boolean;
  skipped: boolean;
  confidence: ExamConfidence;
};

export const TRACK_LAWS = ["civil", "commerce", "procedure"] as const;
export type TrackLawId = (typeof TRACK_LAWS)[number];

export const TRACK_META: Record<TrackLawId, { title: string; lawTitle: string }> = {
  civil: { title: "قانون مدنی", lawTitle: "قانون مدنی" },
  commerce: { title: "قانون تجارت", lawTitle: "قانون تجارت" },
  procedure: { title: "آیین دادرسی مدنی", lawTitle: "قانون آیین دادرسی دادگاه‌های عمومی و انقلاب در امور مدنی" },
};
