import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { LAWS } from "@/data";
import { CATEGORY_LABEL, type LawCategory } from "@/data/types";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { toFaDigits } from "@/lib/utils";

export const Route = createFileRoute("/laws/")({ component: LawsPage });

const CATS = ["all", ...Object.keys(CATEGORY_LABEL)] as const;

function LawsPage() {
  const [q, setQ] = useState("");
  const [cat, setCat] = useState<string>("all");
  const list = useMemo(() => {
    return LAWS.filter((l) => {
      if (cat !== "all" && l.category !== cat) return false;
      if (!q.trim()) return true;
      return l.title.includes(q) || l.shortTitle.includes(q) || l.summary.includes(q);
    });
  }, [q, cat]);

  return (
    <main className="space-y-4">
      <header>
        <h1 className="text-2xl font-medium">قوانین ایران</h1>
        <p className="mt-1 text-sm leading-7 text-muted">
          فهرست شاخه‌های اصلی نظام حقوقی ایران با ساختار مواد و منابع معتبر. روی هر قانون بزنید تا مواد
          تحلیل‌شده را بخوانید.
        </p>
      </header>
      <Input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="جستجوی عنوان قانون…"
        aria-label="جستجوی قانون"
      />
      <div className="flex gap-1.5 overflow-x-auto pb-1">
        {CATS.map((c) => (
          <button
            key={c}
            type="button"
            onClick={() => setCat(c)}
            className={`h-9 shrink-0 rounded-full px-3 text-xs ${
              cat === c ? "bg-accent text-accent-fg" : "bg-subtle text-muted"
            }`}
          >
            {c === "all" ? "همه" : CATEGORY_LABEL[c as LawCategory]}
          </button>
        ))}
      </div>
      <ul className="space-y-2">
        {list.map((law) => (
          <li key={law.id}>
            <Link
              to="/laws/$lawId"
              params={{ lawId: law.id }}
              className="block rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]"
            >
              <div className="flex items-start justify-between gap-3">
                <h2 className="text-sm font-medium leading-snug">{law.title}</h2>
                <Badge variant="muted">{CATEGORY_LABEL[law.category]}</Badge>
              </div>
              <p className="mt-1 text-xs text-muted">
                مصوب {law.year} · {toFaDigits(law.articleCount)} ماده
              </p>
              <p className="mt-2 line-clamp-2 text-xs leading-6 text-muted">{law.summary}</p>
            </Link>
          </li>
        ))}
      </ul>
    </main>
  );
}
