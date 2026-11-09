import Link from "next/link";

export type BadgeTone = "ok" | "warn" | "err" | "info";

const BADGE_CLASS: Record<BadgeTone, string> = {
  ok: "badge-ok",
  warn: "badge-warn",
  err: "badge-err",
  info: "badge-info"
};

export function StatusBadge({ tone, label }: { tone: BadgeTone; label: string }) {
  return (
    <span className={`inline-flex items-center rounded-md px-2.5 py-1 text-xs font-semibold ${BADGE_CLASS[tone]}`}>
      {label}
    </span>
  );
}

export function DataStateBox({
  state,
  title,
  description,
  onRetry,
  retryLabel = "Coba lagi"
}: {
  state: "loading" | "error" | "empty";
  title: string;
  description: string;
  onRetry?: () => void;
  retryLabel?: string;
}) {
  return (
    <div role={state === "error" ? "alert" : "status"} className="rounded-xl border border-subtle bg-surface p-5">
      <h2 className="font-display text-base font-bold text-ink">{title}</h2>
      <p className="mt-1.5 text-sm leading-relaxed text-subink">{description}</p>
      {onRetry && state !== "loading" ? (
        <button type="button" onClick={onRetry} className="btn-secondary mt-4 min-h-[44px] px-4 py-2 text-sm font-semibold">
          {retryLabel}
        </button>
      ) : null}
    </div>
  );
}

export function SiteFooter() {
  return (
    <footer className="mt-12 border-t border-subtle px-6 py-6 text-xs text-subink">
      <p>
        Ditenagai Cassy oleh AzureDevID.{" "}
        <Link href="/legal" className="underline">
          Ketentuan dan privasi
        </Link>
      </p>
    </footer>
  );
}
