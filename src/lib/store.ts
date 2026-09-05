import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { ExamAttempt, ExamConfidence, ExamSubject, TrackLawId } from "@/data/types";
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
  bookmarks: string[];
  /** آخرین گزینه انتخاب‌شده برای سازگاری با نسخه‌های قبلی اپ. */
  examPicks: Record<string, number>;
  /** تاریخچه واقعی تمرین؛ مبنای مأموریت روزانه، ضعف‌ها و نمره منفی. */
  examAttempts: ExamAttempt[];
  lastNotifyDate: string | null;
  installDismissed: boolean;
  hydrated: boolean;
  setHydrated: () => void;
  toggleNight: () => void;
  setReminder: (patch: Partial<Reminder>) => void;
  completeToday: () => void;
  markArticle: (key: string, mark: ReviewMark) => void;
  toggleBookmark: (key: string) => void;
  recordExam: (
    id: string,
    pick: number | null,
    correct: boolean,
    subject: ExamSubject,
    confidence: ExamConfidence,
  ) => void;
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
      bookmarks: [],
      examPicks: {},
      examAttempts: [],
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
      toggleBookmark: (key) => {
        const has = get().bookmarks.includes(key);
        set({
          bookmarks: has ? get().bookmarks.filter((k) => k !== key) : [...get().bookmarks, key],
        });
      },
      recordExam: (id, pick, correct, subject, confidence) => {
        const at = Date.now();
        const attempt: ExamAttempt = {
          id: `${id}-${at}`,
          questionId: id,
          subject,
          date: todayKey(),
          at,
          pick,
          correct,
          skipped: pick === null,
          confidence,
        };
        set({
          examPicks: pick === null ? get().examPicks : { ...get().examPicks, [id]: pick },
          examAttempts: [...get().examAttempts, attempt].slice(-10000),
        });
      },
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
        bookmarks: s.bookmarks,
        examPicks: s.examPicks,
        examAttempts: s.examAttempts,
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
