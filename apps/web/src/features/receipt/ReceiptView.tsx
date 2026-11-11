"use client";

import { useCallback, useEffect, useState } from "react";
import { useLocale } from "@/components/Providers";
import { ApiError, apiFetch } from "@/lib/api";
import { formatIdr } from "@/lib/format";
import type { Messages } from "@/lib/messages";
import { isFinal, orderPhase, progressStep } from "@/features/order/status";

interface Receipt {
  store: { name: string; address?: string };
  order_number: string;
  status: string;
  payment_status: string;
  issued_at: string;
  lines: { name: string; variant?: string; options?: string[]; quantity: number; amount: number }[];
  subtotal_amount: number;
  discount_amount: number;
  service_charge_amount: number;
  tax_amount: number;
  total_amount: number;
  payments: { method: string }[];
}

// Receipts also come from POS orders, so the labels cover more than the guest methods.
const METHOD: Record<string, string> = {
  qris: "QRIS", static_qris: "QRIS", gopay: "GoPay", shopeepay: "ShopeePay", cash: "Tunai", edc: "EDC", bank_transfer: "Transfer"
};
const REFRESH_MS = 20_000;

// The receipt does not know the outlet's service mode, so "ready" gets a label true for both modes.
function statusText(t: Messages, r: Receipt): string {
  const phase = orderPhase(r);
  if (phase === "cancelled") return t.cancelledTitle;
  if (phase === "refunded") return t.refundedTitle;
  return [t.statusReceived, t.statusPreparing, t.statusReadyShort, t.statusServed][progressStep(r.status)];
}

function Row({ label, value, strong }: { label: string; value: number; strong?: boolean }) {
  return (
    <div className={`flex justify-between py-1 ${strong ? "text-base font-bold" : "text-sm"}`}>
      <span className={strong ? undefined : "text-subink"}>{label}</span>
      <span className="tabular-nums">{formatIdr(value)}</span>
    </div>
  );
}

export function ReceiptView({ token }: { token: string }) {
  const { t, locale } = useLocale();
  const [receipt, setReceipt] = useState<Receipt | null>(null);
  const [state, setState] = useState<"loading" | "ready" | "missing" | "failed">("loading");

  const load = useCallback(async () => {
    try {
      setReceipt(await apiFetch<Receipt>(`/api/v1/public/receipts/${encodeURIComponent(token)}`));
      setState("ready");
    } catch (e) {
      setState(e instanceof ApiError && e.status === 404 ? "missing" : "failed");
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load]);

  // Follow the order until it is done, only while the guest is looking; no push, no email.
  const done = receipt ? isFinal(receipt) : true;
  useEffect(() => {
    if (done) return;
    const id = setInterval(() => {
      if (!document.hidden) void load();
    }, REFRESH_MS);
    return () => clearInterval(id);
  }, [done, load]);

  if (state === "loading") {
    return <p className="mx-auto max-w-md px-5 py-10 text-subink">{t.receiptLoading}</p>;
  }
  if (state === "missing" || state === "failed" || !receipt) {
    return (
      <section className="mx-auto max-w-md px-5 py-10">
        <h1 className="text-2xl font-bold">{state === "missing" ? t.receiptMissingTitle : t.loadFailedTitle}</h1>
        <p className="mt-2 text-base">{state === "missing" ? t.receiptMissingBody : t.loadFailedBody}</p>
        {state === "failed" ? (
          <button type="button" onClick={() => void load()} className="btn-primary mt-6 min-h-[48px] px-6 font-bold">
            {t.retry}
          </button>
        ) : null}
      </section>
    );
  }

  const methods = [...new Set(receipt.payments.map((p) => METHOD[p.method] ?? p.method))];
  return (
    <section className="mx-auto max-w-md px-5 py-8">
      <p className="text-sm text-subink">{t.receiptTitle}</p>
      <h1 className="text-2xl font-bold">{receipt.store.name}</h1>
      <p className="mt-6 text-sm text-subink">
        {t.orderNumber}
        <span className="block text-4xl font-bold text-ink">{receipt.order_number}</span>
      </p>
      <p aria-live="polite" className="mt-4 text-base">
        {t.statusLabel}: <strong>{statusText(t, receipt)}</strong>
      </p>
      <p className="mt-1 text-sm text-subink">
        {new Date(receipt.issued_at).toLocaleString(locale, { dateStyle: "medium", timeStyle: "short" })}
      </p>

      <ul className="mt-6 border-t border-subtle">
        {receipt.lines.map((line, i) => {
          const details = [line.variant, ...(line.options ?? [])].filter(Boolean).join(", ");
          return (
            <li key={i} className="flex justify-between gap-3 border-b border-subtle py-3 text-sm">
              <span>
                <span className="font-semibold">
                  {line.quantity} x {line.name}
                </span>
                {details ? <span className="block text-xs text-subink">{details}</span> : null}
              </span>
              <span className="shrink-0 tabular-nums">{formatIdr(line.amount)}</span>
            </li>
          );
        })}
      </ul>

      <div className="mt-3">
        <Row label={t.subtotal} value={receipt.subtotal_amount} />
        {receipt.discount_amount > 0 ? <Row label={t.discount} value={-receipt.discount_amount} /> : null}
        {receipt.service_charge_amount > 0 ? <Row label={t.service} value={receipt.service_charge_amount} /> : null}
        {receipt.tax_amount > 0 ? <Row label={t.tax} value={receipt.tax_amount} /> : null}
        <div className="mt-2 border-t border-subtle pt-2">
          <Row label={t.total} value={receipt.total_amount} strong />
        </div>
      </div>
      {methods.length > 0 ? (
        <p className="mt-3 text-sm">
          {t.paidWith}: <strong>{methods.join(", ")}</strong>
        </p>
      ) : null}
    </section>
  );
}
