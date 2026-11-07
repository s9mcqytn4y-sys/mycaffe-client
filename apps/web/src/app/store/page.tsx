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
  const [email, setEmail] = useState<string>("owner@kopi-nusantara.demo");
  const [password, setPassword] = useState<string>("PasswordKopi123!");
  const [loginSlug, setLoginSlug] = useState<string>("kopi-nusantara");
  const [activeTab, setActiveTab] = useState<"catalog" | "payment" | "network">("catalog");
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
        token?: string;
        access_token?: string;
        store_slug?: string;
      }>("/api/v1/auth/login", {
        method: "POST",
        body: {
          store_slug: loginSlug.trim().toLowerCase(),
          email: email.trim().toLowerCase(),
          password
        }
      });

      const authToken = res.token || res.access_token || "";
      const confirmedSlug = res.store_slug || loginSlug.trim().toLowerCase();

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
            <div className="mb-4 rounded-xl border border-danger/30 bg-danger/10 p-3 text-xs text-danger">
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
                className="mt-1 w-full rounded-xl border border-subtle bg-canvas px-3 py-2 text-sm text-ink outline-none focus:border-brand-accent focus:ring-1 focus:ring-brand-accent"
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
                className="mt-1 w-full rounded-xl border border-subtle bg-canvas px-3 py-2 text-sm text-ink outline-none focus:border-brand-accent focus:ring-1 focus:ring-brand-accent"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-ink">Kata Sandi</label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Minimal 8 karakter"
                className="mt-1 w-full rounded-xl border border-subtle bg-canvas px-3 py-2 text-sm text-ink outline-none focus:border-brand-accent focus:ring-1 focus:ring-brand-accent"
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
              <Link href="/onboarding" className="font-semibold text-brand-accent hover:underline">
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
                  ? "bg-brand-primary text-brand-contrast"
                  : "bg-surface text-ink border border-subtle hover:bg-muted"
              }`}
            >
              Katalog, Meja & KDS
            </button>
            <button
              onClick={() => setActiveTab("payment")}
              className={`rounded-xl px-4 py-2 text-xs font-semibold transition-colors ${
                activeTab === "payment"
                  ? "bg-brand-primary text-brand-contrast"
                  : "bg-surface text-ink border border-subtle hover:bg-muted"
              }`}
            >
              Midtrans Gateway & E-Wallet
            </button>
            <button
              onClick={() => setActiveTab("network")}
              className={`rounded-xl px-4 py-2 text-xs font-semibold transition-colors ${
                activeTab === "network"
                  ? "bg-brand-primary text-brand-contrast"
                  : "bg-surface text-ink border border-subtle hover:bg-muted"
              }`}
            >
              Topologi Jaringan & MikroTik
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
        </div>
      )}
    </div>
  );
}
