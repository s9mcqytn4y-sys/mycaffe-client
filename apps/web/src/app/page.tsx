"use client";

import React, { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import {
  apiFetch,
  LegalDocument,
  normalizeLegalDoc,
  normalizePlan,
  SubscriptionPlan
} from "@/lib/api";
import { formatIdr } from "@/lib/format";
import { useThemeLocale } from "@/components/Providers";
import { DataStateBox, StatusBadge } from "@/components/Shell";

export default function LandingPage() {
  const { locale } = useThemeLocale();
  const isEn = locale === "en-US";

  const [billingCycle, setBillingCycle] = useState<"monthly" | "annual">("monthly");
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [plansLoading, setPlansLoading] = useState(true);
  const [plansError, setPlansError] = useState<string | null>(null);

  const [legalDocs, setLegalDocs] = useState<LegalDocument[]>([]);
  const [legalLoading, setLegalLoading] = useState(true);
  const [legalError, setLegalError] = useState<string | null>(null);

  const [healthState, setHealthState] = useState<"checking" | "online" | "offline">("checking");
  const [healthLabel, setHealthLabel] = useState<string>("Memeriksa /health/ready...");

  const loadLandingData = useCallback(async () => {
    setPlansLoading(true);
    setPlansError(null);
    try {
      const res = await apiFetch<{ plans?: Array<Record<string, unknown>> }>("/api/v1/public/plans");
      const list = Array.isArray(res.plans) ? res.plans.map(normalizePlan) : [];
      setPlans(list);
    } catch (err) {
      setPlansError(err instanceof Error ? err.message : "Gagal memuat paket langganan");
    } finally {
      setPlansLoading(false);
    }

    setLegalLoading(true);
    setLegalError(null);
    try {
      const res = await apiFetch<{ documents?: Array<Record<string, unknown>> }>(
        `/api/v1/public/legal?locale=${locale}`
      );
      const docs = Array.isArray(res.documents) ? res.documents.map(normalizeLegalDoc) : [];
      setLegalDocs(docs);
    } catch (err) {
      setLegalError(err instanceof Error ? err.message : "Gagal memuat ringkasan dokumen hukum");
    } finally {
      setLegalLoading(false);
    }

    setHealthState("checking");
    try {
      const h = await apiFetch<{ status?: string }>("/health/ready");
      setHealthState("online");
      setHealthLabel(`Backend Go RLS Siap (${h.status ?? "ready"})`);
    } catch {
      setHealthState("offline");
      setHealthLabel("Backend Go belum aktif di :8080");
    }
  }, [locale]);

  useEffect(() => {
    void loadLandingData();
  }, [loadLandingData]);

  return (
    <div className="mx-auto max-w-7xl space-y-14 px-4 py-8 sm:px-6 sm:py-12">
      {/* Asymmetric Operational Hero + Workspace Switcher */}
      <section className="grid grid-cols-1 items-start gap-8 lg:grid-cols-12">
        <div className="space-y-6 lg:col-span-7">
          <div className="flex flex-wrap items-center gap-2.5">
            <StatusBadge
              tone={healthState === "online" ? "ok" : healthState === "checking" ? "info" : "warn"}
              prefix={healthState === "online" ? "ONLINE" : healthState === "checking" ? "CEK" : "LOKAL"}
              label={healthLabel}
            />
            <span className="rounded-md border border-subtle bg-surface px-2.5 py-1 font-mono text-xs text-subink">
              ID Paket: online.abbdullaziz.mycaffe
            </span>
          </div>

          <h1 className="font-display text-3xl font-extrabold leading-tight tracking-tight text-ink sm:text-4xl lg:text-[2.65rem]">
            {isEn
              ? "Table QR Ordering, Tablet POS, Kitchen KDS, and Outlet Network Control in One Ledger."
              : "Pesan Meja QR, Kasir POS Tablet, Layar Dapur KDS, dan Kendali Jaringan Gerai dalam Satu Buku Besar."}
          </h1>

          <p className="max-w-2xl text-base leading-relaxed text-subink sm:text-lg">
            {isEn
              ? "Built by AzureDevID for Indonesian specialty cafes and restaurants. Every Rupiah is calculated server-side in integer IDR, isolated per store via PostgreSQL 18 Row-Level Security, and connected directly to Midtrans Core API and LAN ESC/POS printers."
              : "Dirancang oleh AzureDevID untuk operasional kafe dan restoran di Indonesia. Seluruh nominal dihitung di server dalam bilangan bulat Rupiah, diisolasi per gerai dengan PostgreSQL 18 Row-Level Security, serta terhubung ke Midtrans Core API dan printer thermal LAN ESC/POS."}
          </p>

          <div className="flex flex-wrap gap-3 pt-1">
            <Link
              href="/onboarding"
              className="btn-primary min-h-[48px] px-5 py-2.5 text-sm font-bold"
            >
              {isEn ? "Register New Cafe Store" : "Daftarkan Gerai Baru"}
            </Link>
            <Link
              href="/pos"
              className="btn-secondary min-h-[48px] px-5 py-2.5 text-sm font-semibold"
            >
              {isEn ? "Open Staff POS & Backoffice" : "Buka Ruang Kasir POS"}
            </Link>
            <Link
              href="/order"
              className="btn-secondary min-h-[48px] px-5 py-2.5 text-sm font-semibold"
            >
              {isEn ? "Simulate Guest Table QR" : "Simulasi Pesan Meja QR"}
            </Link>
          </div>
        </div>

        {/* Operational Workspace Ledger Switcher */}
        <div className="rounded-xl border border-subtle bg-surface p-5 sm:p-6 lg:col-span-5">
          <div className="flex items-center justify-between gap-2 border-b border-subtle pb-3">
            <div>
              <h2 className="font-display text-base font-bold text-ink">
                {isEn ? "Direct Workspace Launcher" : "Pemilih Ruang Kerja Operasional"}
              </h2>
              <p className="text-xs text-subink">
                {isEn
                  ? "Jump directly into any role surface."
                  : "Akses langsung setiap peran dari satu repositori klien."}
              </p>
            </div>
            <span className="font-mono text-xs font-semibold text-accent-ink">v1.0</span>
          </div>

          <div className="mt-4 divide-y divide-subtle">
            {[
              {
                href: "/onboarding",
                code: "01 / ONBOARD",
                title: isEn ? "3-Step Store Onboarding + Email OTP" : "Pendaftaran Mandiri 3 Langkah + OTP Email",
                desc: isEn
                  ? "Slug validation, NIB/NPWP verification, ToS/PDP SHA-256 consent."
                  : "Validasi slug gerai, NIB 13 digit, NPWP, dan tanda tangan SHA-256 UU PDP."
              },
              {
                href: "/pos",
                code: "02 / POS + KDS",
                title: isEn ? "Adaptive Cashier, Midtrans BYOK & MikroTik" : "Kasir Adaptif, Midtrans BYOK & Wizard MikroTik",
                desc: isEn
                  ? "Menu & QR tables, KDS tickets, RouterOS v7 .rsc download, offline sync."
                  : "Katalog, QR meja SVG, tiket dapur KDS, unduh skrip .rsc RouterOS v7, dan antrean offline."
              },
              {
                href: "/order",
                code: "03 / GUEST QR",
                title: isEn ? "Guest Table QR Ordering PWA" : "PWA Pemesanan Meja QR Pelanggan",
                desc: isEn
                  ? "No app install, live server quote, scannable Midtrans QRIS & order tracker."
                  : "Tanpa unduh aplikasi, hitung harga server, kode QRIS langsung dipindai, dan pelacak pesanan."
              },
              {
                href: "/platform",
                code: "04 / AZUREDEVID",
                title: isEn ? "Platform Superadmin & 1-Click Provisioning" : "Konsol Superadmin & Provisi Gerai 1-Klik",
                desc: isEn
                  ? "Approve onboarding, set initial Owner PIN, generate QR tables, review KYB."
                  : "Setujui pengajuan gerai, atur PIN pemilik awal, buat meja QR otomatis, dan verifikasi KYB."
              },
              {
                href: "/legal",
                code: "05 / LEGAL PDP",
                title: isEn ? "Live Legal Documents & SHA-256 Hashes" : "Dokumen Hukum Aktif & Hash SHA-256",
                desc: isEn
                  ? "Terms of Service, UU PDP Privacy Policy, Refund Policy, and Tenant DPA."
                  : "Syarat Layanan, Kebijakan Privasi UU PDP, Kebijakan Pengembalian Dana, dan DPA Tenant."
              }
            ].map((ws) => (
              <Link
                key={ws.href}
                href={ws.href}
                className="block py-3 transition-colors hover:bg-muted/60 focus-visible:bg-muted/60"
              >
                <div className="flex items-baseline justify-between gap-2">
                  <span className="font-display text-sm font-bold text-ink">{ws.title}</span>
                  <span className="shrink-0 font-mono text-[11px] font-semibold text-accent-ink">
                    {ws.code}
                  </span>
                </div>
                <p className="mt-0.5 text-xs leading-relaxed text-subink">{ws.desc}</p>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Architecture Ledger Strip (Varied Composition for RHYTHM 2) */}
      <section className="rounded-xl border border-subtle bg-muted p-6">
        <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
          <div className="space-y-1.5">
            <p className="font-mono text-xs font-bold text-accent-ink">ISOLASI MULTI-TENANT</p>
            <h2 className="font-display text-base font-bold text-ink">
              PostgreSQL 18 Row-Level Security
            </h2>
            <p className="text-sm leading-relaxed text-subink">
              Setiap transaksi kasir, menu, meja QR, dan laporan keuangan dikunci pada konteks UUID gerai melalui role non-superuser <code className="font-mono text-xs">mycaffe_app</code>.
            </p>
          </div>
          <div className="space-y-1.5">
            <p className="font-mono text-xs font-bold text-accent-ink">PEMBAYARAN DUA JALUR</p>
            <h2 className="font-display text-base font-bold text-ink">
              Midtrans Core API &amp; Fallback QRIS Statis
            </h2>
            <p className="text-sm leading-relaxed text-subink">
              Gerai baru langsung dapat menguji Sandbox Midtrans atau memakai QRIS Statis Kasir pada hari pertama, lalu beralih ke Production setelah KYB disetujui.
            </p>
          </div>
          <div className="space-y-1.5">
            <p className="font-mono text-xs font-bold text-accent-ink">JARINGAN GERAI MANDIRI</p>
            <h2 className="font-display text-base font-bold text-ink">
              Generator MikroTik RouterOS v7 &amp; TCP:9100
            </h2>
            <p className="text-sm leading-relaxed text-subink">
              Pisahkan VLAN kasir dan printer thermal dari Wi-Fi pengunjung dengan skrip <code className="font-mono text-xs">.rsc</code> siap unduh, panduan Winbox, dan kebijakan SFTP.
            </p>
          </div>
        </div>
      </section>

      {/* Live Subscription Plans Section (/api/v1/public/plans) */}
      <section id="paket-langganan" className="space-y-5">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <h2 className="font-display text-2xl font-extrabold text-ink">
              {isEn ? "Official SaaS Subscription Plans" : "Paket Langganan Resmi Platform"}
            </h2>
            <p className="mt-1 text-sm text-subink">
              {isEn
                ? "Fetched live from /api/v1/public/plans in whole Indonesian Rupiah."
                : "Data harga dimuat langsung dari endpoint /api/v1/public/plans dalam Rupiah penuh."}
            </p>
          </div>

          <div
            role="group"
            aria-label="Siklus tagihan"
            className="inline-flex rounded-lg border border-subtle bg-surface p-1"
          >
            <button
              type="button"
              onClick={() => setBillingCycle("monthly")}
              aria-pressed={billingCycle === "monthly"}
              className={`min-h-[44px] rounded-md px-4 py-1.5 text-xs font-bold ${
                billingCycle === "monthly" ? "bg-brand text-brand-fg" : "text-ink"
              }`}
            >
              {isEn ? "Monthly" : "Bulanan"}
            </button>
            <button
              type="button"
              onClick={() => setBillingCycle("annual")}
              aria-pressed={billingCycle === "annual"}
              className={`min-h-[44px] rounded-md px-4 py-1.5 text-xs font-bold ${
                billingCycle === "annual" ? "bg-brand text-brand-fg" : "text-ink"
              }`}
            >
              {isEn ? "Annual" : "Tahunan"}
            </button>
          </div>
        </div>

        {plansLoading ? (
          <DataStateBox
            state="loading"
            title="Memuat paket langganan dari server"
            description="Mengambil struktur tarif terbaru dari /api/v1/public/plans..."
          />
        ) : plansError ? (
          <DataStateBox
            state="error"
            title="Paket langganan belum dapat dimuat"
            description={plansError}
            onRetry={loadLandingData}
          />
        ) : plans.length === 0 ? (
          <DataStateBox
            state="empty"
            title="Belum ada paket langganan aktif"
            description="Server mengembalikan daftar paket kosong. Hubungi administrator platform AzureDevID."
            onRetry={loadLandingData}
          />
        ) : (
          <div className="overflow-hidden rounded-xl border border-subtle bg-surface">
            <div className="divide-y divide-subtle">
              {plans.map((plan) => {
                const activePrice =
                  billingCycle === "annual" ? plan.annual_price_idr : plan.monthly_price_idr;
                return (
                  <div
                    key={plan.code}
                    className="grid grid-cols-1 items-center gap-4 p-5 lg:grid-cols-12"
                  >
                    <div className="space-y-1 lg:col-span-4">
                      <div className="flex items-center gap-2">
                        <h3 className="font-display text-lg font-bold text-ink">{plan.name}</h3>
                        <span className="rounded bg-muted px-2 py-0.5 font-mono text-xs text-subink">
                          {plan.code}
                        </span>
                      </div>
                      <p className="text-sm text-subink">
                        {plan.description ||
                          `Kapasitas hingga ${plan.max_tables} meja QR dan ${plan.max_staff} akun staf.`}
                      </p>
                    </div>

                    <div className="flex flex-wrap gap-2 text-xs text-ink lg:col-span-4">
                      <span className="rounded-md border border-subtle bg-muted px-2.5 py-1 font-medium">
                        Maks {plan.max_tables} Meja QR
                      </span>
                      <span className="rounded-md border border-subtle bg-muted px-2.5 py-1 font-medium">
                        Maks {plan.max_staff} Staf
                      </span>
                      <span className="rounded-md border border-subtle bg-muted px-2.5 py-1 font-medium">
                        Maks {plan.max_products} Menu
                      </span>
                      {plan.features.slice(0, 3).map((feat) => (
                        <span
                          key={feat}
                          className="rounded-md border border-subtle bg-canvas px-2.5 py-1 text-subink"
                        >
                          {feat}
                        </span>
                      ))}
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-4 lg:col-span-4 lg:justify-end">
                      <div className="text-left lg:text-right">
                        <p className="font-display text-2xl font-extrabold tabular-nums text-ink">
                          {formatIdr(activePrice)}
                        </p>
                        <p className="text-xs text-subink">
                          per {billingCycle === "annual" ? "tahun" : "bulan"}
                        </p>
                      </div>
                      <Link
                        href={`/onboarding?plan=${encodeURIComponent(plan.code)}&cycle=${billingCycle}`}
                        className="btn-primary min-h-[44px] px-4 py-2 text-xs font-bold"
                      >
                        Pilih {plan.name}
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </section>

      {/* Live Legal Governance Summary (/api/v1/public/legal) */}
      <section className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="font-display text-xl font-bold text-ink">
              {isEn
                ? "Cryptographically Hashed Legal & Privacy Governance"
                : "Tata Kelola Hukum & Pelindungan Data Pribadi (SHA-256)"}
            </h2>
            <p className="text-sm text-subink">
              {isEn
                ? "Verified live against /api/v1/public/legal for Midtrans Business Review & UU PDP compliance."
                : "Setiap persetujuan pendaftaran gerai mengikat hash SHA-256 dokumen resmi AzureDevID."}
            </p>
          </div>
          <Link href="/legal" className="btn-secondary min-h-[44px] px-4 py-2 text-xs font-semibold">
            {isEn ? "Read Full Legal Texts" : "Baca Dokumen Lengkap"}
          </Link>
        </div>

        {legalLoading ? (
          <DataStateBox
            state="loading"
            title="Memuat daftar dokumen hukum"
            description="Mengambil metadata SHA-256 dokumen dari /api/v1/public/legal..."
          />
        ) : legalError ? (
          <DataStateBox
            state="error"
            title="Ringkasan dokumen hukum belum tersedia"
            description={legalError}
            onRetry={loadLandingData}
          />
        ) : legalDocs.length === 0 ? (
          <DataStateBox
            state="empty"
            title="Belum ada dokumen hukum terdaftar"
            description="Endpoint /api/v1/public/legal belum mengembalikan dokumen."
            onRetry={loadLandingData}
          />
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {legalDocs.map((doc) => (
              <Link
                key={doc.doc_type}
                href={`/legal?doc=${encodeURIComponent(doc.doc_type)}`}
                className="rounded-xl border border-subtle bg-surface p-4 transition-colors hover:bg-muted/50"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-display text-sm font-bold text-ink">{doc.title}</span>
                  <span className="rounded bg-muted px-2 py-0.5 font-mono text-[11px] font-semibold text-subink">
                    v{doc.version}
                  </span>
                </div>
                <p className="mt-2 truncate font-mono text-xs text-subink">
                  SHA-256: {doc.content_sha256 || "terverifikasi"}
                </p>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

