"use client";

import React from "react";
import { Landmark } from "lucide-react";
import { useLocale } from "@/components/Providers";
import type { BankCode, PayMethod } from "./types";

export const METHOD_LABEL: Record<PayMethod, string> = {
  qris: "QRIS",
  gopay: "GoPay",
  shopeepay: "ShopeePay",
  bank_transfer: "Transfer Bank (VA)"
};

const METHOD_LOGO: Record<Exclude<PayMethod, "bank_transfer">, string> = {
  qris: "/brand/midtrans/qris.png",
  gopay: "/brand/midtrans/gopay.svg",
  shopeepay: "/brand/midtrans/shopeepay.svg"
};

export const VA_BANKS: { code: BankCode; label: string; logo: string }[] = [
  { code: "bca", label: "BCA", logo: "/brand/midtrans/bca.png" },
  { code: "bni", label: "BNI", logo: "/brand/midtrans/bni.png" },
  { code: "bri", label: "BRI", logo: "/brand/midtrans/bri.png" },
  { code: "mandiri", label: "Mandiri", logo: "/brand/midtrans/mandiri.png" },
  { code: "permata", label: "Permata", logo: "/brand/midtrans/permata_bank.png" }
];

export function bankMeta(code?: string) {
  return VA_BANKS.find((b) => b.code === (code ?? "").toLowerCase());
}

export function PaymentChannelLogo({ method, className = "h-5 w-auto" }: { method: PayMethod; className?: string }) {
  if (method === "bank_transfer") {
    return (
      <span className="inline-flex h-7 items-center justify-center rounded bg-white px-1.5 py-0.5 text-[#17211E] ring-1 ring-black/10">
        <Landmark size={16} aria-hidden="true" />
      </span>
    );
  }
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
  const { t } = useLocale();
  return (
    <div className="mt-2 flex flex-wrap gap-2">
      {methods.map((m) => {
        const active = selected === m;
        const label = m === "bank_transfer" ? t.bankTransferLabel : METHOD_LABEL[m];
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
            <span>{label}</span>
          </label>
        );
      })}
    </div>
  );
}

export function BankPicker({
  selected,
  disabled,
  label,
  onSelect
}: {
  selected: BankCode;
  disabled?: boolean;
  label: string;
  onSelect: (bank: BankCode) => void;
}) {
  return (
    <fieldset className="mt-3">
      <legend className="text-sm font-bold text-ink">{label}</legend>
      <div className="mt-2 grid grid-cols-3 gap-2 sm:grid-cols-5">
        {VA_BANKS.map((bank) => {
          const active = selected === bank.code;
          return (
            <label
              key={bank.code}
              className={`flex min-h-[48px] cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border px-2 py-1.5 text-xs font-bold motion-safe:transition-transform motion-safe:active:scale-[0.98] has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 ${
                active ? "border-brand bg-brand text-brand-fg shadow-sm" : "border-subtle bg-surface text-ink"
              } ${disabled ? "pointer-events-none opacity-60" : ""}`}
            >
              <input
                type="radio"
                name="va-bank"
                value={bank.code}
                disabled={disabled}
                checked={active}
                onChange={() => onSelect(bank.code)}
                className="sr-only"
              />
              <span className="inline-flex h-5 items-center justify-center rounded bg-white px-1.5">
                <img src={bank.logo} alt="" aria-hidden="true" className="h-3.5 w-auto object-contain" />
              </span>
              <span>{bank.label}</span>
            </label>
          );
        })}
      </div>
    </fieldset>
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
