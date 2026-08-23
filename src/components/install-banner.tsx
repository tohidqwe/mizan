import { useEffect, useState } from "react";
import { Download, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useMizan } from "@/lib/store";

type BIPEvent = Event & { prompt: () => Promise<void>; userChoice: Promise<{ outcome: string }> };

export function InstallBanner() {
  const dismissed = useMizan((s) => s.installDismissed);
  const dismiss = useMizan((s) => s.dismissInstall);
  const [event, setEvent] = useState<BIPEvent | null>(null);
  const [standalone, setStandalone] = useState(false);

  useEffect(() => {
    const media = window.matchMedia("(display-mode: standalone)");
    setStandalone(media.matches || (navigator as Navigator & { standalone?: boolean }).standalone === true);
    const onBip = (e: Event) => {
      e.preventDefault();
      setEvent(e as BIPEvent);
    };
    window.addEventListener("beforeinstallprompt", onBip);
    return () => window.removeEventListener("beforeinstallprompt", onBip);
  }, []);

  if (standalone || dismissed) return null;

  return (
    <div className="fixed inset-x-0 bottom-16 z-20 mx-auto w-full max-w-3xl px-4 pb-[env(safe-area-inset-bottom)]">
      <div className="flex items-center gap-3 rounded-xl bg-accent px-3 py-2.5 text-accent-fg shadow-[var(--shadow-card)]">
        <Download className="size-4 shrink-0" />
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium">نصب روی صفحه اصلی</p>
          <p className="text-xs leading-5 opacity-80">منوی مرورگر اندروید، سپس افزودن به صفحه اصلی.</p>
          {event ? (
            <Button
              size="sm"
              variant="secondary"
              className="mt-2"
              onClick={async () => {
                await event.prompt();
                dismiss();
              }}
            >
              نصب سریع
            </Button>
          ) : null}
        </div>
        <button
          type="button"
          aria-label="بستن"
          onClick={dismiss}
          className="flex size-10 shrink-0 items-center justify-center rounded-md opacity-80 hover:opacity-100"
        >
          <X className="size-4" />
        </button>
      </div>
    </div>
  );
}
