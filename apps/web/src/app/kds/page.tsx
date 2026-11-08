"use client";

import React, { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { apiFetch, KdsTicket } from "@/lib/api";
import { useTenantWebSocket } from "@/lib/useTenantWebSocket";
import { DataStateBox, StatusBadge } from "@/components/Shell";

export default function DedicatedKdsPage() {
  const [tickets, setTickets] = useState<KdsTicket[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedStation, setSelectedStation] = useState<"all" | "bar" | "kitchen">("all");
  const [lastUpdated, setLastUpdated] = useState<string>("");
  const [token, setToken] = useState<string>("");
  const [storeSlug, setStoreSlug] = useState<string>("");

  useEffect(() => {
    if (typeof window !== "undefined") {
      const savedToken = localStorage.getItem("mycaffe_store_token") || "";
      const savedSlug = localStorage.getItem("mycaffe_store_slug") || "";
      setToken(savedToken);
      setStoreSlug(savedSlug);
    }
  }, []);

  const fetchKdsTickets = useCallback(async () => {
    if (!token) {
      setLoading(false);
      return;
    }
    try {
      const res = await apiFetch<{ tickets?: KdsTicket[] }>(
        "/api/v1/pos/kds/tickets?status=active",
        {
          bearerToken: token
        }
      ).catch(() => ({ tickets: [] }));

      const list = Array.isArray(res.tickets) ? res.tickets : [];
      setTickets(list);
      setLastUpdated(new Date().toLocaleTimeString("id-ID"));
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Gagal memperbarui tiket KDS");
    } finally {
      setLoading(false);
    }
  }, [token]);

  const wsUrl = typeof window !== "undefined" && token
    ? `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}/api/v1/realtime/ws?channels=kds&token=${encodeURIComponent(token)}`
    : "";

  const { status: wsStatus } = useTenantWebSocket({
    url: wsUrl,
    enabled: Boolean(token),
    onMessage: () => {
      fetchKdsTickets();
    },
    onFallbackPoll: fetchKdsTickets,
    fallbackPollingIntervalMs: 5000
  });

  const handleBumpTicket = async (ticketId: string, nextStatus: "preparing" | "ready" | "served") => {
    try {
      // Optimistic update
      setTickets((prev) =>
        prev
          .map((t) => (t.id === ticketId ? { ...t, status: nextStatus } : t))
          .filter((t) => nextStatus !== "served" || t.id !== ticketId)
      );

      await apiFetch(`/api/v1/pos/kds/tickets/${encodeURIComponent(ticketId)}/status`, {
        method: "PUT",
        bearerToken: token,
        body: { status: nextStatus }
      });
    } catch {
      // Rollback on failure
      fetchKdsTickets();
    }
  };

  const filteredTickets = tickets.filter((t) => {
    if (selectedStation === "all") return true;
    return (t.station || "bar") === selectedStation;
  });

  if (!token && !loading) {
    return (
      <div className="mx-auto max-w-lg py-16 px-4">
        <DataStateBox
          state="empty"
          title="Sesi Kasir/Dapur Diperlukan"
          description="Layar KDS memerlukan sesi login kasir atau pengelola gerai aktif. Silakan masuk terlebih dahulu."
        />
        <div className="mt-4 text-center">
          <Link href="/store" className="btn-primary min-h-[48px] px-6 py-2.5 text-xs font-bold">
            Masuk ke Konsol Gerai
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-canvas text-ink p-4 sm:p-6 space-y-6">
      {/* Top KDS Control Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-subtle bg-surface p-4 shadow-sm">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="font-display text-xl sm:text-2xl font-black tracking-tight text-ink">
              Kitchen Display System (KDS)
            </h1>
            <StatusBadge tone="ok" label={storeSlug || "Gerai Aktif"} />
            <StatusBadge
              tone={wsStatus === "connected" ? "ok" : wsStatus === "reconnecting" ? "warn" : "info"}
              label={
                wsStatus === "connected"
                  ? "Realtime WS"
                  : wsStatus === "reconnecting"
                  ? "Reconnecting..."
                  : "Polling Fallback (5s)"
              }
            />
          </div>
          <p className="mt-1 text-xs text-subink">
            Sinkronisasi otomatis | Pembaruan terakhir: {lastUpdated || "Menghubungkan..."}
          </p>
        </div>

        {/* Station Filters */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setSelectedStation("all")}
            className={`min-h-[44px] rounded-xl px-4 py-2 text-xs font-bold transition-colors ${
              selectedStation === "all"
                ? "bg-brand text-brand-fg"
                : "bg-canvas text-ink border border-subtle hover:bg-muted"
            }`}
          >
            Semua Antrean ({tickets.length})
          </button>
          <button
            onClick={() => setSelectedStation("bar")}
            className={`min-h-[44px] rounded-xl px-4 py-2 text-xs font-bold transition-colors ${
              selectedStation === "bar"
                ? "bg-brand text-brand-fg"
                : "bg-canvas text-ink border border-subtle hover:bg-muted"
            }`}
          >
            Bar / Barista
          </button>
          <button
            onClick={() => setSelectedStation("kitchen")}
            className={`min-h-[44px] rounded-xl px-4 py-2 text-xs font-bold transition-colors ${
              selectedStation === "kitchen"
                ? "bg-brand text-brand-fg"
                : "bg-canvas text-ink border border-subtle hover:bg-muted"
            }`}
          >
            Dapur Panas
          </button>
        </div>
      </div>

      {error && (
        <div className="rounded-xl border border-red-300 bg-red-50 p-3 text-xs text-red-700">
          {error}
        </div>
      )}

      {/* Ticket Grid */}
      {filteredTickets.length === 0 ? (
        <div className="py-20 text-center">
          <DataStateBox
            state="empty"
            title="Tidak Ada Pesanan Tertunda"
            description="Semua pesanan meja dan kasir telah selesai disajikan. Pesanan baru akan langsung tampil di sini."
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {filteredTickets.map((tk) => {
            const isReady = tk.status === "ready";
            const isPreparing = tk.status === "preparing";

            return (
              <div
                key={tk.id}
                className={`flex flex-col justify-between rounded-2xl border p-5 shadow-sm transition-all ${
                  isReady
                    ? "border-emerald-500 bg-emerald-50/20"
                    : "border-subtle bg-surface"
                }`}
              >
                <div>
                  {/* Ticket Header */}
                  <div className="flex items-start justify-between border-b border-subtle pb-3">
                    <div>
                      <span className="font-mono text-2xl font-black text-ink">
                        {tk.queue_number || tk.order_number || tk.id.slice(0, 6).toUpperCase()}
                      </span>
                      <p className="mt-0.5 text-xs font-semibold text-subink">
                        Meja: <span className="text-ink">{tk.table_number || "Takeaway"}</span>
                      </p>
                    </div>
                    <div className="text-right">
                      <StatusBadge
                        tone={isReady ? "ok" : isPreparing ? "warn" : "info"}
                        label={isReady ? "Siap Saji" : isPreparing ? "Sedang Dimasak" : "Menunggu"}
                      />
                      <p className="mt-1 font-mono text-[10px] text-subink">
                        Stasiun: {tk.station === "kitchen" ? "Dapur" : "Bar"}
                      </p>
                    </div>
                  </div>

                  {/* Order Item List */}
                  <div className="my-4 space-y-2">
                    {Array.isArray(tk.items) && tk.items.length > 0 ? (
                      <ul className="divide-y divide-subtle">
                        {tk.items.map((item, idx) => (
                          <li key={idx} className="py-2 first:pt-0 last:pb-0">
                            <div className="flex items-start justify-between gap-2">
                              <span className="font-display text-sm font-bold text-ink">
                                <span className="font-mono text-base font-black text-accent mr-1.5">
                                  {item.quantity}x
                                </span>
                                {item.name || item.product_name || "Item Menu"}
                              </span>
                            </div>
                            {item.notes && (
                              <p className="mt-0.5 text-xs italic text-subink bg-muted px-2 py-0.5 rounded">
                                Catatan: {item.notes}
                              </p>
                            )}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="text-xs text-subink italic">Rincian item sedang dimuat...</p>
                    )}
                  </div>
                </div>

                {/* Bump Action Buttons (>=56px touch target for busy kitchen staff) */}
                <div className="pt-3 border-t border-subtle flex gap-2">
                  {!isReady ? (
                    <button
                      type="button"
                      onClick={() => handleBumpTicket(tk.id, "ready")}
                      className="btn-primary min-h-[56px] w-full py-3 text-sm font-bold shadow"
                    >
                      Pesanan Siap Saji
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => handleBumpTicket(tk.id, "served")}
                      className="btn-secondary min-h-[56px] w-full py-3 text-sm font-bold text-emerald-700 border-emerald-400 bg-emerald-50 hover:bg-emerald-100 shadow"
                    >
                      Selesai Disajikan
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
