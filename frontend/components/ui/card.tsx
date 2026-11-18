import * as React from "react";
import { cn } from "@/lib/utils";

function Card({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      className={cn("overflow-hidden rounded-xl border bg-card text-foreground shadow-xs shadow-black/5", className)}
      {...props}
    />
  );
}

/** Seção de um card. Seções vizinhas ficam separadas por uma linha fina (divide-y no Card ou border-t aqui). */
function CardSection({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("p-4", className)} {...props} />;
}

function CardTitle({ className, ...props }: React.ComponentProps<"h2">) {
  return <h2 className={cn("text-sm font-semibold", className)} {...props} />;
}

export { Card, CardSection, CardTitle };
