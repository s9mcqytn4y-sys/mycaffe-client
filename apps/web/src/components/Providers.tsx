"use client";

import React, { createContext, useContext, useEffect, useState } from "react";

export type AppTheme = "light" | "dark";
export type AppLocale = "id-ID" | "en-US";

interface ThemeLocaleContextValue {
  theme: AppTheme;
  locale: AppLocale;
  toggleTheme: () => void;
  setLocale: (loc: AppLocale) => void;
}

const ThemeLocaleContext = createContext<ThemeLocaleContextValue>({
  theme: "light",
  locale: "id-ID",
  toggleTheme: () => undefined,
  setLocale: () => undefined
});

export function ThemeLocaleProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = useState<AppTheme>("light");
  const [locale, setLocaleState] = useState<AppLocale>("id-ID");

  useEffect(() => {
    try {
      const savedTheme = localStorage.getItem("mycaffe_theme");
      if (savedTheme === "dark" || savedTheme === "light") {
        setTheme(savedTheme);
        document.documentElement.setAttribute("data-theme", savedTheme);
      } else {
        document.documentElement.setAttribute("data-theme", "light");
      }

      const savedLocale = localStorage.getItem("mycaffe_locale");
      if (savedLocale === "id-ID" || savedLocale === "en-US") {
        setLocaleState(savedLocale);
        document.documentElement.lang = savedLocale === "en-US" ? "en" : "id";
      }
    } catch {
      document.documentElement.setAttribute("data-theme", "light");
    }
  }, []);

  const toggleTheme = () => {
    setTheme((prev) => {
      const next: AppTheme = prev === "light" ? "dark" : "light";
      document.documentElement.setAttribute("data-theme", next);
      try {
        localStorage.setItem("mycaffe_theme", next);
      } catch {
        // Ignore storage quota errors in private browsing
      }
      return next;
    });
  };

  const setLocale = (next: AppLocale) => {
    setLocaleState(next);
    if (typeof document !== "undefined") {
      document.documentElement.lang = next === "en-US" ? "en" : "id";
    }
    try {
      localStorage.setItem("mycaffe_locale", next);
    } catch {
      // Ignore storage quota errors
    }
  };

  return (
    <ThemeLocaleContext.Provider value={{ theme, locale, toggleTheme, setLocale }}>
      {children}
    </ThemeLocaleContext.Provider>
  );
}

export function useThemeLocale() {
  return useContext(ThemeLocaleContext);
}

