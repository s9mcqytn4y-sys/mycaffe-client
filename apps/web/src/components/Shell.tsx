"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useThemeLocale } from "./Providers";

export type BadgeTone = "ok" | "warn" | "err" | "info";

export function StatusBadge({
  tone,
  label,
  prefix
}: {
  tone: BadgeTone;
  label: string;
  prefix?: string;
}) {
  const defaultPrefix =
    prefix ??
    (tone === "ok"
      ? "OK"
      : tone === "warn"
      ? "PERHATIAN"
      : tone === "err"
      ? "GALAT"
      : "INFO");

  const cls =
    tone === "ok"
      ? "badge-ok"
      : tone === "warn"
      ? "badge-warn"
      : tone === "err"
      ? "badge-err"
      : "badge-info";

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-semibold ${cls}`}>
      <span className="font-mono text-[11px] font-bold">[{defaultPrefix}]</span>
      <span>{label}</span>
    </span>
  );
}

export function DataStateBox({
  state,
  title,
  description,
  onRetry,
  retryLabel = "Muat Ulang Data"
}: {
  state: "loading" | "error" | "empty";
  title: string;
  description: string;
  onRetry?: () => void;
  retryLabel?: string;
}) {
  const tone: BadgeTone = state === "error" ? "err" : state === "loading" ? "info" : "warn";
  const prefix = state === "error" ? "GALAT" : state === "loading" ? "MEMUAT" : "KOSONG";

  return (
    <div
      role={state === "error" ? "alert" : "status"}
      aria-live="polite"
      className="rounded-xl border border-subtle bg-surface p-5"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <StatusBadge tone={tone} prefix={prefix} label={title} />
        {onRetry && state !== "loading" ? (
          <button
            type="button"
            onClick={onRetry}
            className="btn-secondary min-h-[44px] px-4 py-2 text-sm font-semibold"
          >
            {retryLabel}
          </button>
        ) : null}
      </div>
      <p className="mt-2.5 text-sm leading-relaxed text-subink">{description}</p>
    </div>
  );
}

export function ModalDialog({
  open,
  title,
  onClose,
  children
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="max-h-[90dvh] w-full max-w-xl overflow-y-auto rounded-xl border border-subtle bg-surface p-5 text-ink shadow-xl"
      >
        <div className="flex items-center justify-between gap-3 border-b border-subtle pb-3">
          <h2 className="font-display text-lg font-bold">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="btn-secondary min-h-[44px] min-w-[44px] px-3 py-1.5 text-sm font-semibold"
          >
            Tutup (Esc)
          </button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}

const NAV_ITEMS = [
  { href: "/", idLabel: "Beranda", enLabel: "Overview" },
  { href: "/onboarding", idLabel: "Daftar Gerai", enLabel: "Store Onboarding" },
  { href: "/pos", idLabel: "Ruang Kasir POS", enLabel: "POS Workspace" },
  { href: "/order", idLabel: "Pesan Meja QR", enLabel: "Guest QR Order" },
  { href: "/platform", idLabel: "Superadmin", enLabel: "Superadmin" },
  { href: "/legal", idLabel: "Hukum & PDP", enLabel: "Legal & Privacy" }
];

export function TopNavBar() {
  const pathname = usePathname();
  const { theme, locale, toggleTheme, setLocale } = useThemeLocale();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!mobileMenuOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMobileMenuOpen(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [mobileMenuOpen]);

  return (
    <header className="sticky top-0 z-40 border-b border-subtle bg-surface">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-4 py-2.5 sm:px-6">
        <Link
          href="/"
          className="flex min-h-[44px] items-center gap-2.5 rounded-lg px-1.5 py-1"
        >
          <span
            aria-hidden="true"
            className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-brand font-display text-sm font-extrabold text-brand-fg"
          >
            CS
          </span>
          <span className="leading-tight">
            <span className="block font-display text-base font-extrabold tracking-tight text-ink">
              Cassy
            </span>
            <span className="block text-[11px] font-medium text-subink">
              by AzureDevID
            </span>
          </span>
        </Link>

        <nav aria-label="Navigasi utama" className="hidden items-center gap-1 lg:flex">
          {NAV_ITEMS.map((item) => {
            const active = pathname === item.href || (item.href !== "/" && pathname?.startsWith(item.href));
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={`inline-flex min-h-[44px] items-center rounded-lg px-3 py-2 text-sm font-semibold transition-colors ${
                  active
                    ? "bg-brand text-brand-fg"
                    : "text-ink hover:bg-muted"
                }`}
              >
                {locale === "en-US" ? item.enLabel : item.idLabel}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setLocale(locale === "id-ID" ? "en-US" : "id-ID")}
            aria-label={locale === "id-ID" ? "Ganti bahasa ke English" : "Switch language to Bahasa Indonesia"}
            className="btn-secondary min-h-[44px] px-3 py-1.5 font-mono text-xs font-semibold"
          >
            {locale === "id-ID" ? "ID" : "EN"}
          </button>

          <button
            type="button"
            onClick={toggleTheme}
            aria-pressed={theme === "dark"}
            className="btn-secondary min-h-[44px] px-3 py-1.5 text-xs font-semibold"
          >
            {theme === "dark"
              ? locale === "en-US"
                ? "Light Mode"
                : "Tema Terang"
              : locale === "en-US"
              ? "Dark Mode"
              : "Tema Gelap"}
          </button>

          <button
            type="button"
            onClick={() => setMobileMenuOpen((v) => !v)}
            aria-expanded={mobileMenuOpen}
            aria-controls="mobile-nav-drawer"
            className="btn-primary min-h-[44px] px-3.5 py-1.5 text-xs font-semibold lg:hidden"
          >
            {mobileMenuOpen ? "Tutup" : "Menu"}
          </button>
        </div>
      </div>

      {mobileMenuOpen ? (
        <nav
          id="mobile-nav-drawer"
          aria-label="Menu seluler"
          className="border-t border-subtle bg-surface px-4 py-3 lg:hidden"
        >
          <div className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
            {NAV_ITEMS.map((item) => {
              const active = pathname === item.href || (item.href !== "/" && pathname?.startsWith(item.href));
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex min-h-[44px] items-center rounded-lg px-3.5 py-2.5 text-sm font-semibold ${
                    active ? "bg-brand text-brand-fg" : "bg-muted text-ink"
                  }`}
                >
                  {locale === "en-US" ? item.enLabel : item.idLabel}
                </Link>
              );
            })}
          </div>
        </nav>
      ) : null}
    </header>
  );
}

export function SiteFooter() {
  const { locale } = useThemeLocale();
  return (
    <footer className="mt-16 border-t border-subtle bg-surface">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
        <div className="flex flex-col justify-between gap-6 md:flex-row md:items-start">
          <div className="max-w-xl space-y-2">
            <p className="font-display text-base font-bold text-ink">
              Cassy by AzureDevID
            </p>
            <p className="text-sm text-subink">
              {locale === "en-US"
                ? "Operated by AzureDevID (Jakarta Pusat, DKI Jakarta, Indonesia). Multi-tenant cafe POS, table QR ordering, KDS, and outlet network governance."
                : "Dioperasikan oleh AzureDevID (Jakarta Pusat, DKI Jakarta, Indonesia). Sistem kasir POS multi-tenant, pesan meja QR, layar dapur KDS, dan tata kelola jaringan gerai."}
            </p>
            <p className="font-mono text-xs text-subink">
              App ID: online.abbdullaziz.mycaffe | Kepatuhan UU No. 27 Tahun 2022 (UU PDP) &amp; QRIS IDR
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Link href="/onboarding" className="btn-secondary min-h-[44px] px-3.5 py-2 text-xs font-semibold">
              {locale === "en-US" ? "Register Store" : "Pendaftaran Gerai"}
            </Link>
            <Link href="/pos" className="btn-secondary min-h-[44px] px-3.5 py-2 text-xs font-semibold">
              {locale === "en-US" ? "Staff POS" : "Kasir & Backoffice"}
            </Link>
            <Link href="/legal" className="btn-secondary min-h-[44px] px-3.5 py-2 text-xs font-semibold">
              {locale === "en-US" ? "Terms & Privacy" : "Ketentuan & Privasi"}
            </Link>
            <a
              href="/network-topology-simulator.html"
              className="btn-secondary min-h-[44px] px-3.5 py-2 text-xs font-semibold"
            >
              {locale === "en-US" ? "Standalone LAN Simulator" : "Simulator Topologi LAN"}
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}

