import * as React from "react";
import { cn } from "@/lib/utils";

export function Input({ className, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      className={cn(
        "flex h-11 w-full rounded-md bg-elevated px-3 text-sm text-fg shadow-[0_0_0_1px_var(--mizan-line)] placeholder:text-faint focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/35 disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
}
