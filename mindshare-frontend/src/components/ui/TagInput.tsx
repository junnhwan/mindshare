import { useState, type KeyboardEvent } from "react";
import { cn } from "../../lib/cn";
import { Tag } from "./Tag";

interface TagInputProps {
  value: string[];
  onChange: (tags: string[]) => void;
  placeholder?: string;
  maxTags?: number;
}

export function TagInput({
  value,
  onChange,
  placeholder = "输入标签后按回车",
  maxTags,
}: TagInputProps) {
  const [text, setText] = useState("");

  const addTag = (raw: string) => {
    const trimmed = raw.trim();
    if (!trimmed) return;
    if (value.includes(trimmed)) return;
    if (maxTags !== undefined && value.length >= maxTags) return;
    onChange([...value, trimmed]);
    setText("");
  };

  const removeTag = (index: number) => {
    onChange(value.filter((_, i) => i !== index));
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    // Support pasting comma-separated values
    if (val.includes(",")) {
      const parts = val.split(",");
      let tags = [...value];
      for (const part of parts) {
        const trimmed = part.trim();
        if (!trimmed || tags.includes(trimmed)) continue;
        if (maxTags !== undefined && tags.length >= maxTags) break;
        tags.push(trimmed);
      }
      onChange(tags);
      setText("");
    } else {
      setText(val);
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      addTag(text);
    } else if (e.key === "Backspace" && !text && value.length > 0) {
      removeTag(value.length - 1);
    }
  };

  const atLimit = maxTags !== undefined && value.length >= maxTags;

  return (
    <div className="w-full">
      {value.length > 0 && (
        <div className="mb-2 flex flex-wrap gap-1.5">
          {value.map((tag, index) => (
            <Tag
              key={`${tag}-${index}`}
              variant="azure"
              size="md"
              onRemove={() => removeTag(index)}
            >
              {tag}
            </Tag>
          ))}
        </div>
      )}

      {!atLimit && (
        <input
          type="text"
          value={text}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder={value.length === 0 ? placeholder : undefined}
          className={cn(
            "w-full rounded-md border border-white/70 bg-white/60 px-4 py-2.5 text-sm text-text-primary backdrop-blur-sm placeholder:text-text-tertiary transition-all duration-200",
            "focus:border-azure-500 focus:outline-none focus:ring-2 focus:ring-azure-500/20"
          )}
        />
      )}
    </div>
  );
}
