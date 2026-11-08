"use client";

import React, { useCallback, useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { useTenantWebSocket } from "@/lib/useTenantWebSocket";

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
      const savedToken =
        localStorage.getItem("cassy_display_token") ||
        localStorage.getItem("mycaffe_display_token") ||
        "";
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

  const wsUrl = typeof window !== "undefined" && displayToken
    ? `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}/api/v1/realtime/ws?channels=queue&token=${encodeURIComponent(displayToken)}`
    : "";

  const { status: wsStatus } = useTenantWebSocket({
    url: wsUrl,
    enabled: Boolean(displayToken),
    onMessage: () => {
      fetchQueueOrders();
    },
    onFallbackPoll: fetchQueueOrders,
    fallbackPollingIntervalMs: 3000
  });

  const handleSaveToken = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const tokenInput = String(formData.get("token") || "").trim();
    if (tokenInput) {
      setDisplayToken(tokenInput);
      localStorage.setItem("cassy_display_token", tokenInput);
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
    <div className="flex h-screen flex-col bg-canvas text-ink p-6 select-none overflow-hidden">
      {/* Top TV Bar */}
      <header className="flex items-center justify-between border-b border-subtle pb-4">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand font-display text-xl font-black text-brand-fg">
            C
          </div>
          <div>
            <h1 className="font-display text-2xl font-black tracking-tight text-ink">
              Nomor Panggilan Antrean
            </h1>
            <p className="text-xs text-subink">
              Silakan ambil pesanan Anda di meja pengambilan (Pickup Counter) saat nomor Anda muncul di kolom hijau.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            <span
              className={`h-2.5 w-2.5 rounded-full ${
                wsStatus === "connected"
                  ? "bg-emerald-500 animate-pulse"
                  : wsStatus === "reconnecting"
                  ? "bg-amber-500 animate-ping"
                  : "bg-slate-400"
              }`}
            />
            <span className="font-mono text-xs text-subink">
              {wsStatus === "connected" ? "LIVE WS" : "POLLING"}
            </span>
          </div>
          <div className="text-right">
            <div className="font-mono text-3xl font-black text-ink">{currentTime}</div>
            <div className="text-xs font-semibold text-accent">WIB (Jakarta)</div>
          </div>
        </div>
      </header>

      {/* Main Split Panels */}
      <div className="mt-6 grid flex-1 grid-cols-12 gap-6 overflow-hidden">
        {/* Panel Sedang Dipersiapkan (Barista/Kitchen) */}
        <div className="col-span-5 flex flex-col rounded-3xl border border-subtle bg-surface p-6 shadow-sm overflow-hidden">
          <div className="flex items-center justify-between border-b border-subtle pb-3">
            <h2 className="font-display text-xl font-black text-amber-700 dark:text-amber-400">
              Sedang Disiapkan
            </h2>
            <span className="rounded-full bg-amber-100 px-3 py-1 font-mono text-xs font-bold text-amber-800">
              {preparingList.length} Pesanan
            </span>
          </div>

          <div className="mt-4 flex-1 overflow-y-auto pr-1">
            {preparingList.length === 0 ? (
              <div className="flex h-full items-center justify-center text-center text-sm text-subink italic">
                Semua pesanan yang masuk telah siap disajikan
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-3">
                {preparingList.map((item) => (
                  <div
                    key={item.id}
                    className="flex min-h-[72px] items-center justify-center rounded-2xl border border-amber-200 bg-amber-50/50 p-4 shadow-sm"
                  >
                    <span className="font-mono text-3xl font-black text-ink">
                      {item.queue_number}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Panel Silakan Ambil / Siap Saji */}
        <div className="col-span-7 flex flex-col rounded-3xl border-2 border-emerald-500 bg-emerald-50/20 p-6 shadow-md overflow-hidden">
          <div className="flex items-center justify-between border-b border-emerald-200 pb-3">
            <h2 className="font-display text-2xl font-black text-emerald-800 dark:text-emerald-400">
              Silakan Ambil Pesanan
            </h2>
            <span className="rounded-full bg-emerald-100 px-3 py-1 font-mono text-xs font-bold text-emerald-800">
              {readyList.length} Siap
            </span>
          </div>

          <div className="mt-4 flex-1 overflow-y-auto pr-1">
            {readyList.length === 0 ? (
              <div className="flex h-full items-center justify-center text-center text-sm text-subink italic">
                Menunggu pesanan selesai disiapkan...
              </div>
            ) : (
              <div className="grid grid-cols-3 gap-4">
                {readyList.map((item, idx) => (
                  <div
                    key={item.id}
                    className={`flex min-h-[96px] items-center justify-center rounded-2xl border-2 border-emerald-500 bg-surface p-4 shadow-lg ${
                      idx === 0 ? "animate-bounce" : ""
                    }`}
                  >
                    <span className="font-mono text-4xl font-black text-emerald-700">
                      {item.queue_number}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
