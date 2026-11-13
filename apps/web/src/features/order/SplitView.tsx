"use client";

import { useEffect, useState } from "react";
import { Check, CircleCheck, ExternalLink, QrCode, Share2 } from "lucide-react";
import { useLocale } from "@/components/Providers";
import { QrCodeSvg } from "@/components/QrCodeSvg";
import { apiFetch } from "@/lib/api";
import { formatIdr } from "@/lib/format";
import type { SplitShareLink } from "./types";

const POLL_MS = 5_000;

/** Hands out the share links of a split order and shows which shares are paid (W13). */
export function SplitView({ orderId, shares, onBack }: { orderId: string; shares: SplitShareLink[]; onBack: () => void }) {
  const { t } = useLocale();
  const [paid, setPaid] = useState<Set<number>>(new Set());
  const [qrFor, setQrFor] = useState<number | null>(null);
  const [copied, setCopied] = useState<number | null>(null);

  useEffect(() => {
    let stop = false;
    const load = async () => {
      if (document.hidden) return;
      try {
        const { splits } = await apiFetch<{ splits: { split_number: number; status: string }[] }>(
          `/api/v1/public/guest/orders/${encodeURIComponent(orderId)}/splits`
        );
        if (!stop) setPaid(new Set(splits.filter((s) => s.status === "paid").map((s) => s.split_number)));
      } catch {
        // The next poll tries again; the order tracker still reports the final result.
      }
    };
    void load();
    const id = setInterval(load, POLL_MS);
    return () => {
      stop = true;
      clearInterval(id);
    };
  }, [orderId]);

  useEffect(() => {
    if (copied === null) return;
    const timer = setTimeout(() => setCopied(null), 2500);
    return () => clearTimeout(timer);
  }, [copied]);

  const urlOf = (s: SplitShareLink) => `${window.location.origin}/bagian/${encodeURIComponent(s.token)}`;
  const send = async (s: SplitShareLink) => {
    const url = urlOf(s);
    try {
      if (navigator.share) {
        await navigator.share({ title: `${t.share} ${s.split_number}`, url });
        return;
      }
      await navigator.clipboard.writeText(url);
      setCopied(s.split_number);
    } catch {
      // A dismissed share sheet is not an error.
    }
  };

  return (
    <section className="mx-auto max-w-md py-8 pl-[max(1.25rem,env(safe-area-inset-left))] pr-[max(1.25rem,env(safe-area-inset-right))] pt-[max(2rem,env(safe-area-inset-top))]">
      <h1 className="text-2xl font-bold">{t.splitTitle}</h1>
      <p className="mt-2 text-sm text-subink">{t.splitIntro}</p>
      <ul className="mt-6 space-y-3">
        {shares.map((s) => {
          const done = paid.has(s.split_number);
          return (
            <li key={s.split_number} className="rounded-xl border border-subtle p-4">
              <div className="flex items-baseline justify-between gap-3">
                <span className="font-bold">
                  {t.share} {s.split_number}
                </span>
                <span className="tabular-nums font-bold">{formatIdr(s.amount)}</span>
              </div>
              <p className={`mt-1 inline-flex items-center gap-1.5 text-sm font-semibold ${done ? "text-[var(--state-ok-fg)]" : "text-subink"}`}>
                {done ? <CircleCheck size={16} aria-hidden="true" /> : null}
                <span>{done ? t.sharePaid : t.shareWaiting}</span>
              </p>
              {!done ? (
                <div className="mt-3 flex flex-wrap gap-2">
                  <a href={urlOf(s)} className="btn-primary inline-flex min-h-[44px] items-center gap-1.5 px-4 text-sm font-semibold motion-safe:transition-transform motion-safe:active:scale-[0.98]">
                    <ExternalLink size={16} aria-hidden="true" />
                    <span>{t.shareOpen}</span>
                  </a>
                  <button type="button" onClick={() => void send(s)} className="btn-secondary inline-flex min-h-[44px] items-center gap-1.5 px-4 text-sm font-semibold motion-safe:transition-transform motion-safe:active:scale-[0.98]">
                    {copied === s.split_number ? <Check size={16} aria-hidden="true" /> : <Share2 size={16} aria-hidden="true" />}
                    <span>{t.shareSend}</span>
                  </button>
                  <button
                    type="button"
                    aria-expanded={qrFor === s.split_number}
                    onClick={() => setQrFor(qrFor === s.split_number ? null : s.split_number)}
                    className="btn-secondary inline-flex min-h-[44px] items-center gap-1.5 px-4 text-sm font-semibold motion-safe:transition-transform motion-safe:active:scale-[0.98]"
                  >
                    <QrCode size={16} aria-hidden="true" />
                    <span>{qrFor === s.split_number ? t.shareHideQr : t.shareShowQr}</span>
                  </button>
                </div>
              ) : null}
              {copied === s.split_number ? <p role="status" className="mt-2 text-sm">{t.shareCopied}</p> : null}
              {qrFor === s.split_number && !done ? (
                <div data-testid="split-qr-code" className="mt-3 inline-block rounded-xl bg-white p-3">
                  <QrCodeSvg value={urlOf(s)} size={200} />
                </div>
              ) : null}
            </li>
          );
        })}
      </ul>
      <button type="button" onClick={onBack} className="btn-secondary mt-8 min-h-[44px] px-5 font-semibold">
        {t.back}
      </button>
    </section>
  );
}
