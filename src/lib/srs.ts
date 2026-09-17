export type ReviewRating = "again" | "hard" | "known";

export type ReviewState = {
  articleNo: number;
  box: 1 | 2 | 3 | 4 | 5 | 6;
  maintenanceStep: 0 | 1 | 2 | 3;
  firstSeenDate: string;
  lastReviewDate: string;
  nextReviewDate: string;
  reviewCount: number;
  correctCount: number;
  incorrectCount: number;
  hardCount: number;
  lastRating: ReviewRating;
};

export type ScheduleResult = ReviewState & {
  /** Reinsert in the active session after this many other cards. */
  sameDayRequeueAfter: number | null;
};

export const LEITNER_INTERVAL_DAYS = [1, 2, 4, 8, 16, 32] as const;
export const MAINTENANCE_INTERVAL_DAYS = [60, 90, 120] as const;

export const EXAM_PLAN_DAYS = 140;
export const CONSOLIDATION_DAYS = 28;
export const ACQUISITION_DAYS = EXAM_PLAN_DAYS - CONSOLIDATION_DAYS;
export const CIVIL_BASE_ARTICLE_COUNT = 1335;

function dateKey(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function parseDateKey(value: string): Date {
  const [y, m, d] = value.split("-").map(Number);
  return new Date(y, (m || 1) - 1, d || 1, 12, 0, 0, 0);
}

export function addDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

function normalInterval(state: Pick<ReviewState, "box" | "maintenanceStep">): number {
  if (state.box < 6) return LEITNER_INTERVAL_DAYS[state.box - 1];
  if (state.maintenanceStep > 0) {
    return MAINTENANCE_INTERVAL_DAYS[Math.min(state.maintenanceStep - 1, 2)];
  }
  return LEITNER_INTERVAL_DAYS[5];
}

export function initialReviewState(articleNo: number, now = new Date()): ReviewState {
  const today = dateKey(now);
  return {
    articleNo,
    box: 1,
    maintenanceStep: 0,
    firstSeenDate: today,
    lastReviewDate: today,
    nextReviewDate: today,
    reviewCount: 0,
    correctCount: 0,
    incorrectCount: 0,
    hardCount: 0,
    lastRating: "hard",
  };
}

/**
 * Zero-config scheduler.
 *
 * - AGAIN: reset to box 1, re-show after 7 cards, next long-term review tomorrow.
 * - HARD: keep the same box, shorten its interval, re-show once after 12 cards.
 * - KNOWN: advance one box. After box 6 enter 60/90/120-day maintenance.
 *
 * Being overdue is never counted as a failure by itself; only the learner's answer
 * changes the memory state.
 */
export function scheduleReview(
  current: ReviewState,
  rating: ReviewRating,
  now = new Date(),
): ScheduleResult {
  const today = dateKey(now);
  const reviewCount = current.reviewCount + 1;

  if (rating === "again") {
    return {
      ...current,
      box: 1,
      maintenanceStep: 0,
      lastReviewDate: today,
      nextReviewDate: dateKey(addDays(now, 1)),
      reviewCount,
      incorrectCount: current.incorrectCount + 1,
      lastRating: rating,
      sameDayRequeueAfter: 7,
    };
  }

  if (rating === "hard") {
    const shortened = Math.max(1, Math.ceil(normalInterval(current) / 2));
    return {
      ...current,
      lastReviewDate: today,
      nextReviewDate: dateKey(addDays(now, shortened)),
      reviewCount,
      hardCount: current.hardCount + 1,
      lastRating: rating,
      sameDayRequeueAfter: 12,
    };
  }

  if (current.box < 6) {
    const nextBox = (current.box + 1) as ReviewState["box"];
    const interval = LEITNER_INTERVAL_DAYS[nextBox - 1];
    return {
      ...current,
      box: nextBox,
      maintenanceStep: 0,
      lastReviewDate: today,
      nextReviewDate: dateKey(addDays(now, interval)),
      reviewCount,
      correctCount: current.correctCount + 1,
      lastRating: rating,
      sameDayRequeueAfter: null,
    };
  }

  const nextMaintenance = Math.min(current.maintenanceStep + 1, 3) as ReviewState["maintenanceStep"];
  const interval = MAINTENANCE_INTERVAL_DAYS[nextMaintenance - 1];
  return {
    ...current,
    box: 6,
    maintenanceStep: nextMaintenance,
    lastReviewDate: today,
    nextReviewDate: dateKey(addDays(now, interval)),
    reviewCount,
    correctCount: current.correctCount + 1,
    lastRating: rating,
    sameDayRequeueAfter: null,
  };
}

export function isDue(state: ReviewState, now = new Date()): boolean {
  return parseDateKey(state.nextReviewDate).getTime() <= parseDateKey(dateKey(now)).getTime();
}

export function overdueDays(state: ReviewState, now = new Date()): number {
  const due = parseDateKey(state.nextReviewDate).getTime();
  const today = parseDateKey(dateKey(now)).getTime();
  return Math.max(0, Math.floor((today - due) / 86_400_000));
}

/** Oldest overdue first. Ties put HARD/AGAIN before KNOWN. */
export function sortDueStates(states: ReviewState[], now = new Date()): ReviewState[] {
  const rank: Record<ReviewRating, number> = { again: 0, hard: 1, known: 2 };
  return states
    .filter((s) => isDue(s, now))
    .slice()
    .sort((a, b) => {
      const byDate = a.nextReviewDate.localeCompare(b.nextReviewDate);
      if (byDate !== 0) return byDate;
      const byRating = rank[a.lastRating] - rank[b.lastRating];
      if (byRating !== 0) return byRating;
      return a.articleNo - b.articleNo;
    });
}

/**
 * Hard product rule: any due review blocks all new material.
 */
export function newCardsUnlocked(dueCount: number): boolean {
  return dueCount === 0;
}

export type NewCardPlanInput = {
  totalBaseArticles?: number;
  unseenBaseArticles: number;
  elapsedPlanDays: number;
  dueCount: number;
};

/**
 * 140-day zero-config acquisition target.
 * Days 1..112 aim to finish first exposure, leaving 28 days for consolidation.
 * If the learner falls behind, the target automatically catches up once the due
 * queue is cleared. The user never configures this formula.
 */
export function targetNewCards({
  totalBaseArticles = CIVIL_BASE_ARTICLE_COUNT,
  unseenBaseArticles,
  elapsedPlanDays,
  dueCount,
}: NewCardPlanInput): number {
  if (unseenBaseArticles <= 0 || totalBaseArticles <= 0) return 0;
  if (!newCardsUnlocked(dueCount)) return 0;

  const day = Math.max(0, Math.min(EXAM_PLAN_DAYS - 1, elapsedPlanDays));
  const daysUntilAcquisitionGoal = ACQUISITION_DAYS - day;

  if (daysUntilAcquisitionGoal > 0) {
    return Math.min(20, Math.max(1, Math.ceil(unseenBaseArticles / daysUntilAcquisitionGoal)));
  }

  // Emergency catch-up inside the consolidation window. This should only be
  // reached after missed days; it protects the fixed exam deadline.
  const daysLeft = Math.max(1, EXAM_PLAN_DAYS - day);
  return Math.min(25, Math.max(1, Math.ceil(unseenBaseArticles / daysLeft)));
}

export type Mastery = "new" | "learning" | "familiar" | "strong" | "mastered";

export function masteryOf(state?: ReviewState): Mastery {
  if (!state || state.reviewCount === 0) return "new";
  if (state.box <= 2) return "learning";
  if (state.box <= 4) return "familiar";
  if (state.box === 5 || state.maintenanceStep === 0) return "strong";
  return "mastered";
}
