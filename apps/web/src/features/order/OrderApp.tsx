"use client";

import React, { useEffect, useMemo, useReducer, useRef, useState } from "react";
import { useLocale } from "@/components/Providers";
import { DataStateBox } from "@/components/Shell";
import { ApiError, apiFetch } from "@/lib/api";
import { brandVariables } from "@/lib/brand";
import { formatIdr } from "@/lib/format";
import { CartSheet } from "./CartSheet";
import { cartReducer, toOrderItems, type CartLine } from "./cart";
import { PaymentView } from "./PaymentView";
import { ProductOptionsSheet } from "./ProductOptionsSheet";
import { checkoutMessageKey, moneyTaken, orderPhase, writeActiveOrder, type ActiveOrder } from "./status";
import type { CatalogProduct, PaymentInfo } from "./types";
import { useOrderData, useOrderProgress, useQuote } from "./useOrderData";

function defaultVariant(product: CatalogProduct) {
  return product.variants.find((v) => v.is_default && v.available) ?? product.variants.find((v) => v.available);
}

function ProductRow({
  product,
  canOrder,
  onChoose,
  onQuickAdd
}: {
  product: CatalogProduct;
  canOrder: boolean;
  onChoose: () => void;
  onQuickAdd: () => void;
}) {
  const { t } = useLocale();
  const variant = defaultVariant(product);
  const soldOut = !product.available || !variant;
  const bundled = (product.bundle_slots?.length ?? 0) > 0;
  const needsChoice = product.variants.length > 1 || product.modifier_groups.length > 0;

  return (
    <li className="flex gap-4 py-4">
      {product.image_url ? (
        <img
          src={product.image_url}
          alt={product.name}
          width={96}
          height={96}
          loading="lazy"
          className="h-24 w-24 shrink-0 rounded-lg object-cover"
        />
      ) : null}
      <div className="min-w-0 flex-1">
        <h3 className="text-base font-bold">{product.name}</h3>
        {product.description ? <p className="mt-0.5 line-clamp-2 text-sm text-subink">{product.description}</p> : null}
        <div className="mt-2 flex items-center justify-between gap-3">
          <span className="font-bold">{variant ? formatIdr(variant.price) : ""}</span>
          {soldOut ? (
            <span className="text-sm font-semibold text-subink">{t.soldOut}</span>
          ) : bundled ? (
            <span className="text-sm text-subink">{t.askStaff}</span>
          ) : canOrder ? (
            <button
              type="button"
              aria-label={`${needsChoice ? t.choose : t.add} ${product.name}`}
              onClick={needsChoice ? onChoose : onQuickAdd}
              className="btn-primary min-h-[44px] px-4 text-sm font-bold"
            >
              {needsChoice ? t.choose : t.add}
            </button>
          ) : null}
        </div>
      </div>
    </li>
  );
}

export function OrderApp({ token }: { token: string }) {
  const { t, locale } = useLocale();
  const { state, data, reload } = useOrderData(token);
  const [lines, dispatch] = useReducer(cartReducer, [] as CartLine[]);
  const [category, setCategory] = useState("all");
  const [optionsFor, setOptionsFor] = useState<CatalogProduct | null>(null);
  const [cartOpen, setCartOpen] = useState(false);
  const [guestName, setGuestName] = useState("");
  const [placing, setPlacing] = useState(false);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const [order, setOrder] = useState<ActiveOrder["order"] | null>(null);
  const [payment, setPayment] = useState<PaymentInfo | null>(null);
  const [viewingPayment, setViewingPayment] = useState(false);
  const attemptKey = useRef<string | null>(null);

  const storageKey = `cassy_cart_${token}`;
  const restored = useRef(false);
  useEffect(() => {
    try {
      const saved = sessionStorage.getItem(storageKey);
      if (saved) dispatch({ type: "replace", lines: JSON.parse(saved) as CartLine[] });
    } catch {
      // A blocked or corrupt store just means an empty cart.
    }
    restored.current = true;
  }, [storageKey]);
  useEffect(() => {
    if (!restored.current) return;
    attemptKey.current = null;
    try {
      sessionStorage.setItem(storageKey, JSON.stringify(lines));
    } catch {
      // See above.
    }
  }, [lines, storageKey]);

  const { quote, failed, loading, refresh } = useQuote(data?.table.store_slug, lines);
  const tracked = useOrderProgress(order?.tracking ?? null);
  const phase = orderPhase(tracked);

  const initiatePayment = async (orderId: string, key: string) => {
    const { payment: created } = await apiFetch<{ payment: PaymentInfo }>(
      `/api/v1/public/guest/orders/${encodeURIComponent(orderId)}/payments`,
      { method: "POST", body: { idempotency_key: key } }
    );
    setPayment(created);
    setViewingPayment(true);
  };

  const resumed = useRef(false);
  useEffect(() => {
    if (resumed.current || !data?.resume) return;
    resumed.current = true;
    const { order: saved, payment: savedPayment } = data.resume;
    setOrder(saved);
    if (savedPayment) {
      setPayment(savedPayment);
      setViewingPayment(true);
    } else {
      initiatePayment(saved.id, saved.payKey).catch(() => setCheckoutError(t.checkoutFailed));
    }
    // initiatePayment only uses stable setters; the ref limits this to one run.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  useEffect(() => {
    // A served order is done; a cancelled one stays so a guest who paid still sees where to ask for the refund.
    const served = tracked?.status === "served" || tracked?.status === "completed";
    if (state === "ready") writeActiveOrder(token, order && !served ? { order, payment } : null);
  }, [state, token, order, payment, tracked?.status]);

  const products = useMemo(
    () => (data?.products ?? []).filter((p) => category === "all" || p.category_id === category),
    [data, category]
  );
  const itemCount = lines.reduce((sum, l) => sum + l.quantity, 0);

  if (state === "loading") return <div className="p-5"><DataStateBox state="loading" title={t.loadingTitle} description={t.loadingBody} /></div>;
  if (state === "missing") return <div className="p-5"><DataStateBox state="error" title={t.tableMissingTitle} description={t.tableMissingBody} /></div>;
  if (state === "failed" || !data) {
    return <div className="p-5"><DataStateBox state="error" title={t.loadFailedTitle} description={t.loadFailedBody} onRetry={reload} /></div>;
  }

  const { table, cms, categories } = data;
  const announcement = locale === "en-US" ? cms.announcement_banner_en : cms.announcement_banner_id;
  const brand = brandVariables(cms.primary_hex) as React.CSSProperties;

  const checkout = async () => {
    if (!quote) return;
    setPlacing(true);
    setCheckoutError(null);
    // One key per cart state, so a retry after a dropped connection cannot create a second order.
    const key = (attemptKey.current ??= crypto.randomUUID());
    try {
      const created = await apiFetch<{ order: { id: string; order_number?: string }; tracking_token?: string }>(
        "/api/v1/public/guest/orders",
        {
          method: "POST",
          body: {
            idempotency_key: key,
            customer_name: guestName.trim() || t.guestDefault,
            expected_quote_fingerprint: quote.quote_fingerprint,
            items: toOrderItems(lines)
          }
        }
      );
      const payKey = `pay_${key}`;
      setOrder({ id: created.order.id, number: created.order.order_number, tracking: created.tracking_token ?? null, payKey });
      dispatch({ type: "clear" });
      setCartOpen(false);
      await initiatePayment(created.order.id, payKey);
    } catch (err) {
      const code = err instanceof ApiError ? (err.details as { code?: unknown } | undefined)?.code : undefined;
      setCheckoutError(t[checkoutMessageKey(code)]);
      if (code === "GUEST_SESSION_EXPIRED") void reload();
      else if (code === "QUOTE_CHANGED" || code === "UNAVAILABLE") void refresh();
    } finally {
      setPlacing(false);
    }
  };

  const newQr = async () => {
    if (!order) return;
    setPlacing(true);
    try {
      await initiatePayment(order.id, `pay_${crypto.randomUUID()}`);
    } catch {
      setCheckoutError(t.checkoutFailed);
    } finally {
      setPlacing(false);
    }
  };

  const resumePayment = async () => {
    if (!order) return;
    setCheckoutError(null);
    if (payment) {
      setViewingPayment(true);
      return;
    }
    setPlacing(true);
    try {
      await initiatePayment(order.id, order.payKey);
    } catch {
      setCheckoutError(t.checkoutFailed);
    } finally {
      setPlacing(false);
    }
  };

  const startOver = () => {
    dispatch({ type: "clear" });
    setOrder(null);
    setPayment(null);
    setViewingPayment(false);
    setCheckoutError(null);
  };

  if (payment && order && viewingPayment) {
    return (
      <div style={brand}>
        <PaymentView
          payment={payment}
          phase={phase}
          moneyTaken={moneyTaken(tracked)}
          orderId={order.id}
          orderNumber={tracked?.order_number ?? order.number}
          status={tracked?.status}
          retrying={placing}
          onNewQr={newQr}
          onBack={() => setViewingPayment(false)}
          onOrderAgain={startOver}
        />
      </div>
    );
  }

  return (
    <div style={brand} className="mx-auto max-w-lg pb-28">
      {cms.hero_banner_url ? (
        <img
          src={cms.hero_banner_url}
          alt=""
          width={1280}
          height={480}
          fetchPriority="high"
          className="aspect-[8/3] w-full object-cover"
        />
      ) : null}
      <header className="px-5 pt-5">
        <h1 className="text-2xl font-bold">{table.store_name}</h1>
        <p className="text-sm text-subink">{table.label || `${t.table} ${table.table_number}`}</p>
        {announcement ? <p className="mt-2 text-sm">{announcement}</p> : null}
      </header>

      {order && !viewingPayment ? (
        <div className="mt-4 px-5">
          <button type="button" disabled={placing} onClick={resumePayment} className="btn-primary min-h-[56px] w-full text-base font-bold">
            {t.resumePayment}
          </button>
          {checkoutError ? <p role="alert" className="mt-2 text-sm font-semibold text-[var(--state-err-fg)]">{checkoutError}</p> : null}
        </div>
      ) : null}

      {!table.ordering_open ? (
        <div className="mt-4 px-5">
          <DataStateBox state="empty" title={t.closedTitle} description={t.closedBody} />
        </div>
      ) : null}

      <nav aria-label={t.categories} className="sticky top-0 z-10 mt-3 flex gap-2 overflow-x-auto bg-canvas px-5 py-2">
        {[{ id: "all", name: t.allMenu }, ...categories].map((c) => (
          <button
            key={c.id}
            type="button"
            aria-pressed={category === c.id}
            onClick={() => setCategory(c.id)}
            className={`min-h-[44px] shrink-0 rounded-lg px-3 text-sm font-semibold ${
              category === c.id ? "bg-brand text-brand-fg" : "text-ink"
            }`}
          >
            {c.name}
          </button>
        ))}
      </nav>

      <div className="px-5">
        {products.length === 0 ? (
          <div className="mt-4"><DataStateBox state="empty" title={t.emptyTitle} description={t.emptyBody} /></div>
        ) : (
          <ul>
            {products.map((product) => (
              <ProductRow
                key={product.id}
                product={product}
                canOrder={table.ordering_open}
                onChoose={() => setOptionsFor(product)}
                onQuickAdd={() =>
                  dispatch({
                    type: "add",
                    line: { productId: product.id, variantId: defaultVariant(product)?.id, name: product.name, options: [], quantity: 1, notes: "" }
                  })
                }
              />
            ))}
          </ul>
        )}
      </div>

      {itemCount > 0 ? (
        <div className="fixed inset-x-0 bottom-0 z-20 flex justify-center bg-surface p-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
          <button type="button" onClick={() => setCartOpen(true)} className="btn-primary mx-auto flex min-h-[56px] w-full max-w-lg items-center justify-between px-5 text-base font-bold">
            <span>{t.viewCart} ({itemCount} {t.items})</span>
            <span>{quote ? formatIdr(quote.total_amount) : ""}</span>
          </button>
        </div>
      ) : null}

      {optionsFor ? (
        <ProductOptionsSheet
          product={optionsFor}
          onClose={() => setOptionsFor(null)}
          onAdd={(line) => {
            dispatch({ type: "add", line });
            setOptionsFor(null);
          }}
        />
      ) : null}

      {cartOpen ? (
        <CartSheet
          lines={lines}
          dispatch={dispatch}
          quote={quote}
          quoteLoading={loading}
          quoteFailed={failed}
          onRetryQuote={refresh}
          guestName={guestName}
          onGuestName={setGuestName}
          placing={placing}
          error={checkoutError}
          canOrder={table.ordering_open}
          onPay={checkout}
          onClose={() => setCartOpen(false)}
        />
      ) : null}
    </div>
  );
}
