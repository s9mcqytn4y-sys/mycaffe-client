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
  TOO_MANY_REQUESTS: "tooManyAttempts"
};

export function checkoutMessageKey(code: unknown): keyof Messages {
  return (typeof code === "string" && CHECKOUT_MESSAGES[code]) || "checkoutFailed";
}

export interface ActiveOrder {
  order: { id: string; number?: string; tracking: string | null; payKey: string };
  payment: PaymentInfo | null;
}

const activeKey = (token: string) => `cassy_active_order_${token}`;

export function readActiveOrder(token: string): ActiveOrder | null {
  try {
    const saved = sessionStorage.getItem(activeKey(token));
    return saved ? (JSON.parse(saved) as ActiveOrder) : null;
  } catch {
    return null;
  }
}

export function writeActiveOrder(token: string, active: ActiveOrder | null) {
  try {
    if (active) sessionStorage.setItem(activeKey(token), JSON.stringify(active));
    else sessionStorage.removeItem(activeKey(token));
  } catch {
    // Without storage a reload simply starts from the menu.
  }
}
