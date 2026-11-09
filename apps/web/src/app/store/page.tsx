"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { apiFetch } from "@/lib/api";
import { TabCatalogTablesKds } from "@/components/pos/TabCatalogTablesKds";
import { TabMidtransWallets } from "@/components/pos/TabMidtransWallets";
import { TabNetworkMikrotik } from "@/components/pos/TabNetworkMikrotik";
import { StatusBadge } from "@/components/Shell";

const TOKEN_KEY = "mycaffe_store_token";
const SLUG_KEY = "mycaffe_store_slug";

export default function StoreConsolePage() {
  const [token, setToken] = useState<string>("");
  const [storeSlug, setStoreSlug] = useState<string>("");
  const [email, setEmail] = useState<string>("");
  const [pin, setPin] = useState<string>("");
  const [loginSlug, setLoginSlug] = useState<string>("");
  const [activeTab, setActiveTab] = useState<"catalog" | "payment" | "network" | "privacy">("catalog");
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const savedToken = localStorage.getItem(TOKEN_KEY) || "";
    const savedSlug = localStorage.getItem(SLUG_KEY) || "";
    if (savedToken) setToken(savedToken);
    if (savedSlug) setStoreSlug(savedSlug);
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<{
        access_token?: string;
      }>("/api/v1/auth/staff/login", {
        method: "POST",
        body: {
          store_slug: loginSlug.trim().toLowerCase(),
          email: email.trim().toLowerCase(),
          pin
        }
      });

      const authToken = res.access_token || "";
      const confirmedSlug = loginSlug.trim().toLowerCase();

      if (!authToken) {
        throw new Error("Respon autentikasi tidak menyertakan token sesi");
      }

      setToken(authToken);
      setStoreSlug(confirmedSlug);
      localStorage.setItem(TOKEN_KEY, authToken);
      localStorage.setItem(SLUG_KEY, confirmedSlug);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Gagal masuk ke konsol gerai");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    setToken("");
    setStoreSlug("");
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(SLUG_KEY);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-subtle pb-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="font-display text-2xl font-bold tracking-tight text-ink">
              Konsol Gerai & POS MyCaffe
            </h1>
            {token ? (
              <StatusBadge tone="ok" label={`Gerai: ${storeSlug}`} />
            ) : (
              <StatusBadge tone="info" label="Sesi Tamu" />
            )}
          </div>
          <p className="mt-1 text-sm text-subink">
            Pusat kendali operasional gerai: katalog menu, meja QR, KDS dapur, kredensial Midtrans, dan otomatisasi jaringan MikroTik.
          </p>
        </div>

        {token && (
          <div className="flex items-center gap-3">
            <button
              onClick={handleLogout}
              className="btn-secondary min-h-[40px] px-3 py-1.5 text-xs font-semibold"
            >
              Keluar Sesi
            </button>
          </div>
        )}
      </div>

      {!token ? (
        /* Login Card */
        <div className="mx-auto max-w-md rounded-2xl border border-subtle bg-surface p-6 shadow-sm">
          <div className="mb-6 space-y-1 text-center">
            <h2 className="font-display text-xl font-bold text-ink">Masuk Akun Pengelola Gerai</h2>
            <p className="text-xs text-subink">
              Gunakan kredensial yang didaftarkan pada tahap orientasi gerai.
            </p>
          </div>

          {error && (
            <div className="mb-4 rounded-xl border border-red-300 bg-red-50 p-3 text-xs text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-ink">Slug Gerai</label>
              <input
                type="text"
                required
                value={loginSlug}
                onChange={(e) => setLoginSlug(e.target.value)}
                placeholder="contoh: kopi-nusantara"
                className="mt-1 w-full rounded-xl border border-subtle bg-canvas px-3 py-2 text-sm text-ink outline-none focus:border-accent focus:ring-1 focus:ring-accent"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-ink">Email Penanggung Jawab</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="owner@gerai.com"
                className="mt-1 w-full rounded-xl border border-subtle bg-canvas px-3 py-2 text-sm text-ink outline-none focus:border-accent focus:ring-1 focus:ring-accent"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-ink">PIN</label>
              <input
                type="password"
                inputMode="numeric"
                autoComplete="current-password"
                required
                pattern="[0-9]{6,12}"
                title="PIN 6 sampai 12 angka"
                value={pin}
                onChange={(e) => setPin(e.target.value)}
                placeholder="6 sampai 12 angka"
                className="mt-1 w-full rounded-xl border border-subtle bg-canvas px-3 py-2 text-sm text-ink outline-none focus:border-accent focus:ring-1 focus:ring-accent"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn-primary min-h-[44px] w-full py-2.5 text-xs font-bold"
            >
              {loading ? "Memverifikasi..." : "Masuk ke Konsol Gerai"}
            </button>
          </form>

          <div className="mt-6 border-t border-subtle pt-4 text-center">
            <p className="text-xs text-subink">
              Belum mendaftarkan gerai Anda?{" "}
              <Link href="/onboarding" className="font-semibold text-accent hover:underline">
                Daftar Gerai Baru
              </Link>
            </p>
          </div>
        </div>
      ) : (
        /* Authenticated Store Tabs */
        <div className="space-y-6">
          {/* Navigation Bar */}
          <div className="flex flex-wrap gap-2 border-b border-subtle pb-2">
            <button
              onClick={() => setActiveTab("catalog")}
              className={`rounded-xl px-4 py-2 text-xs font-semibold transition-colors ${
                activeTab === "catalog"
                  ? "bg-brand text-brand-fg"
                  : "bg-surface text-ink border border-subtle hover:bg-muted"
              }`}
            >
              Katalog, Meja & KDS
            </button>
            <button
              onClick={() => setActiveTab("payment")}
              className={`rounded-xl px-4 py-2 text-xs font-semibold transition-colors ${
                activeTab === "payment"
                  ? "bg-brand text-brand-fg"
                  : "bg-surface text-ink border border-subtle hover:bg-muted"
              }`}
            >
              Midtrans Gateway & E-Wallet
            </button>
            <button
              onClick={() => setActiveTab("network")}
              className={`rounded-xl px-4 py-2 text-xs font-semibold transition-colors ${
                activeTab === "network"
                  ? "bg-brand text-brand-fg"
                  : "bg-surface text-ink border border-subtle hover:bg-muted"
              }`}
            >
              Topologi Jaringan & MikroTik
            </button>
            <button
              onClick={() => setActiveTab("privacy")}
              className={`rounded-xl px-4 py-2 text-xs font-semibold transition-colors ${
                activeTab === "privacy"
                  ? "bg-brand text-brand-fg"
                  : "bg-surface text-ink border border-subtle hover:bg-muted"
              }`}
            >
              Privasi & Data (UU PDP)
            </button>
          </div>

          {/* Active Tab Panel */}
          {activeTab === "catalog" && (
            <TabCatalogTablesKds token={token} storeSlug={storeSlug} />
          )}

          {activeTab === "payment" && (
            <TabMidtransWallets token={token} />
          )}

          {activeTab === "network" && (
            <TabNetworkMikrotik token={token} storeSlug={storeSlug} />
          )}

          {activeTab === "privacy" && (
            <div className="space-y-6 rounded-2xl border border-subtle bg-surface p-6 shadow-sm">
              <div className="border-b border-subtle pb-4">
                <h2 className="font-display text-lg font-bold text-ink">
                  Kedaulatan & Perlindungan Data Pribadi Gerai (UU PDP No. 27/2022)
                </h2>
                <p className="mt-1 text-xs text-subink">
                  Data Anda adalah hak milik Anda seutuhnya. Kami menjamin kerahasiaan data operasional dan tidak menjual data transaksi kepada pihak manapun.
                </p>
              </div>

              {/* Data Category Transparency Table */}
              <div className="space-y-3">
                <h3 className="font-display text-sm font-bold text-ink">Tujuan Penggunaan Data</h3>
                <div className="overflow-x-auto rounded-xl border border-subtle">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-canvas border-b border-subtle text-subink font-semibold">
                      <tr>
                        <th className="p-3">Kategori Data</th>
                        <th className="p-3">Tujuan Pemrosesan</th>
                        <th className="p-3">Kerahasiaan & Keamanan</th>
                        <th className="p-3">Masa Retensi</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-subtle text-ink">
                      <tr>
                        <td className="p-3 font-semibold">Transaksi & Pembayaran</td>
                        <td className="p-3 text-subink">Perhitungan laporan laba rugi, penyesuaian stok, rekonsiliasi kasir.</td>
                        <td className="p-3 text-subink">Row Level Security (RLS) PostgreSQL 18 terisolasi per tenant.</td>
                        <td className="p-3 font-mono text-[11px]">10 Tahun (Kepatuhan Pajak UU KUP)</td>
                      </tr>
                      <tr>
                        <td className="p-3 font-semibold">Kredensial Midtrans / SMTP</td>
                        <td className="p-3 text-subink">Penyelesaian transaksi pembayaran dan pengiriman email struk langsung via akun Anda.</td>
                        <td className="p-3 text-subink">Terenkripsi AES-256-GCM. Tidak pernah ditampilkan kembali di antarmuka.</td>
                        <td className="p-3 font-mono text-[11px]">Selama Akun Aktif</td>
                      </tr>
                      <tr>
                        <td className="p-3 font-semibold">Identitas Staf & Akun</td>
                        <td className="p-3 text-subink">Autentikasi sesi kasir, hak akses RBAC, jejak audit operasional.</td>
                        <td className="p-3 text-subink">PIN di-hash menggunakan Argon2id dengan salt acak per pengguna.</td>
                        <td className="p-3 font-mono text-[11px]">Dihapus Saat Tutup Akun</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Data Portability (Export) */}
              <div className="rounded-xl border border-subtle bg-canvas p-4 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <h4 className="font-display text-sm font-bold text-ink">Unduh Salinan Data Gerai (Portabilitas Data)</h4>
                    <p className="text-xs text-subink mt-0.5">
                      Ekspor seluruh data katalog, riwayat pesanan, dan pergerakan kas gerai dalam format arsip JSON & CSV terbuka.
                    </p>
                  </div>
                  <StatusBadge tone="info" label="Segera tersedia" />
                </div>
              </div>

              {/* Account Clearance & Closure Notice */}
              <div className="rounded-xl border border-subtle p-4 space-y-2">
                <h4 className="font-display text-sm font-bold text-ink">Penutupan Akun & Pemusnahan Kunci</h4>
                <p className="text-xs text-subink leading-relaxed">
                  Jika Anda memutuskan untuk berhenti menggunakan MyCaffe, Anda dapat mengajukan permintaan penutupan akun. Sistem memberlakukan masa tenggang 7 hari (*cooling-off period*), menonaktifkan kunci enkripsi, dan menghapus seluruh data PII staf/pelanggan secara permanen.
                </p>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
