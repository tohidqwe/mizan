import { createFileRoute, Link } from "@tanstack/react-router";
import { getLaw, ARTICLES_BY_LAW } from "@/data";
import { CATEGORY_LABEL } from "@/data/types";
import { Badge } from "@/components/ui/badge";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { toFaDigits } from "@/lib/utils";

export const Route = createFileRoute("/laws/$lawId")({ component: LawDetail });

function LawDetail() {
  const { lawId } = Route.useParams();
  const law = getLaw(lawId);
  const articles = ARTICLES_BY_LAW[lawId] ?? [];

  if (!law) {
    return (
      <main className="space-y-3">
        <h1 className="text-xl font-medium">قانون یافت نشد</h1>
        <Link to="/laws" className="text-sm text-accent">
          بازگشت به فهرست
        </Link>
      </main>
    );
  }

  return (
    <main className="space-y-5">
      <header className="space-y-2">
        <Badge>{CATEGORY_LABEL[law.category]}</Badge>
        <h1 className="text-2xl font-medium leading-snug">{law.title}</h1>
        <p className="text-sm text-muted">
          مصوب {law.year} · حدود {toFaDigits(law.articleCount)} ماده
        </p>
        <p className="text-sm leading-7">{law.summary}</p>
      </header>

      <section className="rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
        <h2 className="text-sm font-medium">منابع معتبر</h2>
        <ul className="mt-2 list-disc pr-4 text-sm leading-7 text-muted">
          {law.sources.map((s) => (
            <li key={s}>{s}</li>
          ))}
        </ul>
      </section>

      <section>
        <h2 className="mb-2 text-sm font-medium">ساختار</h2>
        <Accordion type="single" collapsible className="rounded-xl bg-elevated px-4 shadow-[var(--shadow-card)]">
          {law.chapters.map((ch) => (
            <AccordionItem key={ch.title} value={ch.title}>
              <AccordionTrigger>{ch.title}</AccordionTrigger>
              <AccordionContent>
                مواد {toFaDigits(ch.from)} تا {toFaDigits(ch.to)}
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </section>

      <section className="space-y-2">
        <h2 className="text-sm font-medium">مواد با تحلیل ({toFaDigits(articles.length)})</h2>
        {articles.length === 0 ? (
          <p className="text-sm leading-7 text-muted">
            متن کامل این قانون در سامانه ملی قوانین در دسترس است. برای مرور روزانه، مسیر مدنی / تجارت /
            آیین دادرسی را دنبال کنید.
          </p>
        ) : (
          <ul className="space-y-2">
            {articles.map((a) => (
              <li key={a.n}>
                <Link
                  to="/read/$lawId/$n"
                  params={{ lawId, n: String(a.n) }}
                  className="block rounded-lg bg-elevated px-4 py-3 shadow-[var(--shadow-card)]"
                >
                  <p className="text-sm font-medium">
                    ماده {toFaDigits(a.n)}
                    {a.heading ? ` — ${a.heading}` : ""}
                  </p>
                  <p className="mt-1 line-clamp-2 text-xs leading-6 text-muted">{a.text}</p>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
