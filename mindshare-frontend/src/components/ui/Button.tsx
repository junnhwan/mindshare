import { type ButtonHTMLAttributes, type ReactNode } from "react";
import { cn } from "../../lib/cn";
import { Spinner } from "./Spinner";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "ghost" | "danger" | "text";
  size?: "sm" | "md" | "lg";
  loading?: boolean;
  icon?: ReactNode;
  children: ReactNode;
}

const variantClasses = {
  primary:
    "bg-azure-500 text-white hover:bg-azure-600 active:bg-azure-600 shadow-md shadow-azure-500/20",
  secondary:
    "glass text-text-primary hover:bg-white/80 active:bg-white/90",
  ghost:
    "bg-transparent text-text-secondary hover:bg-white/50 active:bg-white/70",
  danger:
    "bg-error text-white hover:bg-red-500 active:bg-red-600 shadow-md shadow-error/20",
  text: "bg-transparent text-azure-500 hover:text-azure-600 hover:bg-azure-100/50",
};

const sizeClasses = {
  sm: "px-3 py-1.5 text-xs rounded-md gap-1.5",
  md: "px-5 py-2.5 text-sm rounded-md gap-2",
  lg: "px-6 py-3 text-base rounded-lg gap-2.5",
};

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  icon,
  children,
  className,
  disabled,
  ...props
}: ButtonProps) {
  return (
    <button
      className={cn(
        "inline-flex items-center justify-center font-medium transition-all duration-200 focus-visible:ring-2 focus-visible:ring-azure-500 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? (
        <Spinner size="sm" className="mr-0" />
      ) : icon ? (
        <span className="shrink-0">{icon}</span>
      ) : null}
      {children}
    </button>
  );
}
