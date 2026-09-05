import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { QUESTIONS } from "@/data";
import {
  SUBJECT_LABEL,
  type ExamConfidence,
  type ExamQuestion,
  type ExamSubject,
} from "@/data/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { useMizan } from "@/lib/store";
import { buildDailyMission } from "@/lib/exam-coach";
import { cn, toFaDigits } from "@/lib/utils";
import { z } from "zod";

const searchSchema = z.object({
  n: z.coerce.number().optional(),
  subjects: z.string().optional(),
  year: z.string().optional(),
  mode: z.enum(["normal", "daily"]).optional(),
});

export const Route = createFileRoute("/exam/quiz")({
  validateSearch: (s) => searchSchema.parse(s),
  component: QuizPage,
});

function pickQuestions(n: number, subjects: string, year: string) {
  let pool = QUESTIONS;
  if (year) pool = pool.filter((q) => q.year === year);
  const subj = subjects
    .split(",")
    .map((x) => x.trim())
    .filter(Boolean) as ExamSubject[];
  if (subj.length) pool = pool.filter((q) => subj.includes(q.subject));
  const shuffled = [...pool].sort(() => Math.random() - 0.5);
  const count = Math.min(n || shuffled.length, shuffled.length);
  return shuffled.slice(0, count);
}

const CONFIDENCE_LABEL: Record<ExamConfidence, string> = {
  sure: "مطمئن بودم",
  between: "بین دو گزینه بودم",
  guess: "حدس می‌زنم",
};

function QuizPage() {
  const search = Route.useSearch();
  const n = search.n ?? 10;
  const subjects = search.subjects ?? "";
  const year = search.year ?? "";
  const mode = search.mode ?? "normal";
  const recordExam = useMizan((s) => s.recordExam);
  const attempts = useMizan((s) => s.examAttempts);
  const [set, setSet] = useState<ExamQuestion[] | null>(null);
  const [i, setI] = useState(0);
  const [choice, setChoice] = useState<number | null>(null);
  const [confidence, setConfidence] = useState<ExamConfidence | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [correctCount, setCorrectCount] = useState(0);
  const [wrongCount, setWrongCount] = useState(0);
  const [skippedCount, setSkippedCount] = useState(0);
  const [done, setDone] = useState(false);

  useEffect(() => {
    const next = mode === "daily" ? buildDailyMission(QUESTIONS, attempts) : pickQuestions(n, subjects, year);
    setSet(next);
    setI(0);
    setChoice(null);
    setConfidence(null);
    setRevealed(false);
    setCorrectCount(0);
    setWrongCount(0);
    setSkippedCount(0);
    setDone(false);
    // مأموریت روزانه فقط هنگام ورود دوباره ساخته می‌شود، نه بعد از تک‌تک پاسخ‌ها.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [n, subjects, year, mode]);

  const examScore = useMemo(() => correctCount - wrongCount / 3, [correctCount, wrongCount]);

  if (!set) return <p className="text-sm text-muted">در حال آماده‌سازی پرسش‌ها…</p>;

  if (!set.length) {
    return (
      <main className="space-y-3">
        <h1 className="text-xl font-medium">
          {mode === "daily" ? "مأموریت امروز تمام شده" : "سؤالی در این فیلتر نیست"}
        </h1>
        <p className="text-sm leading-7 text-muted">
          {mode === "daily"
            ? "اگر ۲۰ پاسخ امروز ثبت شده باشد، سهم روزانه‌ات کامل است. فردا ترکیب تازه بر اساس ضعف‌ها ساخته می‌شود."
            : "فیلتر دیگری انتخاب کن."}
        </p>
        <Link to="/exam" className="text-sm text-accent">بازگشت</Link>
      </main>
    );
  }

  const q = set[i]!;

  if (done) {
    const maxScore = set.length;
    const percentage = maxScore ? Math.max(0, (examScore / maxScore) * 100) : 0;
    return (
      <main className="space-y-4">
        <h1 className="text-2xl font-medium">پایان {mode === "daily" ? "مأموریت امروز" : "آزمون"}</h1>
        <div className="grid grid-cols-3 gap-2 text-center text-sm">
          <div className="rounded-lg bg-elevated p-3"><b>{toFaDigits(correctCount)}</b><br/><span className="text-xs text-muted">درست</span></div>
          <div className="rounded-lg bg-elevated p-3"><b>{toFaDigits(wrongCount)}</b><br/><span className="text-xs text-muted">غلط</span></div>
          <div className="rounded-lg bg-elevated p-3"><b>{toFaDigits(skippedCount)}</b><br/><span className="text-xs text-muted">نزده</span></div>
        </div>
        <p className="text-sm text-muted">نمره با منفی یک‌سوم: {toFaDigits(examScore.toFixed(2))}</p>
        <Progress value={percentage} />
        <div className="flex gap-2">
          <Link to="/exam" className="flex h-11 flex-1 items-center justify-center rounded-md bg-accent text-sm text-accent-fg">گزارش دکتری</Link>
          {mode !== "daily" ? (
            <Button variant="outline" className="flex-1" onClick={() => {
              setSet(pickQuestions(n, subjects, year));
              setI(0); setChoice(null); setConfidence(null); setRevealed(false);
              setCorrectCount(0); setWrongCount(0); setSkippedCount(0); setDone(false);
            }}>ست جدید</Button>
          ) : null}
        </div>
      </main>
    );
  }

  function register(pick: number | null) {
    if (revealed) return;
    const conf = confidence ?? "guess";
    const correct = pick !== null && pick === q.answer;
    setChoice(pick);
    setRevealed(true);
    recordExam(q.id, pick, correct, q.subject, conf);
    if (pick === null) setSkippedCount((s) => s + 1);
    else if (correct) setCorrectCount((s) => s + 1);
    else setWrongCount((s) => s + 1);
  }

  return (
    <main className="space-y-4">
      <div className="flex items-center justify-between text-xs text-muted">
        <span>سؤال {toFaDigits(i + 1)} از {toFaDigits(set.length)}</span>
        <span className="tabular-nums">امتیاز: {toFaDigits((correctCount - wrongCount / 3).toFixed(2))}</span>
      </div>
      <Progress value={((i + (revealed ? 1 : 0)) / set.length) * 100} />

      <article className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
        <div className="mb-3 flex flex-wrap gap-1.5">
          <Badge>{SUBJECT_LABEL[q.subject]}</Badge>
          <Badge variant="muted">{q.year}</Badge>
          <Badge variant="muted">{q.topic}</Badge>
          {q.verified ? <Badge>تأییدشده</Badge> : <Badge variant="muted">نیازمند تطبیق منبع</Badge>}
        </div>
        <h1 className="text-base font-medium leading-8">{q.stem}</h1>

        {!revealed ? (
          <div className="mt-4">
            <p className="mb-2 text-xs text-muted">قبل از پاسخ، میزان اطمینانت را ثبت کن:</p>
            <div className="grid grid-cols-3 gap-2">
              {(Object.keys(CONFIDENCE_LABEL) as ExamConfidence[]).map((c) => (
                <button key={c} type="button" onClick={() => setConfidence(c)} className={cn(
                  "rounded-lg px-2 py-2 text-xs shadow-[0_0_0_1px_var(--mizan-line)]",
                  confidence === c && "bg-accent text-accent-fg",
                )}>{CONFIDENCE_LABEL[c]}</button>
              ))}
            </div>
          </div>
        ) : null}

        <ul className="mt-4 space-y-2">
          {q.choices.map((c, idx) => {
            const isPick = choice === idx;
            const isAns = q.answer === idx;
            return (
              <li key={`${q.id}-${idx}`}>
                <button type="button" disabled={revealed || confidence === null} onClick={() => register(idx)} className={cn(
                  "w-full rounded-lg px-3 py-3 text-right text-sm leading-7 shadow-[0_0_0_1px_var(--mizan-line)] disabled:opacity-55",
                  !revealed && "hover:bg-subtle",
                  revealed && isAns && "bg-ok/12 shadow-[0_0_0_1px_var(--mizan-ok)]",
                  revealed && isPick && !isAns && "bg-seal/10 shadow-[0_0_0_1px_var(--mizan-seal)]",
                )}>
                  <span className="ml-2 text-xs text-muted">{["الف", "ب", "ج", "د"][idx]}.</span> {c}
                </button>
              </li>
            );
          })}
        </ul>
        {!revealed ? (
          <Button variant="outline" className="mt-3 w-full" disabled={confidence === null} onClick={() => register(null)}>
            نمی‌دانم — بدون نمره منفی
          </Button>
        ) : null}
      </article>

      {revealed ? (
        <section className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
          <p className={cn("text-sm font-medium", choice === q.answer ? "text-ok" : "text-seal")}>
            {choice === q.answer ? "پاسخ شما درست است" : choice === null ? `پاسخ درست: ${["الف", "ب", "ج", "د"][q.answer]}` : `پاسخ درست: ${["الف", "ب", "ج", "د"][q.answer]}`}
          </p>
          <h2 className="mt-3 text-xs font-medium text-muted">چرا این گزینه صحیح است؟</h2>
          <p className="mt-1 text-sm leading-7">{q.explanation}</p>
          {q.trap ? <><h2 className="mt-3 text-xs font-medium text-muted">دام طراح</h2><p className="mt-1 text-sm leading-7">{q.trap}</p></> : null}
          <p className="mt-3 text-xs text-muted">مستند: {q.articles.join(" · ")}</p>
          {q.source?.questionNo ? <p className="mt-1 text-xs text-muted">شناسنامه: سؤال {toFaDigits(q.source.questionNo)} · دفترچه {q.source.bookletCode ?? "—"}</p> : null}
          <Button className="mt-4 w-full" onClick={() => {
            if (i + 1 >= set.length) { setDone(true); return; }
            setI(i + 1); setChoice(null); setConfidence(null); setRevealed(false);
          }}>{i + 1 >= set.length ? "مشاهده نتیجه" : "سؤال بعد"}</Button>
        </section>
      ) : (
        <p className="text-xs text-muted">ابتدا میزان اطمینان را انتخاب کن؛ سپس پاسخ بده.</p>
      )}
    </main>
  );
}
