"use client";

import Link from "next/link";
import { useLocale } from "./Providers";

export function SiteFooter() {
  const { t, toggleLocale } = useLocale();
  return (
    <footer className="mt-12 flex flex-wrap items-center justify-center gap-x-4 border-t border-subtle px-5 py-4 text-xs text-subink">
      <span>{t.footerPowered}</span>
      <Link href="/legal" className="inline-flex min-h-[44px] items-center underline">
        {t.legal}
      </Link>
      <button type="button" onClick={toggleLocale} className="min-h-[44px] cursor-pointer underline">
        {t.language}
      </button>
    </footer>
  );
}
