import { format, formatDistanceToNow, parseISO } from "date-fns";
import { zhCN } from "date-fns/locale";

export function formatDate(date: string | Date | null | undefined): string {
  if (!date) return "";
  const d = typeof date === "string" ? parseISO(date) : date;
  try {
    return format(d, "yyyy年M月d日", { locale: zhCN });
  } catch {
    return "";
  }
}

export function formatDateTime(date: string | Date | null | undefined): string {
  if (!date) return "";
  const d = typeof date === "string" ? parseISO(date) : date;
  try {
    return format(d, "yyyy年M月d日 HH:mm", { locale: zhCN });
  } catch {
    return "";
  }
}

export function formatRelativeTime(date: string | Date | null | undefined): string {
  if (!date) return "";
  const d = typeof date === "string" ? parseISO(date) : date;
  try {
    return formatDistanceToNow(d, { addSuffix: true, locale: zhCN });
  } catch {
    return "";
  }
}

export function formatCount(n: number): string {
  if (n >= 10000) {
    return (n / 10000).toFixed(1).replace(/\.0$/, "") + "万";
  }
  if (n >= 1000) {
    return (n / 1000).toFixed(1).replace(/\.0$/, "") + "k";
  }
  return String(n);
}
