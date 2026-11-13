"use client";

export function MenuSkeleton({ count = 6 }: { count?: number }) {
  return (
    <div className="mx-auto max-w-4xl space-y-5 px-5 py-4" aria-busy="true" aria-live="polite">
      <div className="space-y-2 rounded-2xl border border-subtle bg-surface p-4">
        <div className="h-5 w-40 rounded-md cassy-shimmer" />
        <div className="h-4 w-56 rounded-md cassy-shimmer" />
      </div>
      <div className="flex gap-2 overflow-hidden">
        {[1, 2, 3, 4].map((chip) => (
          <div key={chip} className="h-10 w-24 shrink-0 rounded-full cassy-shimmer" />
        ))}
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        {Array.from({ length: count }).map((_, index) => (
          <div
            key={index}
            className="flex items-start justify-between gap-3 rounded-2xl border border-subtle bg-surface p-4"
          >
            <div className="flex-1 space-y-2">
              <div className="h-4 w-3/4 rounded cassy-shimmer" />
              <div className="h-3 w-full rounded cassy-shimmer" />
              <div className="h-4 w-1/3 rounded cassy-shimmer" />
            </div>
            <div className="h-16 w-16 shrink-0 rounded-xl cassy-shimmer" />
          </div>
        ))}
      </div>
    </div>
  );
}
