"use client";

import { useEffect, useReducer, useRef, useState } from "react";
import { cartReducer, type CartLine } from "./cart";
import { playBell, primeBell, readMuted, writeMuted } from "./bell";
import type { ServiceMode } from "./types";

export function useCartPersistence(token: string) {
  const [lines, dispatch] = useReducer(cartReducer, [] as CartLine[]);
  const attemptKey = useRef<string | null>(null);
  const storageKey = `cassy_cart_${token}`;
  const restored = useRef(false);

  useEffect(() => {
    try {
      const saved = sessionStorage.getItem(storageKey);
      if (saved) dispatch({ type: "replace", lines: JSON.parse(saved) as CartLine[] });
    } catch {
      // Blocked or corrupt storage means starting with an empty cart.
    }
    restored.current = true;
  }, [storageKey]);

  useEffect(() => {
    if (!restored.current) return;
    attemptKey.current = null;
    try {
      sessionStorage.setItem(storageKey, JSON.stringify(lines));
    } catch {
      // Ignore storage quota / private mode failures.
    }
  }, [lines, storageKey]);

  return { lines, dispatch, attemptKey };
}

export function useOrderBell(arrived: boolean, arrivedText: string, serviceMode: ServiceMode) {
  const [muted, setMuted] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const wasArrived = useRef(arrived);
  const pendingBell = useRef(false);

  useEffect(() => {
    setMuted(readMuted());
    window.addEventListener("pointerdown", primeBell, { once: true });
    return () => window.removeEventListener("pointerdown", primeBell);
  }, []);

  const toggleSound = () => {
    const next = !muted;
    writeMuted(next);
    setMuted(next);
  };

  useEffect(() => {
    const changed = arrived && !wasArrived.current;
    wasArrived.current = arrived;
    if (!changed) return;
    navigator.vibrate?.(200);
    if (!muted && !document.hidden) {
      void playBell(serviceMode === "table" ? "delivered" : "ready");
      pendingBell.current = false;
    } else if (!muted && document.hidden) {
      pendingBell.current = true;
    }
    setToast(arrivedText);
    const id = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(id);
  }, [arrived, arrivedText, muted, serviceMode]);

  useEffect(() => {
    const onVisible = () => {
      if (!document.hidden && pendingBell.current && !muted) {
        pendingBell.current = false;
        void playBell(serviceMode === "table" ? "delivered" : "ready");
      }
    };
    document.addEventListener("visibilitychange", onVisible);
    return () => document.removeEventListener("visibilitychange", onVisible);
  }, [muted, serviceMode]);

  return { muted, toggleSound, toast };
}

export function useOnlineStatus(onReconnect: () => void) {
  const [online, setOnline] = useState(true);
  useEffect(() => {
    const up = () => {
      setOnline(true);
      onReconnect();
    };
    const down = () => setOnline(false);
    setOnline(navigator.onLine);
    window.addEventListener("online", up);
    window.addEventListener("offline", down);
    return () => {
      window.removeEventListener("online", up);
      window.removeEventListener("offline", down);
    };
  }, [onReconnect]);
  return online;
}
