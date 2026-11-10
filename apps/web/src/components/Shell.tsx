"use client";

import { useLocale } from "./Providers";

export function DataStateBox({
  state,
  title,
  description,
  onRetry
}: {
  state: "loading" | "error" | "empty";
  title: string;
  description: string;
  onRetry?: () => void;
}) {
  const { t } = useLocale();
  return (
    <div role={state === "error" ? "alert" : "status"} className="rounded-xl border border-subtle bg-surface p-5">
      <h2 className="font-display text-base font-bold text-ink">{title}</h2>
      <p className="mt-1.5 text-sm leading-relaxed text-subink">{description}</p>
      {onRetry && state !== "loading" ? (
        <button type="button" onClick={onRetry} className="btn-secondary mt-4 min-h-[44px] px-4 py-2 text-sm font-semibold">
          {t.retry}
        </button>
      ) : null}
    </div>
  );
}
