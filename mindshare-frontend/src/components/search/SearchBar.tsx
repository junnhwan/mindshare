import { useState, useRef, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { Search, X } from "lucide-react";
import { cn } from "../../lib/cn";
import { useDebounce } from "../../hooks/useDebounce";
import { suggest as suggestApi } from "../../api/search";

interface SearchBarProps {
  className?: string;
  autoFocus?: boolean;
  onSearch?: () => void;
}

export function SearchBar({ className, autoFocus = false, onSearch }: SearchBarProps) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const debouncedQuery = useDebounce(query, 300);
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // Fetch suggestions
  useEffect(() => {
    if (debouncedQuery.trim().length < 1) {
      setSuggestions([]);
      return;
    }
    let cancelled = false;
    suggestApi({ prefix: debouncedQuery.trim(), size: 8 })
      .then((data) => {
        if (!cancelled) {
          setSuggestions(data.items);
          setOpen(data.items.length > 0);
          setActiveIndex(-1);
        }
      })
      .catch(() => {
        if (!cancelled) setSuggestions([]);
      });
    return () => {
      cancelled = true;
    };
  }, [debouncedQuery]);

  // Close on click outside
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const doSearch = useCallback(
    (q: string) => {
      if (!q.trim()) return;
      setOpen(false);
      navigate(`/search?q=${encodeURIComponent(q.trim())}`);
      onSearch?.();
    },
    [navigate, onSearch]
  );

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Escape") {
      setOpen(false);
      inputRef.current?.blur();
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((prev) =>
        prev < suggestions.length - 1 ? prev + 1 : 0
      );
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((prev) =>
        prev > 0 ? prev - 1 : suggestions.length - 1
      );
    } else if (e.key === "Enter") {
      if (activeIndex >= 0 && activeIndex < suggestions.length) {
        doSearch(suggestions[activeIndex]);
      } else {
        doSearch(query);
      }
    }
  };

  return (
    <div ref={containerRef} className={cn("relative", className)}>
      <div className="relative">
        <Search
          size={18}
          className="absolute left-3 top-1/2 -translate-y-1/2 text-text-tertiary"
        />
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => {
            if (suggestions.length > 0) setOpen(true);
          }}
          onKeyDown={handleKeyDown}
          placeholder="搜索知识帖子..."
          autoFocus={autoFocus}
          className="w-full rounded-lg border border-white/70 bg-white/60 py-2 pl-10 pr-8 text-sm text-text-primary backdrop-blur-sm placeholder:text-text-tertiary focus:border-azure-500 focus:outline-none focus:ring-2 focus:ring-azure-500/20 transition-all"
        />
        {query && (
          <button
            onClick={() => {
              setQuery("");
              setSuggestions([]);
              setOpen(false);
            }}
            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-text-tertiary hover:text-text-secondary"
          >
            <X size={16} />
          </button>
        )}
      </div>

      {/* Suggestions dropdown */}
      {open && suggestions.length > 0 && (
        <div className="absolute left-0 right-0 top-full z-50 mt-1 rounded-xl border border-white/70 bg-white/90 p-2 shadow-glass-elevated backdrop-blur-xl">
          {suggestions.map((item, i) => (
            <button
              key={i}
              onClick={() => doSearch(item)}
              onMouseEnter={() => setActiveIndex(i)}
              className={cn(
                "w-full rounded-lg px-3 py-2 text-left text-sm transition-colors",
                i === activeIndex
                  ? "bg-azure-100 text-azure-600"
                  : "text-text-primary hover:bg-cream-100"
              )}
            >
              {item}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
