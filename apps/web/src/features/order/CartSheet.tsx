"use client";

import React from "react";
import { useLocale } from "@/components/Providers";
import { Sheet } from "@/components/Sheet";
import { formatIdr } from "@/lib/format";
import type { CartAction, CartLine } from "./cart";
import type { Quote } from "./types";

function Row({ label, value, strong }: { label: string; value: number; strong?: boolean }) {
  return (
    <div className={`flex justify-between py-0.5 ${strong ? "text-base font-bold" : "text-sm text-subink"}`}>
      <span>{label}</span>
      <span>{formatIdr(value)}</span>
    </div>
  );
}

export function CartSheet({
  lines,
  dispatch,
  quote,
  quoteLoading,
  quoteFailed,
  onRetryQuote,
  guestName,
  onGuestName,
  placing,
  error,
  canOrder,
  onPay,
  onClose
}: {
  lines: CartLine[];
  dispatch: (action: CartAction) => void;
  quote: Quote | null;
  quoteLoading: boolean;
  quoteFailed: boolean;
  onRetryQuote: () => void;
  guestName: string;
  onGuestName: (value: string) => void;
  placing: boolean;
  error: string | null;
  canOrder: boolean;
  onPay: () => void;
  onClose: () => void;
}) {
  const { t } = useLocale();

  return (
    <Sheet title={t.cart} onClose={onClose}>
      {lines.length === 0 ? <p className="py-6 text-sm text-subink">{t.cartEmpty}</p> : null}

      <ul>
        {lines.map((line, index) => {
          const priced = quote?.lines[index];
          return (
            <li key={line.key} className="py-3">
              <div className="flex justify-between gap-3 text-sm font-semibold">
                <span>{line.name}</span>
                {priced ? <span>{formatIdr(priced.subtotal_amount)}</span> : null}
              </div>
              {priced?.variant_name || priced?.selected_options ? (
                <p className="text-xs text-subink">{[priced.variant_name, priced.selected_options].filter(Boolean).join(", ")}</p>
              ) : null}
              {line.notes ? <p className="text-xs text-subink">{line.notes}</p> : null}
              <div className="mt-2 flex items-center gap-3">
                <button
                  type="button"
                  aria-label={`${t.decrease} ${line.name}`}
                  onClick={() => dispatch({ type: "setQuantity", key: line.key, quantity: line.quantity - 1 })}
                  className="btn-secondary h-11 w-11 text-lg font-bold"
                >
                  -
                </button>
                <span className="min-w-6 text-center font-bold">{line.quantity}</span>
                <button
                  type="button"
                  aria-label={`${t.increase} ${line.name}`}
                  onClick={() => dispatch({ type: "setQuantity", key: line.key, quantity: line.quantity + 1 })}
                  className="btn-secondary h-11 w-11 text-lg font-bold"
                >
                  +
                </button>
              </div>
            </li>
          );
        })}
      </ul>

      {lines.length > 0 ? (
        <>
          <div aria-live="polite" className="mt-3 border-t border-subtle pt-3">
            {quote ? (
              <>
                <Row label={t.subtotal} value={quote.subtotal_amount} />
                {quote.discount_amount > 0 ? <Row label={t.discount} value={-quote.discount_amount} /> : null}
                {quote.service_charge_amount > 0 ? <Row label={t.service} value={quote.service_charge_amount} /> : null}
                {quote.tax_amount > 0 ? <Row label={t.tax} value={quote.tax_amount} /> : null}
                <Row label={t.total} value={quote.total_amount} strong />
              </>
            ) : quoteFailed ? (
              <div role="alert" className="text-sm">
                <p>{t.quoteFailed}</p>
                <button type="button" onClick={onRetryQuote} className="btn-secondary mt-2 min-h-[44px] px-4 font-semibold">
                  {t.retry}
                </button>
              </div>
            ) : (
              <p className="text-sm text-subink">{t.calculating}</p>
            )}
          </div>

          <label htmlFor="guest-name" className="mt-4 block text-sm font-bold">
            {t.guestName}
          </label>
          <input
            id="guest-name"
            value={guestName}
            onChange={(e) => onGuestName(e.target.value)}
            maxLength={60}
            autoComplete="given-name"
            aria-describedby="guest-name-hint"
            className="input-field mt-1 w-full"
          />
          <p id="guest-name-hint" className="mt-1 text-xs text-subink">
            {t.guestNameHint}
          </p>

          {error ? (
            <p role="alert" className="mt-3 text-sm font-semibold text-[var(--state-err-fg)]">
              {error}
            </p>
          ) : null}

          {canOrder ? (
            <button
              type="button"
              disabled={!quote || quoteLoading || placing}
              onClick={onPay}
              className="btn-primary mt-4 min-h-[56px] w-full text-base font-bold"
            >
              {placing ? t.placing : quote ? `${t.pay} ${formatIdr(quote.total_amount)}` : t.calculating}
            </button>
          ) : null}
        </>
      ) : null}
    </Sheet>
  );
}
