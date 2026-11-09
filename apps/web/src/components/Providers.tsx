"use client";

import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { MESSAGES, type Messages } from "@/lib/messages";

export type AppLocale = keyof typeof MESSAGES;

const STORAGE_KEY = "cassy_locale";

interface LocaleContextValue {
  locale: AppLocale;
  t: Messages;
  toggleLocale: () => void;
}

const LocaleContext = createContext<LocaleContextValue>({
  locale: "id-ID",
  t: MESSAGES["id-ID"],
  toggleLocale: () => undefined
});

export function LocaleProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocale] = useState<AppLocale>("id-ID");

  useEffect(() => {
    try {
      if (localStorage.getItem(STORAGE_KEY) === "en-US") setLocale("en-US");
    } catch {
      // Storage can be blocked in private browsing; the default language still works.
    }
  }, []);

  useEffect(() => {
    document.documentElement.lang = locale === "en-US" ? "en" : "id";
  }, [locale]);

  const value = useMemo(
    () => ({
      locale,
      t: MESSAGES[locale],
      toggleLocale: () => {
        const next: AppLocale = locale === "id-ID" ? "en-US" : "id-ID";
        setLocale(next);
        try {
          localStorage.setItem(STORAGE_KEY, next);
        } catch {
          // See above.
        }
      }
    }),
    [locale]
  );

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocale() {
  return useContext(LocaleContext);
}
