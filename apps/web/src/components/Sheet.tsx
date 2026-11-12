"use client";

import React, { useEffect, useRef } from "react";
import { X } from "lucide-react";
import { useLocale } from "./Providers";

/** Bottom sheet on a native dialog: focus trap and Escape come from the browser, and Back closes it. */
export function Sheet({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  const { t } = useLocale();
  const ref = useRef<HTMLDialogElement>(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    window.history.pushState({ sheet: true }, "");
    const onPop = () => onCloseRef.current();
    window.addEventListener("popstate", onPop);
    return () => {
      window.removeEventListener("popstate", onPop);
      dialog?.close();
      if (window.history.state?.sheet) window.history.back();
    };
  }, []);

  return (
    <dialog
      ref={ref}
      aria-label={title}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      className="mx-auto mb-0 mt-auto max-h-[90dvh] w-full max-w-lg overflow-y-auto rounded-t-2xl bg-surface p-0 text-ink shadow-xl backdrop:bg-black/60"
    >
      <div className="sticky top-0 z-10 bg-surface px-5 pb-3 pt-2">
        <span aria-hidden="true" className="mx-auto mb-2 block h-1 w-10 rounded-full bg-[var(--border-strong)] opacity-40" />
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-xl font-extrabold leading-tight tracking-tight">{title}</h2>
          <button type="button" aria-label={t.close} onClick={onClose} className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-muted">
            <X size={20} />
          </button>
        </div>
      </div>
      <div className="px-5 pb-6">{children}</div>
    </dialog>
  );
}
