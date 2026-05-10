import { cn } from "../../lib/cn";

interface TabsProps {
  tabs: { key: string; label: string }[];
  activeKey: string;
  onChange: (key: string) => void;
  className?: string;
}

export function Tabs({ tabs, activeKey, onChange, className }: TabsProps) {
  return (
    <div
      className={cn(
        "flex gap-1 border-b border-white/50",
        className
      )}
      role="tablist"
    >
      {tabs.map((tab) => {
        const isActive = tab.key === activeKey;
        return (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={isActive}
            onClick={() => onChange(tab.key)}
            className={cn(
              "relative px-4 py-2.5 text-sm font-medium transition-all duration-200",
              "focus-visible:ring-2 focus-visible:ring-azure-500 focus-visible:ring-offset-2",
              isActive
                ? "text-azure-500"
                : "text-text-secondary hover:text-text-primary"
            )}
          >
            {tab.label}
            {isActive && (
              <span className="absolute bottom-0 left-0 right-0 h-0.5 rounded-full bg-azure-500" />
            )}
          </button>
        );
      })}
    </div>
  );
}
