"use client";

import React, { useEffect, useState } from "react";
import { useLocale } from "@/components/Providers";
import { QrCodeSvg } from "@/components/QrCodeSvg";
import { apiFetch } from "@/lib/api";
import { formatIdr } from "@/lib/format";
import type { Messages } from "@/lib/messages";
import type { PaymentInfo } from "./types";

function statusText(t: Messages, status: string | undefined): string {
  if (status === "preparing") return t.statusPreparing;
  if (status === "ready") return t.statusReady;
  if (status === "served" || status === "completed") return t.statusServed;
  return t.statusReceived;
}

export function PaymentView({
  payment,
  paid,
  orderId,
  orderNumber,
  status,
  retrying,
  onNewQr,
  onBack,
  onOrderAgain
}: {
  payment: PaymentInfo;
  paid: boolean;
  orderId: string;
  orderNumber?: string;
  status?: string;
  retrying: boolean;
  onNewQr: () => void;
  onBack: () => void;
  onOrderAgain: () => void;
}) {
  const { t, locale } = useLocale();
  const [expired, setExpired] = useState(false);
  const [email, setEmail] = useState("");
  const [receipt, setReceipt] = useState<"idle" | "sending" | "sent" | "failed">("idle");

  useEffect(() => {
    setExpired(false);
    if (!payment.expires_at || paid) return;
    const remaining = new Date(payment.expires_at).getTime() - Date.now();
    const timer = setTimeout(() => setExpired(true), Math.max(remaining, 0));
    return () => clearTimeout(timer);
  }, [payment.expires_at, paid]);

  const sendReceipt = async (e: React.FormEvent) => {
    e.preventDefault();
    setReceipt("sending");
    try {
      await apiFetch(`/api/v1/public/guest/orders/${encodeURIComponent(orderId)}/receipt/send`, {
        method: "POST",
        body: { channel: "email", to: email.trim() }
      });
      setReceipt("sent");
    } catch {
      setReceipt("failed");
    }
  };

  if (paid) {
    return (
      <section className="mx-auto max-w-md px-5 py-10">
        <h1 className="text-2xl font-bold">{t.paidTitle}</h1>
        {orderNumber ? (
          <p className="mt-6 text-sm text-subink">
            {t.orderNumber}
            <span className="block text-4xl font-bold text-ink">{orderNumber}</span>
          </p>
        ) : null}
        <p aria-live="polite" className="mt-4 text-base">
          {t.statusLabel}: <strong>{statusText(t, status)}</strong>
        </p>

        <form onSubmit={sendReceipt} className="mt-8">
          <label htmlFor="receipt-email" className="block text-sm font-bold">
            {t.receiptEmail}
          </label>
          <div className="mt-1 flex gap-2">
            <input
              id="receipt-email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              className="input-field min-w-0 flex-1"
            />
            <button type="submit" disabled={receipt === "sending"} className="btn-secondary min-h-[44px] px-4 font-semibold">
              {receipt === "sending" ? t.sending : t.send}
            </button>
          </div>
          {receipt === "sent" ? <p role="status" className="mt-2 text-sm">{t.receiptSent}</p> : null}
          {receipt === "failed" ? <p role="alert" className="mt-2 text-sm font-semibold">{t.receiptFailed}</p> : null}
        </form>

        <button type="button" onClick={onOrderAgain} className="btn-primary mt-8 min-h-[56px] w-full text-base font-bold">
          {t.orderAgain}
        </button>
      </section>
    );
  }

  return (
    <section className="mx-auto flex max-w-md flex-col items-center px-5 py-8 text-center">
      <h1 className="text-2xl font-bold">{t.scanToPay}</h1>
      <p className="mt-2 text-sm text-subink">{t.scanHint}</p>
      <p className="mt-5 text-3xl font-bold">{formatIdr(payment.amount)}</p>

      {expired ? (
        <div role="alert" className="mt-6">
          <p className="font-semibold">{t.expired}</p>
          <button type="button" disabled={retrying} onClick={onNewQr} className="btn-primary mt-3 min-h-[56px] px-6 font-bold">
            {t.newQr}
          </button>
        </div>
      ) : (
        <>
          <div className="mt-6 rounded-xl bg-white p-4">
            {payment.qr_string ? <QrCodeSvg value={payment.qr_string} size={240} /> : null}
          </div>
          {payment.expires_at ? (
            <p className="mt-4 text-sm">
              {t.payBefore}{" "}
              <strong>
                {new Date(payment.expires_at).toLocaleTimeString(locale, { hour: "2-digit", minute: "2-digit" })}
              </strong>
            </p>
          ) : null}
          <p aria-live="polite" className="mt-2 text-sm text-subink">{t.waiting}</p>
        </>
      )}

      <button type="button" onClick={onBack} className="btn-secondary mt-8 min-h-[44px] px-5 font-semibold">
        {t.back}
      </button>
    </section>
  );
}
