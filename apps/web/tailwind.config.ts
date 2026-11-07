import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/lib/**/*.{js,ts,jsx,tsx,mdx}"
  ],
  darkMode: ["selector", '[data-theme="dark"]'],
  theme: {
    extend: {
      screens: {
        ipad: "1180px"
      },
      colors: {
        canvas: "var(--bg-canvas)",
        surface: "var(--bg-surface)",
        muted: "var(--bg-muted)",
        ink: "var(--text-primary)",
        subink: "var(--text-secondary)",
        subtle: "var(--border-subtle)",
        strong: "var(--border-strong)",
        brand: "var(--brand-primary)",
        "brand-fg": "var(--brand-on-primary)",
        accent: "var(--brand-accent)",
        "accent-ink": "var(--brand-accent-text)",
        azd: {
          primary: "var(--azd-primary)",
          cyan: "var(--azd-cyan)",
          obsidian: "var(--azd-obsidian)",
          ink: "var(--azd-ink)"
        }
      },
      fontFamily: {
        display: ["var(--font-display)", "Plus Jakarta Sans", "Inter", "system-ui", "sans-serif"],
        sans: ["var(--font-sans)", "Inter", "system-ui", "sans-serif"],
        mono: ["var(--font-mono)", "JetBrains Mono", "Consolas", "monospace"]
      }
    }
  },
  plugins: []
};

export default config;

