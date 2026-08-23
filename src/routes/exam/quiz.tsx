import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { QUESTIONS } from "@/data";
import { SUBJECT_LABEL, type ExamQuestion, type ExamSubject } from "@/data/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { useMizan } from "@/lib/store";
import { cn, toFaDigits } from "@/lib/utils";
import { z } from "zod";

const searchSchema = z.object({
  n: z.coerce.number().optional(),
  subjects: z.string().optional(),
  year: z.string().optional(),
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

function QuizPage() {
  const search = Route.useSearch();
  const n = search.n ?? 10;
  const subjects = search.subjects ?? "";
  const year = search.year ?? "";
  const recordExam = useMizan((s) => s.recordExam);
  const [set, setSet] = useState<ExamQuestion[] | null>(null);
  const [i, setI] = useState(0);
  const [choice, setChoice] = useState<number | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [score, setScore] = useState(0);
  const [done, setDone] = useState(false);

  useEffect(() => {
    setSet(pickQuestions(n, subjects, year));
    setI(0);
    setChoice(null);
    setRevealed(false);
    setScore(0);
    setDone(false);
  }, [n, subjects, year]);

  if (!set) {
    return <p className="text-sm text-muted">در حال آماده‌سازی پرسش‌ها…</p>;
  }

  if (!set.length) {
    return (
      <main className="space-y-3">
        <h1 className="text-xl font-medium">سؤالی در این فیلتر نیست</h1>
        <Link to="/exam" className="text-sm text-accent">
          بازگشت
        </Link>
      </main>
    );
  }

  const q = set[i]!;

  if (done) {
    return (
      <main className="space-y-4">
        <h1 className="text-2xl font-medium">پایان آزمون</h1>
        <p className="text-sm text-muted">
          پاسخ درست: {toFaDigits(score)} از {toFaDigits(set.length)}
        </p>
        <Progress value={(score / set.length) * 100} />
        <div className="flex gap-2">
          <Link
            to="/exam"
            className="flex h-11 flex-1 items-center justify-center rounded-md bg-accent text-sm text-accent-fg"
          >
            بانک سؤال
          </Link>
          <Button
            variant="outline"
            className="flex-1"
            onClick={() => {
              setSet(pickQuestions(n, subjects, year));
              setI(0);
              setChoice(null);
              setRevealed(false);
              setScore(0);
              setDone(false);
            }}
          >
            ست جدید
          </Button>
        </div>
      </main>
    );
  }

  function submit(pick: number) {
    if (revealed) return;
    setChoice(pick);
    setRevealed(true);
    recordExam(q.id, pick);
    if (pick === q.answer) setScore((s) => s + 1);
  }

  return (
    <main className="space-y-4">
      <div className="flex items-center justify-between text-xs text-muted">
        <span>
          سؤال {toFaDigits(i + 1)} از {toFaDigits(set.length)}
        </span>
        <span className="tabular-nums">درست: {toFaDigits(score)}</span>
      </div>
      <Progress value={((i + (revealed ? 1 : 0)) / set.length) * 100} />

      <article className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
        <div className="mb-3 flex flex-wrap gap-1.5">
          <Badge>{SUBJECT_LABEL[q.subject]}</Badge>
          <Badge variant="muted">{q.year}</Badge>
          <Badge variant="muted">{q.topic}</Badge>
        </div>
        <h1 className="text-base font-medium leading-8">{q.stem}</h1>
        <ul className="mt-4 space-y-2">
          {q.choices.map((c, idx) => {
            const isPick = choice === idx;
            const isAns = q.answer === idx;
            const show = revealed;
            return (
              <li key={`${q.id}-${idx}`}>
                <button
                  type="button"
                  disabled={revealed}
                  onClick={() => submit(idx)}
                  className={cn(
                    "w-full rounded-lg px-3 py-3 text-right text-sm leading-7 shadow-[0_0_0_1px_var(--mizan-line)]",
                    !show && "hover:bg-subtle",
                    show && isAns && "bg-ok/12 shadow-[0_0_0_1px_var(--mizan-ok)]",
                    show && isPick && !isAns && "bg-seal/10 shadow-[0_0_0_1px_var(--mizan-seal)]",
                  )}
                >
                  <span className="ml-2 text-xs text-muted">{["الف", "ب", "ج", "د"][idx]}.</span>
                  {" "}
                  {c}
                </button>
              </li>
            );
          })}
        </ul>
      </article>

      {revealed ? (
        <section className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
          <p className={cn("text-sm font-medium", choice === q.answer ? "text-ok" : "text-seal")}>
            {choice === q.answer
              ? "پاسخ شما درست است"
              : `پاسخ درست: ${["الف", "ب", "ج", "د"][q.answer]}`}
          </p>
          <h2 className="mt-3 text-xs font-medium text-muted">علت صحت گزینه</h2>
          <p className="mt-1 text-sm leading-7">{q.explanation}</p>
          <p className="mt-3 text-xs text-muted">مستند: {q.articles.join(" · ")}</p>
          <Button
            className="mt-4 w-full"
            onClick={() => {
              if (i + 1 >= set.length) {
                setDone(true);
                return;
              }
              setI(i + 1);
              setChoice(null);
              setRevealed(false);
            }}
          >
            {i + 1 >= set.length ? "مشاهده نتیجه" : "سؤال بعد"}
          </Button>
        </section>
      ) : (
        <p className="text-xs text-muted">یک گزینه را بزنید تا پاسخ و علت نمایش داده شود.</p>
      )}
    </main>
  );
}
