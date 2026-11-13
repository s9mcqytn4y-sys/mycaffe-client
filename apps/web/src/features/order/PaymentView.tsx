"use client";

import React, { useEffect, useRef, useState } from "react";
import { CircleCheck, CircleX } from "lucide-react";
import { useLocale } from "@/components/Providers";
import { QrCodeSvg } from "@/components/QrCodeSvg";
import { apiFetch } from "@/lib/api";
import { formatIdr } from "@/lib/format";
import type { Messages } from "@/lib/messages";
import { METHOD_LABEL, MidtransTrustFooter, PaymentChannelLogo } from "./PaymentMethodPicker";
import { progressStep, safeDeeplink, type OrderPhase } from "./status";
import type { PaymentInfo, PayMethod, ServiceMode } from "./types";

// Status text per progressStep (0 received, 1 preparing, 2 ready, 3 served) and the steps drawn on the line.
// Pickup ends when the order is ready at the bar; table service goes on until staff mark it delivered.
function stepCopy(t: Messages, mode: ServiceMode) {
  return mode === "table"
    ? { status: [t.statusReceived, t.statusPreparing, t.statusOnTheWay, t.statusDelivered], line: 4 }
    : { status: [t.statusReceived, t.statusPreparing, t.statusReady, t.statusServed], line: 3 };
}

function ProgressSteps({ status, mode }: { status?: string; mode: ServiceMode }) {
  const { t, locale } = useLocale();
  const step = progressStep(status);
  const copy = stepCopy(t, mode);
  const labels = copy.status;
  // Times are only for steps this page saw happen; a step already reached on load has no honest time.
  const [reachedAt, setReachedAt] = useState<Record<number, string>>({});
  const firstStep = useRef(step);
  useEffect(() => {
    if (step === firstStep.current) return;
    const now = new Date().toLocaleTimeString(locale, { hour: "2-digit", minute: "2-digit" });
    setReachedAt((prev) => (prev[step] ? prev : { ...prev, [step]: now }));
  }, [step, locale]);
  return (
    <>
      <p aria-live="polite" className="mt-4 text-base">
        {t.statusLabel}: <strong>{labels[step]}</strong>
      </p>
      <ol aria-label={t.progressLabel} className="mt-4 flex gap-2">
        {labels.slice(0, copy.line).map((label, i) => (
          <li
            key={label}
            aria-current={i === step ? "step" : undefined}
            className={`flex-1 border-t-4 pt-2 text-xs font-semibold motion-safe:transition-colors motion-safe:duration-150 ${
              i <= step ? "border-brand text-ink" : "border-subtle text-subink"
            }`}
          >
            {label}
            {reachedAt[i] ? <span className="block font-normal tabular-nums text-subink">{reachedAt[i]}</span> : null}
          </li>
        ))}
      </ol>
    </>
  );
}

export function PaymentView({
  payment,
  phase,
  moneyTaken,
  orderId,
  orderNumber,
  status,
  retrying,
  error,
  methods,
  serviceMode,
  muted,
  onToggleSound,
  onNewQr,
  onSwitch,
  onBack,
  onOrderAgain
}: {
  payment: PaymentInfo;
  phase: OrderPhase;
  moneyTaken: boolean;
  orderId: string;
  orderNumber?: string;
  status?: string;
  retrying: boolean;
  error: string | null;
  methods: PayMethod[];
  serviceMode: ServiceMode;
  muted: boolean;
  onToggleSound: () => void;
  onNewQr: () => void;
  onSwitch: (method: PayMethod) => void;
  onBack: () => void;
  onOrderAgain: () => void;
}) {
  const { t, locale } = useLocale();
  const [expired, setExpired] = useState(false);
  const [email, setEmail] = useState("");
  const [receipt, setReceipt] = useState<"idle" | "sending" | "sent" | "failed">("idle");

  useEffect(() => {
    setExpired(false);
    if (!payment.expires_at || phase !== "awaiting") return;
    const remaining = new Date(payment.expires_at).getTime() - Date.now();
    const timer = setTimeout(() => setExpired(true), Math.max(remaining, 0));
    return () => clearTimeout(timer);
  }, [payment.expires_at, phase]);

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

  if (phase === "cancelled" || phase === "refunded") {
    const title = phase === "refunded" ? t.refundedTitle : t.cancelledTitle;
    const body = phase === "refunded" ? t.refundedBody : moneyTaken ? t.cancelledPaidBody : t.cancelledBody;
    return (
      <section className="mx-auto max-w-md px-5 py-10">
        <span className="inline-block text-[var(--state-err-fg)] motion-safe:animate-shake">
          <CircleX size={56} />
        </span>
        <h1 className="mt-3 text-2xl font-bold">{title}</h1>
        {orderNumber ? (
          <p className="mt-6 text-sm text-subink">
            {t.orderNumber}
            <span className="block text-4xl font-bold text-ink">{orderNumber}</span>
          </p>
        ) : null}
        <p role="status" className="mt-4 text-base">{body}</p>
        <button type="button" onClick={onOrderAgain} className="btn-primary mt-8 min-h-[56px] w-full text-base font-bold">
          {t.orderAgain}
        </button>
      </section>
    );
  }

  if (phase === "paid") {
    return (
      <section className="mx-auto max-w-md px-5 py-10">
        <span className="inline-block text-[var(--state-ok-fg)] motion-safe:animate-pop">
          <CircleCheck size={56} />
        </span>
        <h1 className="mt-3 text-2xl font-bold">{t.paidTitle}</h1>
        {orderNumber ? (
          <p className="mt-6 text-sm text-subink">
            {t.orderNumber}
            <span className="block text-4xl font-bold text-ink">{orderNumber}</span>
          </p>
        ) : null}
        <ProgressSteps status={status} mode={serviceMode} />
        <button type="button" onClick={onToggleSound} className="btn-secondary mt-4 min-h-[44px] px-4 text-sm font-semibold">
          {muted ? t.unmuteSound : t.muteSound}
        </button>

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
              aria-describedby="receipt-email-hint"
              className="input-field min-w-0 flex-1"
            />
            <button type="submit" disabled={receipt === "sending"} className="btn-secondary min-h-[44px] px-4 font-semibold">
              {receipt === "sending" ? t.sending : t.send}
            </button>
          </div>
          <p id="receipt-email-hint" className="mt-1 text-xs text-subink">
            {t.receiptEmailHint}{" "}
            <a href="/legal" target="_blank" rel="noopener" className="underline">{t.legal}</a>
          </p>
          {receipt === "sent" ? <p role="status" className="mt-2 text-sm">{t.receiptSent}</p> : null}
          {receipt === "failed" ? <p role="alert" className="mt-2 text-sm font-semibold">{t.receiptFailed}</p> : null}
        </form>

        <button type="button" onClick={onOrderAgain} className="btn-primary mt-8 min-h-[56px] w-full text-base font-bold">
          {t.orderAgain}
        </button>
      </section>
    );
  }

  const via = payment.method ?? "qris";
  const wallet = via !== "qris";
  const deeplink = wallet ? safeDeeplink(payment.deeplink_url) : null;
  const others = methods.filter((m) => m !== via);

  return (
    <section className="mx-auto flex max-w-md flex-col items-center px-5 py-8 text-center">
      <h1 className="text-2xl font-bold">{wallet ? METHOD_LABEL[via] : t.scanToPay}</h1>
      <p className="mt-2 text-sm text-subink">{wallet ? t.walletHint : t.scanHint}</p>
      <p className="mt-5 text-3xl font-bold">{formatIdr(payment.amount)}</p>

      {expired ? (
        <div role="alert" className="mt-6 flex flex-col items-center">
          <span className="text-[var(--state-err-fg)] motion-safe:animate-shake">
            <CircleX size={40} />
          </span>
          <p className="mt-2 font-semibold">{t.expired}</p>
          <button type="button" disabled={retrying} onClick={onNewQr} className="btn-primary mt-3 min-h-[56px] px-6 font-bold">
            {t.newQr}
          </button>
        </div>
      ) : (
        <>
          {wallet ? (
            deeplink ? (
              <a href={deeplink} className="btn-primary mt-6 inline-flex min-h-[56px] w-full items-center justify-center px-6 text-base font-bold">
                {t.openWallet}
              </a>
            ) : (
              <p role="alert" className="mt-6 text-sm font-semibold">{t.methodUnavailable}</p>
            )
          ) : (
            <div className="mt-6 rounded-xl bg-white p-4">
              {payment.qr_string ? <QrCodeSvg value={payment.qr_string} size={240} /> : null}
            </div>
          )}
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

      {error ? <p role="alert" className="mt-4 text-sm font-semibold text-[var(--state-err-fg)]">{error}</p> : null}

      {others.length > 0 ? (
        <div className="mt-6 w-full">
          <p className="text-sm font-bold">{t.payOther}</p>
          <div className="mt-2 flex flex-wrap justify-center gap-2">
            {others.map((m) => (
              <button
                key={m}
                type="button"
                disabled={retrying}
                onClick={() => onSwitch(m)}
                className="btn-secondary inline-flex min-h-[48px] items-center gap-2 px-4 text-sm font-semibold motion-safe:transition-transform motion-safe:active:scale-[0.98]"
              >
                <PaymentChannelLogo method={m} className="h-4 w-auto" />
                <span>{METHOD_LABEL[m]}</span>
              </button>
            ))}
          </div>
        </div>
      ) : null}

      <MidtransTrustFooter />

      <button type="button" onClick={onBack} className="btn-secondary mt-6 min-h-[44px] px-5 font-semibold motion-safe:transition-transform motion-safe:active:scale-[0.98]">
        {t.back}
      </button>
    </section>
  );
}
