import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

// Estilo "Origin UI": sombra leve, anel de foco de 3px e ícones dimensionados pelo próprio botão.
const buttonVariants = cva(
  "inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-semibold transition-[color,box-shadow,background-color] outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground shadow-sm shadow-black/10 hover:bg-primary/90 active:bg-primary/80",
        success: "bg-success text-white shadow-sm shadow-black/10 hover:bg-success/90 active:bg-success/80",
        outline: "border border-foreground/80 bg-card shadow-xs shadow-black/5 hover:bg-muted active:bg-muted",
        secondary: "bg-secondary text-secondary-foreground hover:bg-secondary/70",
        warning: "border-2 border-dashed border-warning/60 bg-warning-soft text-warning hover:bg-warning-soft/70",
        ghost: "text-muted-foreground hover:bg-muted hover:text-foreground",
      },
      size: {
        default: "h-11 px-4",
        sm: "h-9 px-3",
        lg: "h-12 px-5 text-base",
        icon: "size-10",
      },
    },
    defaultVariants: { variant: "default", size: "default" },
  },
);

function Button({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: React.ComponentProps<"button"> & VariantProps<typeof buttonVariants> & { asChild?: boolean }) {
  const Comp = asChild ? Slot : "button";
  return <Comp className={cn(buttonVariants({ variant, size }), className)} {...props} />;
}

export { Button, buttonVariants };
