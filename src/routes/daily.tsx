import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { CivilRecallCard } from "@/components/civil-recall-card";
import { Progress } from "@/components/ui/progress";
import { VERIFIED_CIVIL_ARTICLES } from "@/data/civil-verified";
import { auditCivilContent } from "@/data/civil-verified-schema";
import {
  masteryOf,
  sortDueStates,
  targetNewCards,
  type ReviewRating,
} from "@/lib/srs";
import { useMizan } from "@/lib/store";
import { toFaDigits } from "@/lib/utils";

export const Route = createFileRoute("/daily")({ component: DailyPage });

const SESSION_CHUNK = 20;

function elapsedDays(start: string | null): number {
  if (!start) return 0;
  const [y, m, d] = start.split("-").map(Number);
  const from = new Date(y, (m || 1) - 1, d || 1, 12, 0, 0, 0);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 12, 0, 0, 0);
  return Math.max(0, Math.floor((today.getTime() - from.getTime()) / 86_400_000));
}

function DailyPage() {
  const civilReviews = useMizan((s) => s.civilReviews);
  const planStart = useMizan((s) => s.civilPlanStartDate);
  const startCivilPlan = useMizan((s) => s.startCivilPlan);
  const reviewCivilArticle = useMizan((s) => s.reviewCivilArticle);

  const contentAudit = useMemo(() => auditCivilContent(VERIFIED_CIVIL_ARTICLES), []);
  const byNumber = useMemo(
    () => new Map(VERIFIED_CIVIL_ARTICLES.filter((a) => !a.suffix).map((a) => [a.n, a])),
    [],
  );

  const dueStates = useMemo(() => sortDueStates(Object.values(civilReviews)), [civilReviews]);
  const dueCount = dueStates.length;
  const unseen = useMemo(
    () => VERIFIED_CIVIL_ARTICLES.filter((a) => !a.suffix && !civilReviews[String(a.n)]).sort((a, b) => a.n - b.n),
    [civilReviews],
  );
  const days = elapsedDays(planStart);
  const newToday = targetNewCards({ unseenBaseArticles: unseen.length, elapsedPlanDays: days, dueCount });

  const [queue, setQueue] = useState<number[]>([]);
  const [answered, setAnswered] = useState(0);

  useEffect(() => {
    if (!contentAudit.ok || queue.length > 0) return;
    if (dueCount > 0) {
      setQueue(dueStates.slice(0, SESSION_CHUNK).map((s) => s.articleNo));
      return;
    }
    if (newToday > 0) {
      setQueue(unseen.slice(0, Math.min(SESSION_CHUNK, newToday)).map((a) => a.n));
    }
  }, [contentAudit.ok, dueCount, dueStates, newToday, queue.length, unseen]);

  if (!contentAudit.ok) {
    return (
      <main className="space-y-4">
        <header>
          <h1 className="text-2xl font-medium">حافظ قانون مدنی</h1>
          <p className="mt-1 text-sm leading-7 text-muted">دروازه صحت محتوای قانونی بسته است.</p>
        </header>
        <section className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
          <p className="text-sm leading-7">
            برنامه عمداً متن موجود در نسخه قدیمی پروژه را به عنوان «متن رسمی» وارد جعبه لایتنر نمی‌کند. بانک جدید فقط پس از
            تطبیق مواد ۱ تا ۱۳۳۵ با منابع رسمی فعال می‌شود.
          </p>
          <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
            <div className="rounded-lg bg-subtle p-3">
              <p className="text-xs text-muted">رکورد پایه موجود</p>
              <p className="mt-1 text-lg font-medium">{toFaDigits(contentAudit.baseArticleCount)} / ۱۳۳۵</p>
            </div>
            <div className="rounded-lg bg-subtle p-3">
              <p className="text-xs text-muted">تأییدشده</p>
              <p className="mt-1 text-lg font-medium">{toFaDigits(contentAudit.verifiedBaseArticleCount)} / ۱۳۳۵</p>
            </div>
          </div>
          <p className="mt-4 text-xs leading-6 text-muted">
            این توقف یک خطای برنامه نیست؛ سیاست ضدجعل محتواست. تا وقتی Gate پاس نشود، متن مشکوک برای حفظ‌کردن نمایش داده نمی‌شود.
          </p>
        </section>
      </main>
    );
  }

  const activeNo = queue[0];
  const active = activeNo ? byNumber.get(activeNo) : undefined;
  const totalForProgress = answered + queue.length;
  const reviewedCount = Object.keys(civilReviews).length;
  const masteredCount = Object.values(civilReviews).filter((s) => masteryOf(s) === "mastered").length;

  function rate(rating: ReviewRating) {
    if (!activeNo) return;
    startCivilPlan();
    const result = reviewCivilArticle(activeNo, rating);
    const rest = queue.slice(1);
    if (result.sameDayRequeueAfter !== null) {
      const at = Math.min(result.sameDayRequeueAfter, rest.length);
      rest.splice(at, 0, activeNo);
    }
    setAnswered((x) => x + 1);
    setQueue(rest);
  }

  if (!active) {
    return (
      <main className="space-y-4">
        <header>
          <h1 className="text-2xl font-medium">امروز تمام شد</h1>
          <p className="mt-1 text-sm leading-7 text-muted">
            مرور سررسیدشده‌ای باقی نمانده و سهم مواد جدید امروز نیز انجام شده است.
          </p>
        </header>
        <section className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
          <p className="text-sm">مواد وارد چرخه: {toFaDigits(reviewedCount)} از ۱۳۳۵</p>
          <p className="mt-2 text-sm">تسلط بلندمدت: {toFaDigits(masteredCount)}</p>
        </section>
      </main>
    );
  }

  return (
    <main className="space-y-4">
      <header>
        <div className="flex items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl font-medium">مرور امروز</h1>
            <p className="mt-1 text-xs text-muted">
              {dueCount > 0
                ? `${toFaDigits(dueCount)} مرور سررسیدشده — ماده جدید قفل است`
                : `${toFaDigits(newToday)} ماده جدید برای برنامه ۱۴۰روزه`}
            </p>
          </div>
          <span className="text-xs text-muted">روز {toFaDigits(Math.min(days + 1, 140))} / ۱۴۰</span>
        </div>
        <Progress className="mt-3" value={totalForProgress ? (answered / totalForProgress) * 100 : 0} />
        <p className="mt-2 text-xs text-muted">
          انجام‌شده در این جلسه: {toFaDigits(answered)} · باقی‌مانده جلسه: {toFaDigits(queue.length)}
        </p>
      </header>

      <CivilRecallCard article={active} onRate={rate} />

      <p className="text-center text-xs leading-6 text-muted">
        اصل ثابت: مرورهای سررسیدشده اول؛ ماده جدید بعد. مشاهده آزاد مواد خارج از این صفحه، مرور موفق ثبت نمی‌کند.
      </p>
    </main>
  );
}
