import { cn } from "../../lib/cn";

interface BadgeProps {
  variant?: "azure" | "amber" | "green" | "red" | "gray";
  size?: "sm" | "md";
  children: React.ReactNode;
  className?: string;
}

const variantClasses = {
  azure: "bg-azure-100 text-azure-600",
  amber: "bg-amber-100 text-amber-400",
  green: "bg-green-100 text-success",
  red: "bg-red-100 text-error",
  gray: "bg-gray-100 text-text-secondary",
};

const sizeClasses = {
  sm: "px-2 py-0.5 text-[10px]",
  md: "px-2.5 py-1 text-xs",
};

export function Badge({
  variant = "azure",
  size = "sm",
  children,
  className,
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full font-medium",
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
    >
      {children}
    </span>
  );
}
