import * as ProgressPrimitive from "@radix-ui/react-progress";
import { cn } from "@/lib/utils";

export function Progress({
  className,
  value = 0,
  ...props
}: React.ComponentProps<typeof ProgressPrimitive.Root>) {
  return (
    <ProgressPrimitive.Root
      className={cn("relative h-1.5 w-full overflow-hidden rounded-full bg-subtle", className)}
      {...props}
    >
      <ProgressPrimitive.Indicator
        className="h-full bg-accent transition-[width] duration-300"
        style={{ width: `${value}%` }}
      />
    </ProgressPrimitive.Root>
  );
}
