import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center gap-1 whitespace-nowrap rounded-full px-2 py-0.5 text-xs font-semibold [&_svg]:size-3",
  {
    variants: {
      variant: {
        success: "bg-lime text-lime-foreground",
        lime: "bg-lime text-lime-foreground",
        muted: "bg-secondary text-muted-foreground",
        destructive: "bg-destructive-soft text-destructive",
        outline: "border text-muted-foreground",
      },
    },
    defaultVariants: { variant: "muted" },
  },
);

function Badge({ className, variant, ...props }: React.ComponentProps<"span"> & VariantProps<typeof badgeVariants>) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge };
