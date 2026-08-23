import { TRACK_META, type TrackLawId } from "@/data/types";
import { trackArticles } from "@/data";
import { toFaDigits, todayKey } from "./utils";
import { useMizan } from "./store";

const TRACKS: TrackLawId[] = ["civil", "commerce", "procedure"];

export function todaysBatch(track: TrackLawId, cursor: number) {
  const arts = trackArticles(track);
  if (!arts.length) return [];
  const start = ((cursor % arts.length) + arts.length) % arts.length;
  return [0, 1, 2].map((i) => arts[(start + i) % arts.length]);
}

export function dailySummaryText(cursors: Record<TrackLawId, number>): string {
  return TRACKS.map((t) => {
    const batch = todaysBatch(t, cursors[t]);
    const nums = batch.map((a) => toFaDigits(a.n)).join("، ");
    return `${TRACK_META[t].title}: مواد ${nums}`;
  }).join(" · ");
}

export async function registerSw(): Promise<ServiceWorkerRegistration | null> {
  if (typeof navigator === "undefined" || !("serviceWorker" in navigator)) return null;
  try {
    return await navigator.serviceWorker.register("/sw.js");
  } catch {
    return null;
  }
}

export async function ensurePermission(): Promise<NotificationPermission | "unsupported"> {
  if (typeof window === "undefined" || !("Notification" in window)) return "unsupported";
  if (Notification.permission === "granted") return "granted";
  if (Notification.permission === "denied") return "denied";
  return Notification.requestPermission();
}

async function showLocalNotification(title: string, body: string, url: string) {
  const options: NotificationOptions = {
    body,
    lang: "fa",
    dir: "rtl",
    icon: "/icon-192.png",
    badge: "/icon-192.png",
    tag: `mizan-${todayKey()}-${url}`,
    data: { url },
  };
  try {
    const reg = await navigator.serviceWorker.ready;
    await reg.showNotification(title, options);
    return;
  } catch {
    if (Notification.permission === "granted") {
      new Notification(title, options);
    }
  }
}

export async function fireTodaysNotifications() {
  const { reminder, cursors, lastNotifyDate, setLastNotifyDate } = useMizan.getState();
  const today = todayKey();
  if (!reminder.enabled) return;
  if (lastNotifyDate === today) return;
  if (typeof Notification === "undefined" || Notification.permission !== "granted") return;

  if (reminder.articles) {
    await showLocalNotification("سه ماده امروز — میزان", dailySummaryText(cursors), "/daily");
  }
  if (reminder.exam) {
    await showLocalNotification(
      "یادآوری تست دکتری حقوق خصوصی",
      "چند تست بزنید؛ پس از هر سؤال، پاسخ و علت نمایش داده می‌شود.",
      "/exam",
    );
  }
  setLastNotifyDate(today);
}

function msUntil(timeHHmm: string): number {
  const [h, m] = timeHHmm.split(":").map((x) => Number(x) || 0);
  const now = new Date();
  const target = new Date();
  target.setHours(h, m, 0, 0);
  if (target.getTime() <= now.getTime()) target.setDate(target.getDate() + 1);
  return target.getTime() - now.getTime();
}

let timer: ReturnType<typeof setTimeout> | null = null;

export function scheduleReminderLoop() {
  if (timer) clearTimeout(timer);
  const { reminder } = useMizan.getState();
  if (!reminder.enabled) return;
  const wait = msUntil(reminder.time);
  timer = setTimeout(() => {
    void fireTodaysNotifications();
    scheduleReminderLoop();
  }, Math.min(wait, 2_147_000_000));
}

export async function initNotifications() {
  await registerSw();
  const { reminder, lastNotifyDate } = useMizan.getState();
  if (!reminder.enabled) return;
  if (typeof Notification === "undefined" || Notification.permission !== "granted") {
    scheduleReminderLoop();
    return;
  }
  const now = new Date();
  const [h, m] = reminder.time.split(":").map((x) => Number(x) || 0);
  const passed = now.getHours() > h || (now.getHours() === h && now.getMinutes() >= m);
  if (passed && lastNotifyDate !== todayKey()) {
    await fireTodaysNotifications();
  }
  scheduleReminderLoop();
}
