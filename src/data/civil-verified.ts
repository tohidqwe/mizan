import type { VerifiedCivilArticle } from "./civil-verified-schema";

/**
 * Production Civil Code content is intentionally NOT backfilled from model memory
 * or the legacy civil.ts file. The official-source pipeline must replace this
 * array with records that pass auditCivilContent(). Until then, the strict review
 * UI stays content-gated instead of teaching unverified statutory wording.
 */
export const VERIFIED_CIVIL_ARTICLES: VerifiedCivilArticle[] = [];
