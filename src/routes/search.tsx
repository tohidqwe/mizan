import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { searchArticles, searchLaws } from "@/data";
import { Input } from "@/components/ui/input";
import { toFaDigits } from "@/lib/utils";

export const Route = createFileRoute("/search")({ component: SearchPage });

function SearchPage() {
  const [q, setQ] = useState("");
  const laws = useMemo(() => (q.trim() ? searchLaws(q) : []), [q]);
  const articles = useMemo(() => searchArticles(q), [q]);

  return (
    <main className="space-y-4">
      <h1 className="text-2xl font-medium">جستجو</h1>
      <Input
        autoFocus
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="شماره ماده، عبارت قانونی یا نام قانون"
        aria-label="عبارت جستجو"
      />
      {q.trim().length < 2 ? (
        <p className="text-sm text-muted">دست‌کم دو نویسه بنویسید.</p>
      ) : (
        <>
          <section className="space-y-2">
            <h2 className="text-sm font-medium">قوانین</h2>
            {laws.length === 0 ? (
              <p className="text-sm text-muted">موردی نیست.</p>
            ) : (
              laws.slice(0, 8).map((l) => (
                <Link
                  key={l.id}
                  to="/laws/$lawId"
                  params={{ lawId: l.id }}
                  className="block rounded-lg bg-elevated px-4 py-3 text-sm shadow-[var(--shadow-card)]"
                >
                  {l.title}
                </Link>
              ))
            )}
          </section>
          <section className="space-y-2">
            <h2 className="text-sm font-medium">مواد</h2>
            {articles.map((a) => (
              <Link
                key={`${a.lawId}-${a.n}`}
                to="/read/$lawId/$n"
                params={{ lawId: a.lawId, n: String(a.n) }}
                className="block rounded-lg bg-elevated px-4 py-3 shadow-[var(--shadow-card)]"
              >
                <p className="text-sm font-medium">
                  ماده {toFaDigits(a.n)} — {a.heading ?? a.lawId}
                </p>
                <p className="mt-1 line-clamp-2 text-xs leading-6 text-muted">{a.text}</p>
              </Link>
            ))}
          </section>
        </>
      )}
    </main>
  );
}
