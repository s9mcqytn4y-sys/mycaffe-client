import type { Messages } from "@/lib/messages";
import type { PaymentInfo } from "./types";

export type OrderPhase = "awaiting" | "paid" | "cancelled" | "refunded";

export interface OrderProgress {
  status?: string;
  payment_status?: string;
}

export function orderPhase(progress?: OrderProgress | null): OrderPhase {
  if (progress?.status === "cancelled") return "cancelled";
  switch (progress?.payment_status) {
    case "refunded":
    case "chargeback":
      return "refunded";
    case "paid":
    case "partially_refunded":
    case "partial_chargeback":
      return "paid";
    default:
      return "awaiting";
  }
}

// Money may already be in when an order is cancelled: such a guest needs the staff, not a retry.
export function moneyTaken(progress?: OrderProgress | null): boolean {
  return progress?.payment_status === "paid" || progress?.payment_status === "partially_refunded";
}

const CHECKOUT_MESSAGES: Record<string, keyof Messages> = {
  GUEST_SESSION_EXPIRED: "sessionExpired",
  QUOTE_CHANGED: "priceChanged",
  UNAVAILABLE: "itemUnavailable",
  QR_ORDERING_PAUSED: "orderingPaused",
  STORE_CLOSED: "storeClosed",
  TABLE_INACTIVE: "tableInactive",
  UNPAID_ORDER_LIMIT: "unpaidLimit",
  TOO_MANY_REQUESTS: "tooManyAttempts",
  INVALID_PAYMENT: "methodUnavailable",
  MAINTENANCE: "maintenanceShort"
};

// The link comes from Midtrans through our API; still never hand a script or data URL to the browser.
export function safeDeeplink(url?: string): string | null {
  if (!url) return null;
  try {
    const scheme = new URL(url).protocol;
    return ["javascript:", "data:", "file:", "vbscript:", "blob:"].includes(scheme) ? null : url;
  } catch {
    return null;
  }
}

export function checkoutMessageKey(code: unknown): keyof Messages {
  return (typeof code === "string" && CHECKOUT_MESSAGES[code]) || "checkoutFailed";
}

/** 0 received, 1 preparing, 2 ready, 3 served: how far a paid order has moved through the kitchen. */
export function progressStep(status?: string): number {
  if (status === "preparing") return 1;
  if (status === "ready") return 2;
  if (status === "served" || status === "completed") return 3;
  return 0;
}

// After these the tracker has nothing left to report.
export function isFinal(progress?: OrderProgress | null): boolean {
  return ["served", "completed", "cancelled"].includes(progress?.status ?? "") || orderPhase(progress) === "refunded";
}

export const reconnectDelay = (attempt: number) => Math.min(30_000, 1000 * 2 ** attempt);

export interface ActiveOrder {
  order: { id: string; number?: string; tracking: string | null; payKey: string };
  payment: PaymentInfo | null;
}

// Matches the backend guest session TTL; the server still has to confirm ownership on every load.
const ACTIVE_ORDER_TTL_MS = 3 * 60 * 60 * 1000;
const activeKey = (token: string) => `cassy_active_order_${token}`;

export function readActiveOrder(token: string, now = Date.now()): ActiveOrder | null {
  try {
    const saved = localStorage.getItem(activeKey(token));
    if (!saved) return null;
    const { savedAt, ...active } = JSON.parse(saved) as ActiveOrder & { savedAt?: number };
    if (typeof savedAt !== "number" || now - savedAt > ACTIVE_ORDER_TTL_MS) {
      localStorage.removeItem(activeKey(token));
      return null;
    }
    return active;
  } catch {
    return null;
  }
}

// Kept in localStorage so closing the tab before paying does not lose the order.
export function writeActiveOrder(token: string, active: ActiveOrder | null, now = Date.now()) {
  try {
    if (active) localStorage.setItem(activeKey(token), JSON.stringify({ ...active, savedAt: now }));
    else localStorage.removeItem(activeKey(token));
  } catch {
    // Without storage a reload simply starts from the menu.
  }
}
