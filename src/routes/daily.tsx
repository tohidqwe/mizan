import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { TRACK_LAWS, TRACK_META } from "@/data/types";
import { ArticleBlock } from "@/components/article-block";
import { Button } from "@/components/ui/button";
import { todaysBatch } from "@/lib/notify";
import { useMizan } from "@/lib/store";
import { todayKey, toFaDigits } from "@/lib/utils";

export const Route = createFileRoute("/daily")({ component: DailyPage });

function DailyPage() {
  const cursors = useMizan((s) => s.cursors);
  const completeToday = useMizan((s) => s.completeToday);
  const completed = useMizan((s) => s.completedDailyDate);
  const [tab, setTab] = useState<(typeof TRACK_LAWS)[number]>("civil");
  const done = completed === todayKey();
  const batch = todaysBatch(tab, cursors[tab]);

  return (
    <main className="space-y-4">
      <header>
        <h1 className="text-2xl font-medium">سه ماده امروز</h1>
        <p className="mt-1 text-sm leading-7 text-muted">
          هر روز سه ماده متوالی از قانون مدنی، قانون تجارت و آیین دادرسی مدنی. پس از مرور، «روز تمام شد» را
          بزنید تا فردا سه ماده بعدی بیاید. اگر هنوز ملکه نشده، «تکرار شود» را بزنید.
        </p>
      </header>

      <div className="grid grid-cols-3 gap-1 rounded-lg bg-subtle p-1">
        {TRACK_LAWS.map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTab(t)}
            className={`h-10 rounded-md text-xs font-medium ${
              tab === t ? "bg-elevated text-fg shadow-[var(--shadow-card)]" : "text-muted"
            }`}
          >
            {TRACK_META[t].title.replace("قانون ", "").replace("آیین دادرسی مدنی", "آ.د.م")}
          </button>
        ))}
      </div>

      <p className="text-xs text-muted">
        دسته فعلی از ماده {toFaDigits(batch[0]?.n ?? 0)} — پس از تکمیل روز، اشاره {toFaDigits(cursors[tab] + 3)}{" "}
        می‌رود جلو.
      </p>

      <div className="space-y-3">
        {batch.map((a) => (
          <ArticleBlock key={`${a.lawId}-${a.n}`} article={a} />
        ))}
      </div>

      <Button className="w-full" disabled={done} onClick={completeToday}>
        {done ? "مرور امروز ثبت شد" : "روز تمام شد؛ فردا سه ماده بعدی"}
      </Button>
    </main>
  );
}
