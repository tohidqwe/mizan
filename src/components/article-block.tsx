import { Bookmark, Check, RotateCcw } from "lucide-react";
import { Link } from "@tanstack/react-router";
import type { Article } from "@/data/types";
import { LAW_BY_ID, articleKey, getArticle } from "@/data";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useMizan } from "@/lib/store";
import { cn, toFaDigits } from "@/lib/utils";

export function ArticleBlock({
  article,
  compact = false,
}: {
  article: Article;
  compact?: boolean;
}) {
  const key = articleKey(article.lawId, article.n);
  const law = LAW_BY_ID[article.lawId];
  const reviewed = useMizan((s) => s.reviewed[key]);
  const bookmarked = useMizan((s) => s.bookmarks.includes(key));
  const markArticle = useMizan((s) => s.markArticle);
  const toggleBookmark = useMizan((s) => s.toggleBookmark);

  return (
    <article className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs text-muted">
            {law?.shortTitle ?? article.lawId} · ماده {toFaDigits(article.n)}
          </p>
          {article.heading ? (
            <h2 className="mt-1 text-base font-medium leading-snug">{article.heading}</h2>
          ) : null}
        </div>
        <button
          type="button"
          aria-label="نشان‌کردن"
          onClick={() => toggleBookmark(key)}
          className={cn(
            "flex size-10 items-center justify-center rounded-md",
            bookmarked ? "text-accent" : "text-faint",
          )}
        >
          <Bookmark className="size-4" fill={bookmarked ? "currentColor" : "none"} />
        </button>
      </div>

      <blockquote className="mt-3 border-r-2 border-accent/40 pr-3 text-sm leading-8">
        {article.text}
      </blockquote>

      {!compact ? (
        <div className="mt-4 space-y-3">
          <section>
            <h3 className="text-xs font-medium text-muted">تحلیل و بررسی</h3>
            <p className="mt-1 text-sm leading-7 text-fg">{article.analysis}</p>
          </section>
          <section>
            <h3 className="text-xs font-medium text-muted">دکترین و صحت‌سنجی</h3>
            <p className="mt-1 text-sm leading-7 text-muted">{article.doctrine}</p>
          </section>
          <div className="flex flex-wrap gap-1.5">
            {article.sources.map((s) => (
              <Badge key={s} variant="muted">
                {s}
              </Badge>
            ))}
          </div>
        </div>
      ) : (
        <p className="mt-3 line-clamp-4 text-sm leading-7 text-muted">{article.analysis}</p>
      )}

      {article.related.length > 0 && !compact ? (
        <p className="mt-3 text-xs text-muted">
          مرتبط:{" "}
          {article.related.map((r, i) => {
            const [lid, num] = r.split(":");
            const exists = lid && num ? getArticle(lid, Number(num)) : undefined;
            if (!exists || !lid || !num) {
              return (
                <span key={r}>
                  {i > 0 ? "، " : null}
                  {r}
                </span>
              );
            }
            return (
              <span key={r}>
                {i > 0 ? "، " : null}
                <Link to="/read/$lawId/$n" params={{ lawId: lid, n: num }} className="text-accent">
                  ماده {toFaDigits(num)}
                </Link>
              </span>
            );
          })}
        </p>
      ) : null}

      <div className="mt-4 flex gap-2">
        <Button
          size="sm"
          variant={reviewed?.mark === "known" ? "default" : "outline"}
          onClick={() => markArticle(key, "known")}
        >
          <Check className="size-3.5" />
          ملکه شد
        </Button>
        <Button
          size="sm"
          variant={reviewed?.mark === "again" ? "secondary" : "ghost"}
          onClick={() => markArticle(key, "again")}
        >
          <RotateCcw className="size-3.5" />
          تکرار شود
        </Button>
        {reviewed ? (
          <span className="ms-auto self-center text-xs text-faint">
            مرور {toFaDigits(reviewed.count)} بار
          </span>
        ) : null}
      </div>
    </article>
  );
}
