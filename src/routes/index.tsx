import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowLeft, Bell, BookOpen, GraduationCap, Scale } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { ALL_ARTICLES, LAWS, QUESTIONS } from "@/data";
import { TRACK_LAWS, TRACK_META } from "@/data/types";
import { todaysBatch } from "@/lib/notify";
import { useMizan } from "@/lib/store";
import { todayKey, toFaDigits } from "@/lib/utils";

export const Route = createFileRoute("/")({ component: Home });

function Home() {
  const cursors = useMizan((s) => s.cursors);
  const reviewed = useMizan((s) => s.reviewed);
  const examPicks = useMizan((s) => s.examPicks);
  const completedDailyDate = useMizan((s) => s.completedDailyDate);
  const known = Object.values(reviewed).filter((r) => r.mark === "known").length;
  const answered = Object.keys(examPicks).length;
  const doneToday = completedDailyDate === todayKey();

  return (
    <main className="space-y-6">
      <section>
        <p className="text-xs font-medium tracking-wide text-muted">دانشنامه و آزمون</p>
        <h1 className="mt-1 text-3xl font-medium leading-tight">میزان</h1>
        <p className="mt-2 max-w-prose text-sm leading-7 text-muted">
          قوانین ایران با متن رسمی، تحلیل دکترین و صحت‌سنجی؛ به‌علاوه بانک تست دکتری حقوق خصوصی با پاسخ
          تشریحی. هر روز سه ماده از مدنی، تجارت و آیین دادرسی، به ترتیب شماره، تا ملکه ذهن شود.
        </p>
      </section>

      <div className="grid grid-cols-3 gap-2">
        <Stat label="قوانین" value={toFaDigits(LAWS.length)} />
        <Stat label="مواد تحلیل‌شده" value={toFaDigits(ALL_ARTICLES.length)} />
        <Stat label="تست دکتری" value={toFaDigits(QUESTIONS.length)} />
      </div>

      <Card>
        <CardContent className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-medium">مرور امروز</h2>
            {doneToday ? <Badge variant="ok">ثبت شد</Badge> : <Badge variant="muted">در انتظار مرور</Badge>}
          </div>
          <ul className="space-y-2">
            {TRACK_LAWS.map((t) => {
              const batch = todaysBatch(t, cursors[t]);
              return (
                <li key={t} className="flex items-baseline justify-between gap-3 text-sm">
                  <span className="text-muted">{TRACK_META[t].title}</span>
                  <span className="font-medium tabular-nums">
                    مواد {batch.map((a) => toFaDigits(a.n)).join("، ")}
                  </span>
                </li>
              );
            })}
          </ul>
          <Link
            to="/daily"
            className="flex h-11 items-center justify-center gap-2 rounded-md bg-accent text-sm font-medium text-accent-fg"
          >
            شروع مرور روزانه
            <ArrowLeft className="size-4" />
          </Link>
        </CardContent>
      </Card>

      <div className="grid gap-3 sm:grid-cols-2">
        <NavCard
          to="/laws"
          icon={Scale}
          title="قوانین ایران"
          text="اساسی، مدنی، تجارت، کیفری، کار، خانواده، ثبت و ده‌ها قانون خاص با ساختار و منابع."
        />
        <NavCard
          to="/exam"
          icon={GraduationCap}
          title="دکتری حقوق خصوصی"
          text="تست‌های مدنی، تجارت، متون فقه معاملات و حقوق بین‌الملل خصوصی با علت پاسخ."
        />
        <NavCard
          to="/reminders"
          icon={Bell}
          title="یادآوری روزانه"
          text="ساعت دلخواه را بگذارید؛ پوش نوتیفیکیشن مواد و تست را تکرار می‌کند."
        />
        <NavCard
          to="/search"
          icon={BookOpen}
          title="جستجوی مواد"
          text="متن ماده، شماره یا کلیدواژه را بجویید."
        />
      </div>

      <section className="space-y-2">
        <h2 className="text-sm font-medium">پیشرفت شما روی همین دستگاه</h2>
        <div className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
          <div className="mb-2 flex justify-between text-xs text-muted">
            <span>مواد ملکه‌شده</span>
            <span className="tabular-nums">{toFaDigits(known)}</span>
          </div>
          <Progress value={Math.min(100, known * 4)} />
          <div className="mt-4 mb-2 flex justify-between text-xs text-muted">
            <span>تست پاسخ‌داده‌شده</span>
            <span className="tabular-nums">
              {toFaDigits(answered)} از {toFaDigits(QUESTIONS.length)}
            </span>
          </div>
          <Progress value={(answered / QUESTIONS.length) * 100} />
        </div>
      </section>

      <p className="text-xs leading-6 text-faint">
        منابع متن: سامانه ملی قوانین و مقررات، مرکز پژوهش‌های مجلس، روزنامه رسمی. تحلیل‌ها بر پایه دکترین
        معتبر (کاتوزیان، امامی، صفایی، شهیدی، شمس، اسکینی، کاویانی) و آرای وحدت‌رویه است. این برنامه مشاوره
        وکالت نیست.
      </p>
    </main>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-elevated px-3 py-3 text-center shadow-[var(--shadow-card)]">
      <div className="text-lg font-medium tabular-nums">{value}</div>
      <div className="text-xs text-muted">{label}</div>
    </div>
  );
}

function NavCard({
  to,
  icon: Icon,
  title,
  text,
}: {
  to: string;
  icon: typeof Scale;
  title: string;
  text: string;
}) {
  return (
    <Link to={to} className="block rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
      <Icon className="size-5 text-accent" strokeWidth={1.6} />
      <h3 className="mt-3 text-sm font-medium">{title}</h3>
      <p className="mt-1 text-xs leading-6 text-muted">{text}</p>
    </Link>
  );
}
