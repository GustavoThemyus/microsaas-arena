import * as React from "react";
import { cn } from "@/lib/utils";

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      className={cn(
        "flex h-11 w-full min-w-0 rounded-lg border border-input bg-card px-3 py-2 text-sm text-foreground shadow-xs shadow-black/5 outline-none transition-[color,box-shadow] placeholder:text-muted-foreground/70 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/30 disabled:opacity-50",
        "file:-ms-1 file:me-3 file:inline-flex file:h-7 file:rounded-md file:border-0 file:bg-secondary file:px-3 file:text-sm file:font-semibold file:text-foreground",
        className,
      )}
      {...props}
    />
  );
}

export { Input };
