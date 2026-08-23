import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { ensurePermission, fireTodaysNotifications, initNotifications, scheduleReminderLoop } from "@/lib/notify";
import { useMizan } from "@/lib/store";

export const Route = createFileRoute("/reminders")({ component: RemindersPage });

function RemindersPage() {
  const reminder = useMizan((s) => s.reminder);
  const setReminder = useMizan((s) => s.setReminder);
  const [perm, setPerm] = useState<string>("default");
  const [msg, setMsg] = useState("");

  useEffect(() => {
    if ("Notification" in window) setPerm(Notification.permission);
  }, []);

  useEffect(() => {
    scheduleReminderLoop();
  }, [reminder.time, reminder.enabled]);

  return (
    <main className="space-y-5">
      <header>
        <h1 className="text-2xl font-medium">یادآوری</h1>
        <p className="mt-1 text-sm leading-7 text-muted">
          ساعت را مشخص کنید. هر روز در همان ساعت، سه ماده مدنی، سه ماده تجارت و سه ماده آیین دادرسی — به
          ترتیب شماره — و یک یادآوری تست دکتری برایتان اعلان می‌شود. تکرار می‌شود تا ملکه ذهن شود.
        </p>
      </header>

      <section className="space-y-4 rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
        <Row
          title="فعال بودن یادآوری"
          desc="زمان‌بندی روی همین دستگاه ذخیره می‌شود."
          checked={reminder.enabled}
          onCheckedChange={(v) => setReminder({ enabled: v })}
        />
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-sm font-medium">ساعت اعلان</p>
            <p className="text-xs text-muted">۲۴ساعته، به وقت دستگاه شما</p>
          </div>
          <input
            type="time"
            value={reminder.time}
            onChange={(e) => setReminder({ time: e.target.value || "08:00" })}
            className="h-11 rounded-md bg-subtle px-3 text-sm text-fg"
          />
        </div>
        <Row
          title="مواد قانونی روزانه"
          desc="مدنی + تجارت + آیین دادرسی مدنی"
          checked={reminder.articles}
          onCheckedChange={(v) => setReminder({ articles: v })}
        />
        <Row
          title="یادآوری تست‌زنی دکتری"
          desc="یک نوبت در روز برای بانک سؤال"
          checked={reminder.exam}
          onCheckedChange={(v) => setReminder({ exam: v })}
        />
      </section>

      <section className="space-y-3 rounded-xl bg-elevated p-4 shadow-[var(--shadow-card)]">
        <h2 className="text-sm font-medium">مجوز پوش نوتیفیکیشن</h2>
        <p className="text-xs leading-6 text-muted">
          وضعیت فعلی: {permLabel(perm)}. اپ را به صفحه اصلی اندروید اضافه کنید، سپس مجوز اعلان را بدهید.
          مرورگر اگر برنامه را کاملاً ببندد ممکن است اعلان زمان‌بندی‌شده را به تأخیر بیندازد؛ با باز کردن
          میزان، مطالب همان روز آماده است.
        </p>
        <div className="flex flex-col gap-2">
          <Button
            onClick={async () => {
              const p = await ensurePermission();
              setPerm(p);
              if (p === "granted") {
                await initNotifications();
                setMsg("مجوز داده شد. اعلان‌ها در ساعت تنظیم‌شده ارسال می‌شوند.");
              } else if (p === "denied") {
                setMsg("مرورگر مجوز را رد کرده؛ از تنظیمات سایت آن را فعال کنید.");
              } else {
                setMsg("این مرورگر اعلان وب را پشتیبانی نمی‌کند.");
              }
            }}
          >
            درخواست مجوز اعلان
          </Button>
          <Button
            variant="outline"
            onClick={async () => {
              useMizan.setState({ lastNotifyDate: null });
              await fireTodaysNotifications();
              setMsg("اعلان آزمایشی ارسال شد (اگر مجوز داده باشید).");
            }}
          >
            ارسال اعلان آزمایشی همین حالا
          </Button>
        </div>
        {msg ? <p className="text-xs text-ok">{msg}</p> : null}
      </section>
    </main>
  );
}

function Row({
  title,
  desc,
  checked,
  onCheckedChange,
}: {
  title: string;
  desc: string;
  checked: boolean;
  onCheckedChange: (v: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-3">
      <div>
        <p className="text-sm font-medium">{title}</p>
        <p className="text-xs text-muted">{desc}</p>
      </div>
      <Switch checked={checked} onCheckedChange={onCheckedChange} />
    </div>
  );
}

function permLabel(p: string) {
  if (p === "granted") return "مجاز";
  if (p === "denied") return "مسدود";
  return "هنوز پرسیده نشده";
}
