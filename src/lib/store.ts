import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { TrackLawId } from "@/data/types";
import { initialReviewState, scheduleReview, type ReviewRating, type ReviewState } from "./srs";
import { todayKey } from "./utils";

export type ReviewMark = "again" | "known";

type Reminder = {
  enabled: boolean;
  time: string;
  exam: boolean;
  articles: boolean;
};

type ReviewEntry = {
  count: number;
  last: string;
  mark: ReviewMark;
};

type State = {
  night: boolean;
  reminder: Reminder;
  cursors: Record<TrackLawId, number>;
  completedDailyDate: string | null;
  reviewed: Record<string, ReviewEntry>;
  /** Zero-config Civil Code SRS state, keyed by the base article number. */
  civilReviews: Record<string, ReviewState>;
  /** First day of the fixed 140-day PhD preparation plan. */
  civilPlanStartDate: string | null;
  bookmarks: string[];
  examPicks: Record<string, number>;
  lastNotifyDate: string | null;
  installDismissed: boolean;
  hydrated: boolean;
  setHydrated: () => void;
  toggleNight: () => void;
  setReminder: (patch: Partial<Reminder>) => void;
  completeToday: () => void;
  markArticle: (key: string, mark: ReviewMark) => void;
  startCivilPlan: () => void;
  reviewCivilArticle: (articleNo: number, rating: ReviewRating) => ReviewState;
  toggleBookmark: (key: string) => void;
  recordExam: (id: string, pick: number) => void;
  resetExamPicks: () => void;
  setLastNotifyDate: (d: string) => void;
  dismissInstall: () => void;
};

export const useMizan = create<State>()(
  persist(
    (set, get) => ({
      night: false,
      reminder: { enabled: true, time: "08:00", exam: true, articles: true },
      cursors: { civil: 0, commerce: 0, procedure: 0 },
      completedDailyDate: null,
      reviewed: {},
      civilReviews: {},
      civilPlanStartDate: null,
      bookmarks: [],
      examPicks: {},
      lastNotifyDate: null,
      installDismissed: false,
      hydrated: false,
      setHydrated: () => set({ hydrated: true }),
      toggleNight: () => {
        const next = !get().night;
        set({ night: next });
        if (typeof document !== "undefined") {
          document.documentElement.classList.toggle("night", next);
        }
      },
      setReminder: (patch) => set({ reminder: { ...get().reminder, ...patch } }),
      completeToday: () => {
        const today = todayKey();
        if (get().completedDailyDate === today) return;
        const cursors = { ...get().cursors };
        (Object.keys(cursors) as TrackLawId[]).forEach((k) => {
          cursors[k] = cursors[k] + 3;
        });
        set({ cursors, completedDailyDate: today });
      },
      markArticle: (key, mark) => {
        const prev = get().reviewed[key];
        set({
          reviewed: {
            ...get().reviewed,
            [key]: {
              count: (prev?.count ?? 0) + 1,
              last: todayKey(),
              mark,
            },
          },
        });
      },
      startCivilPlan: () => {
        if (get().civilPlanStartDate) return;
        set({ civilPlanStartDate: todayKey() });
      },
      reviewCivilArticle: (articleNo, rating) => {
        const key = String(articleNo);
        const current = get().civilReviews[key] ?? initialReviewState(articleNo);
        const next = scheduleReview(current, rating);
        const { sameDayRequeueAfter: _sameDayRequeueAfter, ...persisted } = next;
        set({
          civilReviews: {
            ...get().civilReviews,
            [key]: persisted,
          },
          civilPlanStartDate: get().civilPlanStartDate ?? todayKey(),
        });
        return persisted;
      },
      toggleBookmark: (key) => {
        const has = get().bookmarks.includes(key);
        set({
          bookmarks: has ? get().bookmarks.filter((k) => k !== key) : [...get().bookmarks, key],
        });
      },
      recordExam: (id, pick) => set({ examPicks: { ...get().examPicks, [id]: pick } }),
      resetExamPicks: () => set({ examPicks: {} }),
      setLastNotifyDate: (d) => set({ lastNotifyDate: d }),
      dismissInstall: () => set({ installDismissed: true }),
    }),
    {
      name: "mizan-v1",
      skipHydration: true,
      partialize: (s) => ({
        night: s.night,
        reminder: s.reminder,
        cursors: s.cursors,
        completedDailyDate: s.completedDailyDate,
        reviewed: s.reviewed,
        civilReviews: s.civilReviews,
        civilPlanStartDate: s.civilPlanStartDate,
        bookmarks: s.bookmarks,
        examPicks: s.examPicks,
        lastNotifyDate: s.lastNotifyDate,
        installDismissed: s.installDismissed,
      }),
    },
  ),
);

export function applyNightClass(night: boolean) {
  if (typeof document === "undefined") return;
  document.documentElement.classList.toggle("night", night);
}
