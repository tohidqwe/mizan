import assert from "node:assert/strict";
import test from "node:test";
import {
  ACQUISITION_DAYS,
  initialReviewState,
  newCardsUnlocked,
  scheduleReview,
  targetNewCards,
} from "./srs.ts";

const NOW = new Date(2026, 8, 17, 12, 0, 0);

test("box 1 + known advances to box 2 and two days", () => {
  const state = initialReviewState(190, NOW);
  const next = scheduleReview(state, "known", NOW);
  assert.equal(next.box, 2);
  assert.equal(next.nextReviewDate, "2026-09-19");
  assert.equal(next.sameDayRequeueAfter, null);
});

test("box 3 + again resets to box 1 and relearns in-session", () => {
  const state = {
    ...initialReviewState(190, NOW),
    box: 3 as const,
    nextReviewDate: "2026-09-17",
  };
  const next = scheduleReview(state, "again", NOW);
  assert.equal(next.box, 1);
  assert.equal(next.nextReviewDate, "2026-09-18");
  assert.equal(next.sameDayRequeueAfter, 7);
});

test("box 5 + hard stays in box 5 with shorter interval", () => {
  const state = {
    ...initialReviewState(219, NOW),
    box: 5 as const,
    nextReviewDate: "2026-09-17",
  };
  const next = scheduleReview(state, "hard", NOW);
  assert.equal(next.box, 5);
  assert.equal(next.nextReviewDate, "2026-09-25");
  assert.equal(next.sameDayRequeueAfter, 12);
});

test("box 6 known enters 60, 90, 120-day maintenance", () => {
  let state = {
    ...initialReviewState(10, NOW),
    box: 6 as const,
    nextReviewDate: "2026-09-17",
  };
  const sixty = scheduleReview(state, "known", NOW);
  assert.equal(sixty.maintenanceStep, 1);
  assert.equal(sixty.nextReviewDate, "2026-11-16");

  state = { ...sixty, nextReviewDate: "2026-11-16" };
  const d2 = new Date(2026, 10, 16, 12, 0, 0);
  const ninety = scheduleReview(state, "known", d2);
  assert.equal(ninety.maintenanceStep, 2);
  assert.equal(ninety.nextReviewDate, "2027-02-14");
});

test("due reviews hard-block all new material", () => {
  assert.equal(newCardsUnlocked(1), false);
  assert.equal(targetNewCards({ unseenBaseArticles: 1335, elapsedPlanDays: 0, dueCount: 1 }), 0);
});

test("initial 140-day plan targets about 12 new articles after dues are clear", () => {
  assert.equal(ACQUISITION_DAYS, 112);
  assert.equal(targetNewCards({ unseenBaseArticles: 1335, elapsedPlanDays: 0, dueCount: 0 }), 12);
});
