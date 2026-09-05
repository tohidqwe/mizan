import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { QUESTIONS } from "@/data";
import { SUBJECT_LABEL, type ExamSubject } from "@/data/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { dailyProgress, subjectAccuracy } from "@/lib/exam-coach";
import { useMizan } from "@/lib/store";
import { toFaDigits } from "@/lib/utils";

export const Route = createFileRoute("/exam/")({ component: ExamHome });

const SUBJECTS: ExamSubject[] = ["civil", "commercial", "fiqh", "pil", "procedure"];

function ExamHome() {
  const picks = useMizan((s) => s.examPicks);
  const attempts = useMizan((s) => s.examAttempts);
  const answered = Object.keys(picks).length;
  const years = Array.from(new Set(QUESTIONS.map((q) => q.year)));
  const progress = dailyProgress(attempts);
  const civilAcc = subjectAccuracy(attempts, "civil");
  const commercialAcc = subjectAccuracy(attempts, "commercial");
  const fiqhAcc = subjectAccuracy(attempts, "fiqh");
  const verifiedCount = QUESTIONS.filter((q) => q.verified).length;

  return (
    <main className="space-y-5">
      <header>
        <h1 className="text-2xl font-medium">دکتری حقوق خصوصی</h1>
        <p className="mt-1 text-sm leading-7 text-muted">
          مربی روزانه بر اساس وضعیت فعلی تو تنظیم شده: مدنی متوسط، تجارت ضعیف و متون فقه ضعیف. تست غلط،
          جواب شانسی و پاسخ مطمئن یکسان حساب نمی‌شوند و در انتخاب تست‌های بعدی اثر دارند.
        </p>
      </header>

      <section className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-base font-medium">مأموریت امروز: ۲۰ تست</h2>
            <p className="mt-1 text-xs leading-6 text-muted">
              هدف فعلی: ۵ مدنی + ۸ تجارت + ۷ متون فقه. با تکمیل بانک معتبر زبان، سهم‌بندی آزمون رسمی نیز جدا اضافه می‌شود.
            </p>
          </div>
          <Badge variant={progress.complete ? "default" : "muted"}>{progress.complete ? "کامل شد" : `${toFaDigits(progress.remaining)} مانده`}</Badge>
        </div>
        <Progress className="mt-3" value={(progress.total / 20) * 100} />
        <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
          <div className="rounded-lg bg-subtle p-2">مدنی<br/><b>{toFaDigits(progress.bySubject.civil)}</b></div>
          <div className="rounded-lg bg-subtle p-2">تجارت<br/><b>{toFaDigits(progress.bySubject.commercial)}</b></div>
          <div className="rounded-lg bg-subtle p-2">فقه<br/><b>{toFaDigits(progress.bySubject.fiqh)}</b></div>
        </div>
        <Link
          to="/exam/quiz"
          search={{ n: 20, subjects: "civil,commercial,fiqh", year: "", mode: "daily" }}
          className="mt-4 flex h-12 w-full items-center justify-center rounded-md bg-accent text-sm font-medium text-accent-fg"
        >
          {progress.complete ? "مرور وضعیت امروز" : "شروع / ادامه مأموریت امروز"}
        </Link>
      </section>

      <section className="space-y-2">
        <h2 className="text-sm font-medium">وضعیت تسلط</h2>
        <div className="grid grid-cols-3 gap-2">
          <AccuracyCard label="مدنی" value={civilAcc} />
          <AccuracyCard label="تجارت" value={commercialAcc} />
          <AccuracyCard label="فقه" value={fiqhAcc} />
        </div>
        <p className="text-xs leading-6 text-muted">
          درصدها فقط از پاسخ‌های ثبت‌شده خودت محاسبه می‌شوند. «نمی‌دانم» در درصد دقت وارد نمی‌شود ولی برای مرور ضعف ثبت می‌شود.
        </p>
      </section>

      <div className="rounded-xl bg-elevated p-4 text-sm shadow-[var(--shadow-card)]">
        <div className="flex items-center justify-between gap-2">
          <span>بانک فعلی: {toFaDigits(QUESTIONS.length)} تست</span>
          <span className="text-xs text-muted">تأیید منبع کامل: {toFaDigits(verifiedCount)}</span>
        </div>
        <p className="mt-2 text-xs leading-6 text-muted">
          برچسب «سنجش ـ تأییدشده» فقط بعد از تطبیق متن سؤال، شماره سؤال و کلید با منبع آزمون فعال می‌شود؛
          official قدیمی به‌تنهایی کافی نیست.
        </p>
      </div>

      <section className="space-y-2">
        <h2 className="text-sm font-medium">شروع سریع</h2>
        <div className="grid gap-2">
          <StartBtn n={10} label="ده تست تصادفی از دروس تخصصی" subjects={["civil", "commercial", "fiqh"]} />
          <StartBtn n={20} label="بیست تست مخلوط" subjects={SUBJECTS} />
        </div>
      </section>

      <section className="space-y-2">
        <h2 className="text-sm font-medium">به تفکیک درس</h2>
        {SUBJECTS.map((s) => {
          const count = QUESTIONS.filter((q) => q.subject === s).length;
          const verified = QUESTIONS.some((q) => q.subject === s && q.verified);
          return (
            <StartRow
              key={s}
              title={SUBJECT_LABEL[s]}
              count={count}
              badge={verified ? "منبع‌سنجی‌شده" : "در حال تکمیل"}
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
              search={{ year: y, n: count, subjects: "", mode: "normal" }}
              className="flex items-center justify-between rounded-lg bg-elevated px-4 py-3 text-sm shadow-[var(--shadow-card)]"
            >
              <span>سال {y}</span>
              <span className="text-xs text-muted">{toFaDigits(count)} سؤال</span>
            </Link>
          );
        })}
      </section>

      <div className="rounded-xl bg-subtle p-4 text-xs leading-6 text-muted">
        پاسخ‌داده‌شده در کل بانک قدیمی: {toFaDigits(answered)}. تاریخچه جدید تلاش‌ها به صورت جدا ذخیره می‌شود و مبنای مربی تطبیقی است.
      </div>
    </main>
  );
}

function AccuracyCard({ label, value }: { label: string; value: number | null }) {
  return (
    <div className="rounded-xl bg-elevated p-3 text-center shadow-[var(--shadow-card)]">
      <div className="text-xs text-muted">{label}</div>
      <div className="mt-1 text-lg font-medium">{value === null ? "—" : `${toFaDigits(value)}٪`}</div>
    </div>
  );
}

function StartBtn({ n, label, subjects }: { n: number; label: string; subjects: ExamSubject[] }) {
  const navigate = useNavigate();
  return (
    <Button
      className="h-12 w-full justify-between"
      onClick={() => navigate({ to: "/exam/quiz", search: { n, subjects: subjects.join(","), year: "", mode: "normal" } })}
    >
      {label}
      <span className="text-xs opacity-80">{toFaDigits(n)}</span>
    </Button>
  );
}

function StartRow({ title, count, badge, subjects }: { title: string; count: number; badge: string; subjects: ExamSubject[] }) {
  return (
    <Link
      to="/exam/quiz"
      search={{ n: count, subjects: subjects.join(","), year: "", mode: "normal" }}
      className="flex items-center justify-between rounded-lg bg-elevated px-4 py-3 shadow-[var(--shadow-card)]"
    >
      <div className="flex items-center gap-2">
        <span className="text-sm">{title}</span>
        <Badge variant={badge === "منبع‌سنجی‌شده" ? "default" : "muted"}>{badge}</Badge>
      </div>
      <span className="text-xs text-muted">{toFaDigits(count)}</span>
    </Link>
  );
}
