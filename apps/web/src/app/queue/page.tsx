"use client";

import React, { useCallback, useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";

interface QueueItem {
  id: string;
  queue_number: string;
  status: "queued" | "preparing" | "ready";
}

interface QueueApiResponse {
  preparing?: QueueItem[];
  ready?: QueueItem[];
  queue?: QueueItem[];
}

export default function QueueDisplayPage() {
  const [displayToken, setDisplayToken] = useState<string>("");
  const [preparingList, setPreparingList] = useState<QueueItem[]>([]);
  const [readyList, setReadyList] = useState<QueueItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [currentTime, setCurrentTime] = useState<string>("");

  useEffect(() => {
    if (typeof window !== "undefined") {
      const savedToken = localStorage.getItem("mycaffe_display_token") || "";
      setDisplayToken(savedToken);
    }
  }, []);

  useEffect(() => {
    const clockInterval = setInterval(() => {
      const now = new Date();
      setCurrentTime(
        now.toLocaleTimeString("id-ID", {
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit"
        })
      );
    }, 1000);
    return () => clearInterval(clockInterval);
  }, []);

  const fetchQueueOrders = useCallback(async () => {
    if (!displayToken) {
      setLoading(false);
      return;
    }
    try {
      const res = await apiFetch<QueueApiResponse>("/api/v1/public/queue", {
        bearerToken: displayToken
      }).catch(() => ({} as QueueApiResponse));

      const rawPreparing = Array.isArray(res.preparing) ? res.preparing : [];
      const rawReady = Array.isArray(res.ready) ? res.ready : [];

      setPreparingList(rawPreparing);
      setReadyList(rawReady);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Gagal memuat status antrean");
    } finally {
      setLoading(false);
    }
  }, [displayToken]);

  useEffect(() => {
    fetchQueueOrders();
    // Auto-refresh every 3 seconds for smooth real-time TV displays
    const interval = setInterval(() => {
      fetchQueueOrders();
    }, 3000);
    return () => clearInterval(interval);
  }, [fetchQueueOrders]);

  const handleSaveToken = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const tokenInput = String(formData.get("token") || "").trim();
    if (tokenInput) {
      setDisplayToken(tokenInput);
      localStorage.setItem("mycaffe_display_token", tokenInput);
      setLoading(true);
      fetchQueueOrders();
    }
  };

  if (!displayToken) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-canvas p-4 text-ink">
        <div className="w-full max-w-md rounded-2xl border border-subtle bg-surface p-6 shadow-xl space-y-4">
          <div className="text-center space-y-1">
            <h1 className="font-display text-xl font-bold text-ink">
              Konfigurasi Layar Antrean TV
            </h1>
            <p className="text-xs text-subink">
              Masukkan Token Display yang diterbitkan dari menu Backoffice Gerai untuk mengaktifkan antrean Smart TV.
            </p>
          </div>

          <form onSubmit={handleSaveToken} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-ink">Token Layar Display</label>
              <input
                name="token"
                type="text"
                required
                placeholder="dt_..."
                className="input-field mt-1 font-mono text-center text-sm"
              />
            </div>
            <button
              type="submit"
              className="btn-primary min-h-[48px] w-full py-2.5 text-xs font-bold"
            >
              Aktifkan Layar TV
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col bg-canvas text-ink select-none overflow-hidden">
      {/* Top Header Bar */}
      <header className="flex items-center justify-between border-b border-subtle bg-surface px-8 py-5 shadow-sm">
        <div className="flex items-center gap-4">
          <div className="h-4 w-4 rounded-full bg-emerald-500 animate-pulse" />
          <h1 className="font-display text-2xl font-black tracking-tight text-ink">
            Antrean Pesanan MyCaffe
          </h1>
        </div>

        <div className="flex items-center gap-6">
          <div className="font-mono text-2xl font-bold tabular-nums text-subink">
            {currentTime}
          </div>
          <button
            onClick={() => {
              localStorage.removeItem("mycaffe_display_token");
              setDisplayToken("");
            }}
            className="text-[11px] text-subink/60 hover:text-ink underline"
          >
            Ubah Token
          </button>
        </div>
      </header>

      {/* Main 2-Column Split: Sedang Disiapkan vs Siap Diambil */}
      <main className="grid flex-1 grid-cols-2 divide-x divide-subtle">
        {/* Column 1: Sedang Disiapkan */}
        <section className="flex flex-col p-8 space-y-6">
          <div className="flex items-center justify-between border-b border-subtle pb-4">
            <h2 className="font-display text-2xl sm:text-3xl font-extrabold text-subink">
              Sedang Disiapkan
            </h2>
            <span className="rounded-xl bg-muted px-4 py-1.5 font-mono text-sm font-bold text-subink">
              {preparingList.length} Pesanan
            </span>
          </div>

          {preparingList.length === 0 ? (
            <div className="flex flex-1 items-center justify-center text-subink/60 text-lg italic">
              Tidak ada antrean persiapan
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
              {preparingList.map((item) => (
                <div
                  key={item.id}
                  className="flex items-center justify-center rounded-2xl border border-subtle bg-surface p-6 shadow-sm"
                >
                  <span className="font-mono text-4xl sm:text-5xl font-black tabular-nums text-ink">
                    {item.queue_number}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Column 2: Siap Diambil */}
        <section className="flex flex-col p-8 space-y-6 bg-emerald-50/30">
          <div className="flex items-center justify-between border-b border-emerald-300 pb-4">
            <h2 className="font-display text-2xl sm:text-3xl font-black text-emerald-800">
              Silakan Diambil
            </h2>
            <span className="rounded-xl bg-emerald-200/80 px-4 py-1.5 font-mono text-sm font-black text-emerald-900">
              {readyList.length} Siap
            </span>
          </div>

          {readyList.length === 0 ? (
            <div className="flex flex-1 items-center justify-center text-emerald-800/60 text-lg italic">
              Menunggu pesanan selesai disajikan
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
              {readyList.map((item) => (
                <div
                  key={item.id}
                  className="flex items-center justify-center rounded-2xl border-2 border-emerald-500 bg-white p-6 shadow-md"
                >
                  <span className="font-mono text-5xl sm:text-6xl font-black tabular-nums text-emerald-700">
                    {item.queue_number}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>

      {/* Footer Banner */}
      <footer className="border-t border-subtle bg-surface px-8 py-3 text-center text-xs text-subink">
        Silakan tunjukkan struk digital atau nomor pesanan Anda saat mengambil minuman/makanan di konter.
      </footer>
    </div>
  );
}
