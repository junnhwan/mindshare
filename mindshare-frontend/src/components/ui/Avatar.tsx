import { cn } from "../../lib/cn";

interface AvatarProps {
  src?: string | null;
  alt?: string;
  size?: "xs" | "sm" | "md" | "lg" | "xl" | "2xl";
  className?: string;
}

const sizeClasses = {
  xs: "h-6 w-6 text-[10px]",
  sm: "h-8 w-8 text-xs",
  md: "h-10 w-10 text-sm",
  lg: "h-12 w-12 text-base",
  xl: "h-16 w-16 text-lg",
  "2xl": "h-24 w-24 text-2xl",
};

const gradients = [
  "from-azure-500 to-blue-600",
  "from-amber-400 to-orange-500",
  "from-green-400 to-teal-500",
  "from-purple-400 to-pink-500",
  "from-rose-400 to-red-500",
];

function getGradient(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return gradients[Math.abs(hash) % gradients.length];
}

function getInitials(name: string): string {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

export function Avatar({ src, alt = "", size = "md", className }: AvatarProps) {
  if (src) {
    return (
      <img
        src={src}
        alt={alt}
        className={cn(
          "rounded-full border-2 border-white object-cover shadow-sm",
          sizeClasses[size],
          className
        )}
      />
    );
  }

  const initials = getInitials(alt || "?");

  return (
    <div
      className={cn(
        "flex items-center justify-center rounded-full border-2 border-white bg-gradient-to-br font-semibold text-white shadow-sm",
        getGradient(alt || "?"),
        sizeClasses[size],
        className
      )}
      role="img"
      aria-label={alt || "用户头像"}
    >
      {initials}
    </div>
  );
}
