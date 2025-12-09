import { cn } from "../../lib/utils";

type BadgeVariant = "default" | "warning" | "success" | "danger";

const variantClasses: Record<BadgeVariant, string> = {
  default: "bg-slate-800 text-slate-200",
  warning: "bg-amber-500/20 text-amber-300",
  success: "bg-emerald-500/20 text-emerald-300",
  danger: "bg-rose-500/20 text-rose-300",
};

type BadgeProps = React.HTMLAttributes<HTMLSpanElement> & {
  variant?: BadgeVariant;
};

export function Badge({ className, variant = "default", ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        variantClasses[variant],
        className
      )}
      {...props}
    />
  );
}
