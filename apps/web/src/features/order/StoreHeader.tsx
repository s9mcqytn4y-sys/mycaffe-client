"use client";

import { Megaphone } from "lucide-react";
import { CassyMonogram } from "@/components/CassyLogo";

/** Outlet name leads (PRD v4 Q2); the Cassy mark stands in until outlets can upload their own logo. */
export function StoreHeader({
  storeName,
  tableLabel,
  tagline,
  announcement
}: {
  storeName: string;
  tableLabel: string;
  tagline?: string;
  announcement?: string;
}) {
  return (
    <header className="px-5 pt-5">
      <div className="flex items-center gap-3">
        <CassyMonogram size={48} className="shrink-0 rounded-2xl bg-surface p-1.5 ring-1 ring-subtle" />
        <div className="min-w-0 flex-1">
          <h1 className="line-clamp-2 text-2xl font-extrabold leading-tight tracking-tight">{storeName}</h1>
          {tagline ? <p className="mt-0.5 line-clamp-1 text-sm text-subink">{tagline}</p> : null}
        </div>
        <p className="shrink-0 rounded-full bg-brand px-3 py-1.5 text-sm font-bold text-brand-fg">{tableLabel}</p>
      </div>
      {announcement ? (
        <p className="mt-4 flex items-start gap-2.5 rounded-xl bg-muted px-3.5 py-3 text-sm leading-snug">
          <Megaphone size={18} className="mt-0.5 shrink-0 text-subink" />
          <span className="line-clamp-2">{announcement}</span>
        </p>
      ) : null}
    </header>
  );
}
