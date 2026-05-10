import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Inter", "system-ui", "-apple-system", "sans-serif"],
      },
      colors: {
        cream: {
          50: "#FAF7F2",
          100: "#F5F0EB",
          200: "#EDE5DA",
        },
        azure: {
          100: "#DBEAFE",
          500: "#4A90D9",
          600: "#3A7BC8",
        },
        amber: {
          100: "#F5EDE3",
          400: "#D4A574",
        },
        "text-primary": "#2D2420",
        "text-secondary": "#7A7268",
        "text-tertiary": "#A09890",
        success: "#6BAF7B",
        error: "#E07060",
      },
      borderRadius: {
        sm: "6px",
        md: "10px",
        lg: "14px",
        xl: "20px",
        "2xl": "28px",
      },
      boxShadow: {
        glass: "0 8px 32px rgba(0, 0, 0, 0.06)",
        "glass-hover": "0 12px 36px rgba(0, 0, 0, 0.08)",
        "glass-elevated": "0 16px 48px rgba(0, 0, 0, 0.10)",
      },
      backdropBlur: {
        glass: "16px",
        "glass-elevated": "24px",
      },
    },
  },
  plugins: [],
} satisfies Config;
