import { cn } from "../../lib/cn";
import { X } from "lucide-react";

interface TagProps {
  children: React.ReactNode;
  onRemove?: () => void;
  variant?: "default" | "azure" | "amber";
  size?: "sm" | "md";
  className?: string;
}

const variantClasses = {
  default: "bg-white/60 text-text-secondary",
  azure: "bg-azure-100 text-azure-600",
  amber: "bg-amber-100 text-amber-400",
};

const sizeClasses = {
  sm: "px-2 py-0.5 text-[10px]",
  md: "px-2.5 py-1 text-xs",
};

export function Tag({
  children,
  onRemove,
  variant = "default",
  size = "sm",
  className,
}: TagProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border border-white/50 font-medium backdrop-blur-sm",
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
    >
      {children}
      {onRemove && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onRemove();
          }}
          className="ml-0.5 rounded-full p-0.5 hover:bg-black/10 transition-colors"
        >
          <X size={10} />
        </button>
      )}
    </span>
  );
}
