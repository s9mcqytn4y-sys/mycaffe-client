"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { toOrderItems, type CartLine } from "./cart";
import { isFinal, readActiveOrder, reconnectDelay, writeActiveOrder, type ActiveOrder } from "./status";
import type { CatalogCategory, CatalogProduct, Quote, StoreCms, TableInfo } from "./types";

export type LoadState = "loading" | "ready" | "missing" | "failed";

export interface OrderData {
  table: TableInfo;
  cms: StoreCms;
  categories: CatalogCategory[];
  products: CatalogProduct[];
  resume: ActiveOrder | null;
}

export function useOrderData(token: string) {
  const [state, setState] = useState<LoadState>("loading");
  const [data, setData] = useState<OrderData | null>(null);

  const load = useCallback(async () => {
    setState("loading");
    try {
      const path = encodeURIComponent(token);
      const table = await apiFetch<TableInfo>(`/api/v1/public/qr/${path}`);
      const slug = encodeURIComponent(table.store_slug);
      const [catalog, cmsResponse] = await Promise.all([
        apiFetch<{ categories?: CatalogCategory[]; products?: CatalogProduct[] }>(`/api/v1/public/stores/${slug}/catalog`),
        apiFetch<{ cms?: StoreCms }>(`/api/v1/public/stores/${slug}/cms`).catch(() => ({ cms: {} }))
      ]);
      // A new session would orphan the saved order, so keep the cookie while this device still owns it.
      let resume = readActiveOrder(token);
      if (resume) {
        const owned = await apiFetch<{ orders?: { id: string }[] }>("/api/v1/public/guest/orders").catch(() => null);
        if (!owned?.orders?.some((o) => o.id === resume?.order.id)) {
          writeActiveOrder(token, null);
          resume = null;
        }
      }
      if (table.ordering_open && !resume) {
        await apiFetch(`/api/v1/public/qr/${path}/session`, { method: "POST", body: {} });
      }
      setData({
        table,
        cms: cmsResponse.cms ?? {},
        categories: catalog.categories ?? [],
        products: catalog.products ?? [],
        resume
      });
      setState("ready");
    } catch (err) {
      setState(err instanceof ApiError && err.status === 404 ? "missing" : "failed");
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load]);

  return { state, data, reload: load };
}

export function useQuote(slug: string | undefined, lines: CartLine[]) {
  const [quote, setQuote] = useState<Quote | null>(null);
  const [failed, setFailed] = useState(false);
  const [loading, setLoading] = useState(false);
  const requestId = useRef(0);

  const refresh = useCallback(async () => {
    const id = ++requestId.current;
    if (!slug || lines.length === 0) {
      setQuote(null);
      setFailed(false);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const result = await apiFetch<Quote>(`/api/v1/public/stores/${encodeURIComponent(slug)}/orders/quote`, {
        method: "POST",
        body: { order_type: "dine_in", items: toOrderItems(lines) }
      });
      if (id !== requestId.current) return;
      setQuote(result);
      setFailed(false);
    } catch {
      if (id !== requestId.current) return;
      setQuote(null);
      setFailed(true);
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }, [slug, lines]);

  useEffect(() => {
    const timer = setTimeout(() => void refresh(), 250);
    return () => clearTimeout(timer);
  }, [refresh]);

  return { quote, failed, loading, refresh };
}

interface Tracked {
  status?: string;
  payment_status?: string;
  order_number?: string;
}

const POLL_MS = 10_000;

/**
 * Follows an order over the guest socket and falls back to slow polling, which the rate limit can bear.
 * Both stop once the order is final; a dropped socket reconnects with backoff.
 */
export function useOrderProgress(trackingToken: string | null) {
  const [tracked, setTracked] = useState<Tracked | null>(null);

  useEffect(() => {
    if (!trackingToken) {
      setTracked(null);
      return;
    }
    let stopped = false;
    let socket: WebSocket | null = null;
    let retry: ReturnType<typeof setTimeout> | undefined;
    let attempt = 0;
    let timer: ReturnType<typeof setInterval> | undefined;
    // Coming back from an e-wallet app (or a hidden tab) checks at once instead of waiting for the next poll.
    const onVisible = () => {
      if (!document.hidden) void poll();
    };
    const stop = () => {
      stopped = true;
      clearInterval(timer);
      clearTimeout(retry);
      socket?.close();
      document.removeEventListener("visibilitychange", onVisible);
      window.removeEventListener("pageshow", onVisible);
    };
    const poll = async () => {
      try {
        const result = await apiFetch<Tracked>(`/api/v1/public/orders/track/${encodeURIComponent(trackingToken)}`);
        if (stopped) return;
        setTracked(result);
        if (isFinal(result)) stop();
      } catch {
        // The next poll or socket event retries.
      }
    };
    const reconnectLater = () => {
      if (!stopped) retry = setTimeout(() => void connect(), reconnectDelay(attempt++));
    };
    const connect = async () => {
      try {
        const { ticket } = await apiFetch<{ ticket: string }>("/api/v1/public/guest/ws-ticket", { method: "POST", body: {} });
        if (stopped) return;
        const scheme = window.location.protocol === "https:" ? "wss:" : "ws:";
        socket = new WebSocket(`${scheme}//${window.location.host}/ws?ticket=${encodeURIComponent(ticket)}`);
        socket.onopen = () => {
          attempt = 0;
        };
        socket.onmessage = () => void poll();
        socket.onclose = reconnectLater;
      } catch {
        reconnectLater();
      }
    };
    timer = setInterval(() => void poll(), POLL_MS);
    document.addEventListener("visibilitychange", onVisible);
    window.addEventListener("pageshow", onVisible);
    void poll();
    void connect();
    return stop;
  }, [trackingToken]);

  return tracked;
}
