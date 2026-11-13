"use client";

import React from "react";
import { Minus, Plus } from "lucide-react";
import { useLocale } from "@/components/Providers";
import { Sheet } from "@/components/Sheet";
import { formatIdr } from "@/lib/format";
import { isValidGuestName, stripControlChars } from "@/lib/sanitize";
import { optionNames, type CartAction, type CartLine } from "./cart";
import { BankPicker, METHOD_LABEL, PaymentMethodPicker } from "./PaymentMethodPicker";
import type { BankCode, PayMethod, Quote } from "./types";

export { METHOD_LABEL };

function Row({ label, value, strong }: { label: string; value: number; strong?: boolean }) {
  return (
    <div className={`flex justify-between ${strong ? "mt-2 border-t border-subtle pt-3 text-lg font-extrabold" : "py-1 text-sm text-subink"}`}>
      <span>{label}</span>
      <span className="tabular-nums">{formatIdr(value)}</span>
    </div>
  );
}

export function CartSheet({
  lines,
  multiVariant,
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
  methods,
  method,
  onMethod,
  bank,
  onBank,
  splitEnabled,
  splitCount,
  onSplitCount,
  onPay,
  onClose
}: {
  lines: CartLine[];
  multiVariant: Set<string>;
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
  methods: PayMethod[];
  method: PayMethod;
  onMethod: (method: PayMethod) => void;
  bank: BankCode;
  onBank: (bank: BankCode) => void;
  splitEnabled: boolean;
  // null pays the whole bill alone; 2 to 20 splits it equally (W13).
  splitCount: number | null;
  onSplitCount: (count: number | null) => void;
  onPay: () => void;
  onClose: () => void;
}) {
  const { t } = useLocale();
  const invalidName = !isValidGuestName(guestName);

  return (
    <Sheet title={t.cart} onClose={onClose}>
      {lines.length === 0 ? <p className="py-6 text-sm text-subink">{t.cartEmpty}</p> : null}

      <ul className="divide-y divide-[var(--border-subtle)]">
        {lines.map((line, index) => {
          const priced = quote?.lines[index];
          // "Reguler" says nothing when the product has no other size.
          const variant = multiVariant.has(line.productId) ? priced?.variant_name : undefined;
          const details = line.details ?? [variant, ...optionNames(priced?.selected_options)].filter(Boolean);
          // Two lines of one product need distinct button names for screen readers too.
          const label = [line.name, ...details].join(", ");
          return (
            <li key={line.key} className="flex items-start gap-3 py-4">
              <div className="min-w-0 flex-1">
                <p className="text-[0.9375rem] font-semibold leading-snug">{line.name}</p>
                {details.length > 0 ? <p className="mt-0.5 text-sm text-subink">{details.join(", ")}</p> : null}
                {line.notes ? <p className="mt-0.5 text-sm italic text-subink">{line.notes}</p> : null}
                {priced ? <p className="mt-1.5 text-base font-bold tabular-nums">{formatIdr(priced.subtotal_amount)}</p> : null}
              </div>
              <div role="group" aria-label={label} className="flex shrink-0 items-center gap-1 rounded-full bg-muted p-1">
                <button
                  type="button"
                  aria-label={`${t.decrease} ${label}`}
                  onClick={() => dispatch({ type: "setQuantity", key: line.key, quantity: line.quantity - 1 })}
                  className="btn-secondary h-11 w-11 rounded-full"
                >
                  <Minus size={18} />
                </button>
                <span className="min-w-7 text-center font-bold tabular-nums">{line.quantity}</span>
                <button
                  type="button"
                  aria-label={`${t.increase} ${label}`}
                  onClick={() => dispatch({ type: "setQuantity", key: line.key, quantity: line.quantity + 1 })}
                  className="btn-secondary h-11 w-11 rounded-full"
                >
                  <Plus size={18} />
                </button>
              </div>
            </li>
          );
        })}
      </ul>

      {lines.length > 0 ? (
        <>
          <h3 className="mt-4 text-base font-bold">{t.orderSummary}</h3>
          <div aria-live="polite" className="mt-2 rounded-2xl bg-muted px-4 py-3">
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

          <label htmlFor="guest-name" className="mt-6 block text-base font-bold">
            {t.guestName}
          </label>
          <input
            id="guest-name"
            value={guestName}
            onChange={(e) => onGuestName(stripControlChars(e.target.value, 60))}
            maxLength={60}
            autoComplete="given-name"
            aria-invalid={invalidName || undefined}
            aria-describedby={invalidName ? "guest-name-hint guest-name-error" : "guest-name-hint"}
            className="input-field mt-1 w-full"
          />
          <p id="guest-name-hint" className="mt-1 text-xs text-subink">
            {t.guestNameHint}{" "}
            <a href="/legal" target="_blank" rel="noopener" className="underline">{t.legal}</a>
          </p>
          {invalidName ? (
            <p id="guest-name-error" role="alert" className="mt-1 text-xs font-semibold text-[var(--state-err-fg)]">
              {t.invalidGuestName}
            </p>
          ) : null}

          {splitEnabled ? (
            <div className="mt-4">
              <label className="flex min-h-[44px] cursor-pointer items-center gap-3 text-sm font-bold">
                <input
                  type="checkbox"
                  checked={splitCount !== null}
                  onChange={(e) => onSplitCount(e.target.checked ? 2 : null)}
                  className="h-5 w-5 accent-[var(--brand-primary)]"
                />
                {t.splitToggle}
              </label>
              {splitCount !== null ? (
                <>
                  <div className="mt-1 flex items-center gap-3">
                    <span className="text-sm">{t.splitPeople}</span>
                    <button
                      type="button"
                      aria-label={`${t.decrease} ${t.splitPeople}`}
                      disabled={splitCount <= 2}
                      onClick={() => onSplitCount(splitCount - 1)}
                      className="btn-secondary h-11 w-11 rounded-full"
                    >
                      <Minus size={18} />
                    </button>
                    <span aria-live="polite" className="min-w-6 text-center font-bold">{splitCount}</span>
                    <button
                      type="button"
                      aria-label={`${t.increase} ${t.splitPeople}`}
                      disabled={splitCount >= 20}
                      onClick={() => onSplitCount(splitCount + 1)}
                      className="btn-secondary h-11 w-11 rounded-full"
                    >
                      <Plus size={18} />
                    </button>
                  </div>
                  <p className="mt-1 text-xs text-subink">{t.splitHint}</p>
                </>
              ) : null}
            </div>
          ) : null}

          {methods.length > 1 && splitCount === null ? (
            <fieldset className="mt-4">
              <legend className="text-base font-bold">{t.payMethod}</legend>
              <PaymentMethodPicker methods={methods} selected={method} onSelect={onMethod} />
              {method === "bank_transfer" ? (
                <BankPicker selected={bank} disabled={placing} label={t.selectBank} onSelect={onBank} />
              ) : null}
            </fieldset>
          ) : null}

          {error ? (
            <p role="alert" className="mt-3 text-sm font-semibold text-[var(--state-err-fg)]">
              {error}
            </p>
          ) : null}

          {canOrder ? (
            <div className="sticky bottom-0 -mx-5 mt-6 border-t border-subtle bg-surface px-5 pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-3">
            <button
              type="button"
              disabled={!quote || quoteLoading || placing || invalidName}
              onClick={onPay}
              className="btn-primary min-h-[56px] w-full rounded-xl text-base font-bold"
            >
              {placing
                ? t.placing
                : !quote
                  ? t.calculating
                  : splitCount !== null
                    ? `${t.splitPay} ${formatIdr(quote.total_amount)}`
                    : `${t.pay} ${formatIdr(quote.total_amount)}`}
            </button>
            </div>
          ) : null}
        </>
      ) : null}
    </Sheet>
  );
}
