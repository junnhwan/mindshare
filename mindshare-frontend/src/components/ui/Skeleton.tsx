import { cn } from "../../lib/cn";

interface SkeletonProps {
  className?: string;
  variant?: "text" | "circular" | "rectangular";
  width?: string | number;
  height?: string | number;
}

export function Skeleton({
  className,
  variant = "text",
  width,
  height,
}: SkeletonProps) {
  return (
    <div
      className={cn(
        "animate-pulse rounded-md bg-cream-200",
        variant === "circular" && "rounded-full",
        variant === "text" && "h-4 w-full rounded-sm",
        className
      )}
      style={{ width, height }}
    />
  );
}
