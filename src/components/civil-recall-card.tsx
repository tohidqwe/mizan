import { useEffect, useState } from "react";
import type { VerifiedCivilArticle } from "@/data/civil-verified-schema";
import type { ReviewRating } from "@/lib/srs";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn, toFaDigits } from "@/lib/utils";

export function CivilRecallCard({
  article,
  onRate,
}: {
  article: VerifiedCivilArticle;
  onRate: (rating: ReviewRating) => void;
}) {
  const [phase, setPhase] = useState<"recall" | "choice" | "revealed">("recall");
  const [pick, setPick] = useState<0 | 1 | null>(null);

  useEffect(() => {
    setPhase("recall");
    setPick(null);
  }, [article.n, article.suffix]);

  const q = article.twoOptionQuestion;
  const correct = pick === q.answer;

  return (
    <article className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
      <div className="mb-3 flex items-center justify-between gap-3">
        <Badge variant="muted">بازیابی فعال</Badge>
        <span className="text-xs text-muted">ماده {toFaDigits(article.n)}{article.suffix ? ` ${article.suffix}` : ""}</span>
      </div>

      {phase === "recall" ? (
        <div className="space-y-4">
          <h1 className="text-lg font-medium leading-8">{article.recallPrompt}</h1>
          <p className="text-sm leading-7 text-muted">
            پاسخ را قبل از دیدن گزینه‌ها در ذهن یا با صدای آهسته بازیابی کن. هدف «یادآوری» است، نه صرفاً تشخیص گزینه.
          </p>
          <Button className="w-full" onClick={() => setPhase("choice")}>
            پاسخ را بازیابی کردم؛ تست کوتاه
          </Button>
        </div>
      ) : null}

      {phase === "choice" ? (
        <div className="space-y-4">
          <h2 className="text-base font-medium leading-8">{q.stem}</h2>
          <div className="space-y-2">
            {q.choices.map((choice, idx) => (
              <button
                key={`${q.id}-${idx}`}
                type="button"
                onClick={() => {
                  setPick(idx as 0 | 1);
                  setPhase("revealed");
                }}
                className="w-full rounded-lg px-4 py-3 text-right text-sm leading-7 shadow-[0_0_0_1px_var(--mizan-line)] hover:bg-subtle"
              >
                <span className="ml-2 text-xs text-muted">{idx === 0 ? "الف" : "ب"}.</span>
                {choice}
              </button>
            ))}
          </div>
        </div>
      ) : null}

      {phase === "revealed" ? (
        <div className="space-y-4">
          <p className={cn("text-sm font-medium", correct ? "text-ok" : "text-seal")}>
            {correct ? "پاسخ تست درست بود" : `گزینه درست: ${q.answer === 0 ? "الف" : "ب"}`}
          </p>

          <section className="rounded-lg bg-subtle p-3">
            <h2 className="text-xs font-medium text-muted">چرا؟</h2>
            <p className="mt-1 text-sm leading-7">{q.rationale}</p>
          </section>

          <section>
            <h2 className="text-xs font-medium text-muted">متن رسمی قانون</h2>
            <blockquote className="mt-2 border-r-2 border-accent/40 pr-3 text-sm leading-8">
              {article.officialText}
            </blockquote>
          </section>

          {q.doctrineRefs.length > 0 ? (
            <section>
              <h2 className="text-xs font-medium text-muted">مبنای دکترین</h2>
              <ul className="mt-1 space-y-1 text-xs leading-6 text-muted">
                {q.doctrineRefs.map((ref, idx) => (
                  <li key={`${ref.author}-${ref.work}-${idx}`}>
                    {ref.author}، {ref.work}
                    {ref.volume ? `، ج ${ref.volume}` : ""}
                    {ref.page ? `، ص ${ref.page}` : ""}
                    {ref.edition ? `، ${ref.edition}` : ""}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}

          <div className="rounded-lg border border-line p-3 text-xs leading-6 text-muted">
            حالا «کیفیت بازیابی خودت» را ثبت کن؛ درست‌زدن اتفاقی تست دوگزینه‌ای به‌تنهایی مساوی تسلط نیست.
          </div>

          <div className="grid grid-cols-3 gap-2">
            <Button variant="secondary" onClick={() => onRate("again")}>نمی‌دانستم</Button>
            <Button variant="outline" onClick={() => onRate("hard")}>سخت بود</Button>
            <Button onClick={() => onRate("known")}>بلد بودم</Button>
          </div>
        </div>
      ) : null}
    </article>
  );
}
