import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { QUESTIONS } from "@/data";
import { SUBJECT_LABEL, type ExamSubject } from "@/data/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useMizan } from "@/lib/store";
import { toFaDigits } from "@/lib/utils";

export const Route = createFileRoute("/exam/")({ component: ExamHome });

const SUBJECTS: ExamSubject[] = ["civil", "commercial", "fiqh", "pil", "procedure"];

function ExamHome() {
  const picks = useMizan((s) => s.examPicks);
  const answered = Object.keys(picks).length;
  const years = Array.from(new Set(QUESTIONS.map((q) => q.year)));

  return (
    <main className="space-y-5">
      <header>
        <h1 className="text-2xl font-medium">دکتری حقوق خصوصی</h1>
        <p className="mt-1 text-sm leading-7 text-muted">
          مواد امتحانی سازمان سنجش برای این رشته عمدتاً متون فقه معاملات، حقوق مدنی و حقوق تجارت است.
          آیین دادرسی مدنی و تعارض قوانین به‌عنوان مکمل (مصاحبه و آزمون‌های دانشگاهی) آمده‌اند. پس از هر
          تست، گزینه درست و علت آن فوراً نشان داده می‌شود.
        </p>
      </header>

      <div className="rounded-xl bg-elevated p-4 text-sm shadow-[var(--shadow-card)]">
        پاسخ‌داده‌شده:{" "}
        <span className="tabular-nums">
          {toFaDigits(answered)} / {toFaDigits(QUESTIONS.length)}
        </span>
      </div>

      <section className="space-y-2">
        <h2 className="text-sm font-medium">شروع سریع</h2>
        <div className="grid gap-2">
          <StartBtn n={10} label="ده تست تصادفی از دروس رسمی" subjects={["civil", "commercial", "fiqh"]} />
          <StartBtn n={20} label="بیست تست مخلوط" subjects={SUBJECTS} />
        </div>
      </section>

      <section className="space-y-2">
        <h2 className="text-sm font-medium">به تفکیک درس</h2>
        {SUBJECTS.map((s) => {
          const count = QUESTIONS.filter((q) => q.subject === s).length;
          const official = QUESTIONS.some((q) => q.subject === s && q.official);
          return (
            <StartRow
              key={s}
              title={SUBJECT_LABEL[s]}
              count={count}
              badge={official ? "سنجش" : "مکمل"}
              subjects={[s]}
            />
          );
        })}
      </section>

      <section className="space-y-2">
        <h2 className="text-sm font-medium">به تفکیک سال</h2>
        {years.map((y) => {
          const count = QUESTIONS.filter((q) => q.year === y).length;
          return (
            <Link
              key={y}
              to="/exam/quiz"
              search={{ year: y, n: count, subjects: "" }}
              className="flex items-center justify-between rounded-lg bg-elevated px-4 py-3 text-sm shadow-[var(--shadow-card)]"
            >
              <span>سال {y}</span>
              <span className="text-xs text-muted">{toFaDigits(count)} سؤال</span>
            </Link>
          );
        })}
      </section>
    </main>
  );
}

function StartBtn({ n, label, subjects }: { n: number; label: string; subjects: ExamSubject[] }) {
  const navigate = useNavigate();
  return (
    <Button
      className="h-12 w-full justify-between"
      onClick={() =>
        navigate({
          to: "/exam/quiz",
          search: { n, subjects: subjects.join(","), year: "" },
        })
      }
    >
      {label}
      <span className="text-xs opacity-80">{toFaDigits(n)}</span>
    </Button>
  );
}

function StartRow({
  title,
  count,
  badge,
  subjects,
}: {
  title: string;
  count: number;
  badge: string;
  subjects: ExamSubject[];
}) {
  return (
    <Link
      to="/exam/quiz"
      search={{ n: count, subjects: subjects.join(","), year: "" }}
      className="flex items-center justify-between rounded-lg bg-elevated px-4 py-3 shadow-[var(--shadow-card)]"
    >
      <div className="flex items-center gap-2">
        <span className="text-sm">{title}</span>
        <Badge variant={badge === "سنجش" ? "default" : "muted"}>{badge}</Badge>
      </div>
      <span className="text-xs text-muted">{toFaDigits(count)}</span>
    </Link>
  );
}
