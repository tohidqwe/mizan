import { createFileRoute, Link } from "@tanstack/react-router";
import { getArticle, getLaw } from "@/data";
import { ArticleBlock } from "@/components/article-block";

export const Route = createFileRoute("/read/$lawId/$n")({ component: ReadArticle });

function ReadArticle() {
  const { lawId, n } = Route.useParams();
  const article = getArticle(lawId, Number(n));
  const law = getLaw(lawId);

  if (!article) {
    return (
      <main className="space-y-3">
        <h1 className="text-xl font-medium">ماده در بانک تحلیل نیست</h1>
        <p className="text-sm leading-7 text-muted">
          شماره ماده در فهرست روزانه یا قوانین دارای شرح موجود نیست. از مسیر روزانه به ترتیب شماره پیش
          بروید.
        </p>
        <Link to="/laws/$lawId" params={{ lawId }} className="text-sm text-accent">
          بازگشت به {law?.shortTitle ?? "قانون"}
        </Link>
      </main>
    );
  }

  return (
    <main className="space-y-4">
      <Link to="/laws/$lawId" params={{ lawId }} className="text-xs text-muted">
        {law?.title}
      </Link>
      <ArticleBlock article={article} />
    </main>
  );
}
