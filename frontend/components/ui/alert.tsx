import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const alertVariants = cva(
  "relative grid grid-cols-[1rem_1fr] items-start gap-x-3 gap-y-0.5 rounded-lg border px-4 py-3 text-sm [&>svg]:mt-0.5 [&>svg]:size-4",
  {
    variants: {
      variant: {
        success: "border-success/25 bg-success-soft text-success",
        destructive: "border-destructive/25 bg-destructive-soft text-destructive",
        warning: "border-warning/30 bg-warning-soft text-warning",
      },
    },
    defaultVariants: { variant: "success" },
  },
);

function Alert({ className, variant, ...props }: React.ComponentProps<"div"> & VariantProps<typeof alertVariants>) {
  return <div role="alert" className={cn(alertVariants({ variant }), className)} {...props} />;
}

function AlertTitle({ className, ...props }: React.ComponentProps<"p">) {
  return <p className={cn("col-start-2 font-semibold", className)} {...props} />;
}

function AlertDescription({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("col-start-2 text-sm opacity-90", className)} {...props} />;
}

export { Alert, AlertTitle, AlertDescription };
