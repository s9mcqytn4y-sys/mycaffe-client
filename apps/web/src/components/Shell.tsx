"use client";

import { CassySpinner } from "./CassySpinner";
import { useLocale } from "./Providers";

const ILLUSTRATIONS: Record<"error" | "empty" | "maintenance", string> = {
  error: "/brand/illustrations/error-state.jpg",
  empty: "/brand/illustrations/empty-menu.jpg",
  maintenance: "/brand/illustrations/maintenance-state.jpg"
};

export function DataStateBox({
  state,
  title,
  description,
  illustration,
  onRetry
}: {
  state: "loading" | "error" | "empty";
  title: string;
  description: string;
  illustration?: "error" | "empty" | "maintenance";
  onRetry?: () => void;
}) {
  const { t } = useLocale();
  const art = illustration ?? (state === "error" ? "error" : state === "empty" ? "empty" : undefined);
  return (
    <div role={state === "error" ? "alert" : "status"} className="rounded-2xl border border-subtle bg-surface p-5 text-center sm:text-left">
      {art ? (
        <img
          src={ILLUSTRATIONS[art]}
          alt=""
          aria-hidden="true"
          width={240}
          height={180}
          className="mx-auto mb-4 h-32 w-auto rounded-xl object-contain sm:mx-0"
        />
      ) : null}
      <div className="flex items-center justify-center gap-2 sm:justify-start">
        {state === "loading" ? <CassySpinner /> : null}
        <h2 className="font-display text-base font-bold text-ink">{title}</h2>
      </div>
      <p className="mt-1.5 text-sm leading-relaxed text-subink">{description}</p>
      {onRetry && state !== "loading" ? (
        <button type="button" onClick={onRetry} className="btn-secondary mt-4 min-h-[48px] px-5 py-2 text-sm font-semibold">
          {t.retry}
        </button>
      ) : null}
    </div>
  );
}
