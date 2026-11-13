"use client";

export function CassySpinner({ label }: { label?: string }) {
  return (
    <span className="inline-flex items-center gap-2" role="status" aria-live="polite">
      <svg
        width="20"
        height="20"
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
        className="motion-safe:animate-spin"
      >
        <circle cx="12" cy="12" r="9" stroke="var(--border-subtle)" strokeWidth="3" />
        <path
          d="M12 3a9 9 0 0 1 9 9"
          stroke="var(--accent-primary)"
          strokeWidth="3"
          strokeLinecap="round"
        />
        <circle cx="12" cy="3" r="1.75" fill="var(--accent-warm, #E9A23B)" />
      </svg>
      {label ? <span className="text-sm font-medium text-[var(--text-secondary)]">{label}</span> : null}
    </span>
  );
}
