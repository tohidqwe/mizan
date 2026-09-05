import type { ExamAttempt, ExamQuestion, ExamSubject } from "@/data/types";
import { todayKey } from "./utils";

export const CORE_PHD_SUBJECTS: ExamSubject[] = ["civil", "commercial", "fiqh"];

/**
 * فعلاً تا زمانی که بانک زبان عمومیِ دارای منبع وارد نشده، مأموریت ۲۰تایی بر سه درس تخصصی توزیع می‌شود.
 * وزن‌ها بر اساس وضعیت فعلی داوطلب: مدنی متوسط، تجارت ضعیف، متون فقه ضعیف.
 */
export const DAILY_TARGET: Record<"civil" | "commercial" | "fiqh", number> = {
  civil: 5,
  commercial: 8,
  fiqh: 7,
};

export function todayAttempts(attempts: ExamAttempt[]) {
  const today = todayKey();
  return attempts.filter((a) => a.date === today);
}

export function dailyProgress(attempts: ExamAttempt[]) {
  const today = todayAttempts(attempts);
  const bySubject = {
    civil: today.filter((a) => a.subject === "civil").length,
    commercial: today.filter((a) => a.subject === "commercial").length,
    fiqh: today.filter((a) => a.subject === "fiqh").length,
  };
  const total = bySubject.civil + bySubject.commercial + bySubject.fiqh;
  return { total, remaining: Math.max(0, 20 - total), bySubject, complete: total >= 20 };
}

function questionWeakness(q: ExamQuestion, attempts: ExamAttempt[]) {
  const own = attempts.filter((a) => a.questionId === q.id);
  if (!own.length) return 8;
  return own.reduce((score, a) => {
    if (!a.correct) score += 7;
    if (a.confidence === "guess") score += 4;
    if (a.confidence === "between") score += 2;
    if (a.correct && a.confidence === "sure") score -= 3;
    return score;
  }, 0);
}

function stableDailyNoise(id: string) {
  const key = `${todayKey()}-${id}`;
  let h = 0;
  for (let i = 0; i < key.length; i += 1) h = (h * 31 + key.charCodeAt(i)) | 0;
  return Math.abs(h % 1000) / 1000;
}

function rankPool(pool: ExamQuestion[], attempts: ExamAttempt[]) {
  return [...pool].sort((a, b) => {
    const verifiedA = a.verified ? 2 : 0;
    const verifiedB = b.verified ? 2 : 0;
    const sa = questionWeakness(a, attempts) + verifiedA + stableDailyNoise(a.id);
    const sb = questionWeakness(b, attempts) + verifiedB + stableDailyNoise(b.id);
    return sb - sa;
  });
}

export function buildDailyMission(questions: ExamQuestion[], attempts: ExamAttempt[]) {
  const today = todayAttempts(attempts);
  const already = new Set(today.map((a) => a.questionId));
  const picked: ExamQuestion[] = [];

  for (const subject of CORE_PHD_SUBJECTS) {
    const target = DAILY_TARGET[subject as keyof typeof DAILY_TARGET];
    const done = today.filter((a) => a.subject === subject).length;
    const need = Math.max(0, target - done);
    const pool = questions.filter((q) => q.subject === subject && !already.has(q.id));
    picked.push(...rankPool(pool, attempts).slice(0, need));
  }

  // اگر یک درس به اندازه کافی سؤال ندارد، با سؤال‌های تخصصی دیگر تا سقف باقیمانده پر می‌کنیم.
  const progress = dailyProgress(attempts);
  const needTotal = Math.max(0, progress.remaining);
  if (picked.length < needTotal) {
    const selected = new Set(picked.map((q) => q.id));
    const fallback = questions.filter(
      (q) => CORE_PHD_SUBJECTS.includes(q.subject) && !already.has(q.id) && !selected.has(q.id),
    );
    picked.push(...rankPool(fallback, attempts).slice(0, needTotal - picked.length));
  }

  return picked.slice(0, needTotal);
}

export function subjectAccuracy(attempts: ExamAttempt[], subject: ExamSubject) {
  const list = attempts.filter((a) => a.subject === subject && !a.skipped);
  if (!list.length) return null;
  const correct = list.filter((a) => a.correct).length;
  return Math.round((correct / list.length) * 100);
}

/** نمره آزمونی با منفی یک‌سوم: درست +۱، غلط -۱/۳، بی‌پاسخ صفر. */
export function negativeMarkScore(attempts: ExamAttempt[]) {
  const correct = attempts.filter((a) => a.correct).length;
  const wrong = attempts.filter((a) => !a.correct && !a.skipped).length;
  return correct - wrong / 3;
}
