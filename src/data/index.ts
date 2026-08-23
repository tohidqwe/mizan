import { CIVIL_ARTICLES } from "./civil";
import { COMMERCE_ARTICLES } from "./commerce";
import { PROCEDURE_ARTICLES } from "./procedure";
import { EXTRA_ARTICLES } from "./extra-articles";
import { LAWS } from "./catalog";
import type { Article, TrackLawId } from "./types";

export * from "./types";
export { LAWS, getLaw, LAW_BY_ID } from "./catalog";
export { QUESTIONS } from "./questions";

export const ALL_ARTICLES: Article[] = [
  ...CIVIL_ARTICLES,
  ...COMMERCE_ARTICLES,
  ...PROCEDURE_ARTICLES,
  ...EXTRA_ARTICLES,
];

export const ARTICLES_BY_LAW: Record<string, Article[]> = ALL_ARTICLES.reduce(
  (acc, a) => {
    (acc[a.lawId] ??= []).push(a);
    return acc;
  },
  {} as Record<string, Article[]>,
);

for (const list of Object.values(ARTICLES_BY_LAW)) {
  list.sort((a, b) => a.n - b.n);
}

export function getArticle(lawId: string, n: number): Article | undefined {
  return ARTICLES_BY_LAW[lawId]?.find((a) => a.n === n);
}

export function articleKey(lawId: string, n: number): string {
  return `${lawId}:${n}`;
}

export const TRACK_IDS: Record<TrackLawId, string> = {
  civil: "civil",
  commerce: "commerce",
  procedure: "procedure",
};

export function trackArticles(track: TrackLawId): Article[] {
  return ARTICLES_BY_LAW[TRACK_IDS[track]] ?? [];
}

export function searchArticles(query: string): Article[] {
  const q = query.trim();
  if (q.length < 2) return [];
  return ALL_ARTICLES.filter(
    (a) =>
      a.text.includes(q) ||
      a.analysis.includes(q) ||
      a.heading?.includes(q) ||
      String(a.n) === q ||
      a.tags.some((t) => t.includes(q)),
  ).slice(0, 40);
}

export function searchLaws(query: string) {
  const q = query.trim();
  if (!q) return LAWS;
  return LAWS.filter(
    (l) => l.title.includes(q) || l.shortTitle.includes(q) || l.summary.includes(q) || l.year.includes(q),
  );
}
