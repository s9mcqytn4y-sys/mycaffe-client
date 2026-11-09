"use client";

import React, { useEffect, useRef } from "react";
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
      <div className="sticky top-0 flex items-center justify-between gap-3 bg-surface px-5 py-3">
        <h2 className="text-lg font-bold">{title}</h2>
        <button type="button" onClick={onClose} className="btn-secondary min-h-[44px] px-3 text-sm font-semibold">
          {t.close}
        </button>
      </div>
      <div className="px-5 pb-6">{children}</div>
    </dialog>
  );
}
