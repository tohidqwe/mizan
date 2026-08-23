import { Link, useRouterState } from "@tanstack/react-router";
import { Bell, BookOpen, Home, Scale, Search, SunMoon } from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { applyNightClass, useMizan } from "@/lib/store";
import { initNotifications } from "@/lib/notify";
import { cn } from "@/lib/utils";
import { InstallBanner } from "./install-banner";

const NAV = [
  { to: "/", label: "خانه", icon: Home },
  { to: "/laws", label: "قوانین", icon: Scale },
  { to: "/daily", label: "روزانه", icon: BookOpen },
  { to: "/exam", label: "آزمون", icon: Search },
  { to: "/reminders", label: "یادآوری", icon: Bell },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const night = useMizan((s) => s.night);
  const toggleNight = useMizan((s) => s.toggleNight);

  useEffect(() => {
    void Promise.resolve(useMizan.persist.rehydrate()).then(() => {
      useMizan.getState().setHydrated();
      applyNightClass(useMizan.getState().night);
      void initNotifications();
    });
  }, []);

  useEffect(() => {
    applyNightClass(night);
  }, [night]);

  return (
    <div className="min-h-dvh bg-bg text-fg">
      <header className="sticky top-0 z-30 border-b border-line bg-bg/90 backdrop-blur-sm">
        <div className="mx-auto flex h-14 max-w-3xl items-center justify-between gap-3 px-4">
          <Link to="/" className="flex items-center gap-2">
            <span className="flex size-8 items-center justify-center rounded-sm bg-accent text-accent-fg">
              <Scale className="size-4" strokeWidth={1.75} />
            </span>
            <span className="text-base font-medium tracking-tight">میزان</span>
          </Link>
          <div className="flex items-center gap-1">
            <Link
              to="/search"
              aria-label="جستجو"
              className="flex size-11 items-center justify-center rounded-md text-muted hover:bg-subtle hover:text-fg"
            >
              <Search className="size-4" />
            </Link>
            <button
              type="button"
              aria-label="حالت مطالعه"
              onClick={toggleNight}
              className="flex size-11 items-center justify-center rounded-md text-muted hover:bg-subtle hover:text-fg"
            >
              <SunMoon className="size-4" />
            </button>
          </div>
        </div>
      </header>

      <div className="mx-auto w-full max-w-3xl px-4 pb-28 pt-5">{children}</div>

      <InstallBanner />

      <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-elevated/95 pb-[env(safe-area-inset-bottom)] backdrop-blur-sm">
        <ul className="mx-auto grid max-w-3xl grid-cols-5">
          {NAV.map((item) => {
            const active =
              item.to === "/"
                ? pathname === "/"
                : pathname === item.to || pathname.startsWith(`${item.to}/`);
            const Icon = item.icon;
            return (
              <li key={item.to}>
                <Link
                  to={item.to}
                  className={cn(
                    "flex h-16 flex-col items-center justify-center gap-1 text-xs",
                    active ? "text-accent" : "text-muted",
                  )}
                >
                  <Icon className="size-5" strokeWidth={active ? 2 : 1.6} />
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </div>
  );
}
