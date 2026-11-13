"use client";

import React from "react";
import type { PayMethod } from "./types";

export const METHOD_LABEL: Record<PayMethod, string> = {
  qris: "QRIS",
  gopay: "GoPay",
  shopeepay: "ShopeePay"
};

const METHOD_LOGO: Record<PayMethod, string> = {
  qris: "/brand/midtrans/qris.png",
  gopay: "/brand/midtrans/gopay.svg",
  shopeepay: "/brand/midtrans/shopeepay.svg"
};

export function PaymentChannelLogo({ method, className = "h-5 w-auto" }: { method: PayMethod; className?: string }) {
  return (
    <span className="inline-flex h-7 items-center justify-center rounded bg-white px-1.5 py-0.5 ring-1 ring-black/10">
      <img src={METHOD_LOGO[method]} alt="" aria-hidden="true" className={`${className} object-contain`} />
    </span>
  );
}

export function PaymentMethodPicker({
  methods,
  selected,
  disabled,
  onSelect
}: {
  methods: PayMethod[];
  selected: PayMethod;
  disabled?: boolean;
  onSelect: (method: PayMethod) => void;
}) {
  return (
    <div className="mt-2 flex flex-wrap gap-2">
      {methods.map((m) => {
        const active = selected === m;
        return (
          <label
            key={m}
            className={`flex min-h-[48px] cursor-pointer items-center gap-2.5 rounded-xl border px-3.5 text-sm font-semibold motion-safe:transition-transform motion-safe:active:scale-[0.98] has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 ${
              active ? "border-brand bg-brand text-brand-fg shadow-sm" : "border-subtle bg-surface text-ink"
            } ${disabled ? "pointer-events-none opacity-60" : ""}`}
          >
            <input
              type="radio"
              name="pay-method"
              value={m}
              disabled={disabled}
              checked={active}
              onChange={() => onSelect(m)}
              className="sr-only"
            />
            <PaymentChannelLogo method={m} />
            <span>{METHOD_LABEL[m]}</span>
          </label>
        );
      })}
    </div>
  );
}

export function MidtransTrustFooter() {
  return (
    <div className="mt-6 flex items-center justify-center gap-2 text-xs text-subink">
      <span>Powered by</span>
      <img src="/brand/midtrans/midtrans_logo.svg" alt="Midtrans" className="h-4 w-auto object-contain" />
    </div>
  );
}
