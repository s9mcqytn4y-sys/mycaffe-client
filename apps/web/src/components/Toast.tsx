"use client";

import { Icon } from "./Icon";

/**
 * Top-of-screen notices. The live regions stay mounted so screen readers announce a message the
 * moment it appears; the offline notice is an alert because the guest's next tap would fail.
 */
export function Notices({ toast, offline, offlineText }: { toast: string | null; offline: boolean; offlineText: string }) {
  return (
    <div className="pointer-events-none fixed inset-x-0 top-0 z-30 flex flex-col items-center gap-2 p-3">
      <div role="alert" className="w-full max-w-lg">
        {offline ? (
          <p className="flex items-center gap-2 rounded-lg bg-[var(--state-warn-bg)] px-4 py-3 text-sm font-semibold text-[var(--state-warn-fg)] shadow-md">
            <Icon name="offline" size={18} />
            {offlineText}
          </p>
        ) : null}
      </div>
      <div role="status" aria-live="polite" className="w-full max-w-lg">
        {toast ? (
          <p className="flex items-center gap-2 rounded-lg bg-brand px-4 py-3 text-sm font-semibold text-brand-fg shadow-md motion-safe:animate-enter">
            <Icon name="check" size={18} />
            {toast}
          </p>
        ) : null}
      </div>
    </div>
  );
}
