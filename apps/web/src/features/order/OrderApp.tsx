"use client";

import React, { useEffect, useMemo, useReducer, useRef, useState } from "react";
import { Icon } from "@/components/Icon";
import { Notices } from "@/components/Toast";
import { useLocale } from "@/components/Providers";
import { DataStateBox } from "@/components/Shell";
import { ApiError, apiFetch } from "@/lib/api";
import { brandVariables } from "@/lib/brand";
import { formatIdr } from "@/lib/format";
import { CartSheet } from "./CartSheet";
import { cartReducer, toOrderItems, type CartLine } from "./cart";
import { CategoryNav, sectionId } from "./CategoryNav";
import { menuSections } from "./menu";
import { PaymentView } from "./PaymentView";
import { ProductImage } from "./ProductImage";
import { ProductOptionsSheet } from "./ProductOptionsSheet";
import { checkoutMessageKey, moneyTaken, orderPhase, writeActiveOrder, type ActiveOrder } from "./status";
import type { CatalogProduct, PaymentInfo, PayMethod, ServiceMode } from "./types";
import { playBell, primeBell, readMuted, writeMuted } from "./bell";
import { useOrderData, useOrderProgress, useQuote } from "./useOrderData";

function defaultVariant(product: CatalogProduct) {
  return product.variants.find((v) => v.is_default && v.available) ?? product.variants.find((v) => v.available);
}

const initials = (name: string) =>
  name
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0] ?? "")
    .join("")
    .toUpperCase();

/** Photo-led card: the photo opens the details sheet, the round button adds or opens the choices. */
function ProductCard({
  product,
  canOrder,
  onOpen,
  onQuickAdd,
  eager
}: {
  product: CatalogProduct;
  canOrder: boolean;
  onOpen: () => void;
  onQuickAdd: () => void;
  eager: boolean;
}) {
  const { t } = useLocale();
  const variant = defaultVariant(product);
  const soldOut = !product.available || !variant;
  const bundled = (product.bundle_slots?.length ?? 0) > 0;
  const needsChoice = product.variants.length > 1 || product.modifier_groups.length > 0;

  return (
    <li className="min-w-0">
      <div className="relative">
        <button
          type="button"
          aria-label={`${t.details} ${product.name}`}
          onClick={onOpen}
          className={`block w-full overflow-hidden rounded-2xl ${soldOut ? "opacity-50" : ""}`}
        >
          {product.image_url ? (
            <ProductImage src={product.image_url} alt="" width={320} height={320} eager={eager} className="aspect-square w-full" />
          ) : (
            <span aria-hidden="true" className="grid aspect-square w-full place-items-center bg-muted text-2xl font-bold text-subink">
              {initials(product.name)}
            </span>
          )}
        </button>
        {!soldOut && !bundled && canOrder ? (
          <button
            type="button"
            aria-label={`${needsChoice ? t.choose : t.add} ${product.name}`}
            onClick={needsChoice ? onOpen : onQuickAdd}
            className="btn-primary absolute bottom-2 right-2 h-11 w-11 rounded-full shadow-md"
          >
            <Icon name="plus" />
          </button>
        ) : null}
      </div>
      <h3 className="mt-2 line-clamp-2 text-sm font-bold leading-snug">{product.name}</h3>
      <p className="mt-0.5 text-base font-bold tabular-nums">{variant ? formatIdr(variant.price) : ""}</p>
      {soldOut ? <p className="text-xs font-semibold text-subink">{t.soldOut}</p> : null}
      {!soldOut && bundled ? <p className="text-xs text-subink">{t.askStaff}</p> : null}
    </li>
  );
}

export function OrderApp({ token }: { token: string }) {
  const { t, locale } = useLocale();
  const { state, data, reload } = useOrderData(token);
  const [lines, dispatch] = useReducer(cartReducer, [] as CartLine[]);
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

  const methods: PayMethod[] = data?.table.payment_methods?.length ? data.table.payment_methods : ["qris"];
  const [picked, setPicked] = useState<PayMethod | null>(null);
  const method = picked && methods.includes(picked) ? picked : methods[0];

  // One key per method: retrying the same method replays, switching makes a new attempt.
  const initiatePayment = async (orderId: string, key: string, via: PayMethod) => {
    const { payment: created } = await apiFetch<{ payment: PaymentInfo }>(
      `/api/v1/public/guest/orders/${encodeURIComponent(orderId)}/payments`,
      { method: "POST", body: { idempotency_key: `${key}_${via}`, method: via } }
    );
    setPayment({ ...created, method: created.method ?? via });
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
      initiatePayment(saved.id, saved.payKey, method).catch(() => setCheckoutError(t.checkoutFailed));
    }
    // initiatePayment only uses stable setters; the ref limits this to one run.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  useEffect(() => {
    // A served order is done; a cancelled one stays so a guest who paid still sees where to ask for the refund.
    const served = tracked?.status === "served" || tracked?.status === "completed";
    if (state === "ready") writeActiveOrder(token, order && !served ? { order, payment } : null);
  }, [state, token, order, payment, tracked?.status]);

  // The moment the guest waits for after paying: ready at the bar (pickup) or delivered to the table.
  const serviceMode: ServiceMode = data?.table.service_mode ?? "pickup";
  const arrived =
    serviceMode === "table" ? tracked?.status === "served" || tracked?.status === "completed" : tracked?.status === "ready";
  const arrivedText = serviceMode === "table" ? t.toastDelivered : t.toastReady;

  useEffect(() => {
    if (!data) return;
    const base = `${data.table.store_name} | ${data.table.label || `${t.table} ${data.table.table_number}`}`;
    // A guest on another tab sees the news in the tab bar.
    const apply = () => {
      document.title = arrived && document.hidden ? `${arrivedText} | ${base}` : base;
    };
    apply();
    document.addEventListener("visibilitychange", apply);
    return () => document.removeEventListener("visibilitychange", apply);
  }, [data, t.table, arrived, arrivedText]);

  const [muted, setMuted] = useState(false);
  useEffect(() => {
    setMuted(readMuted());
    // Browsers allow sound only after a tap; the first tap anywhere unlocks the bell for later.
    window.addEventListener("pointerdown", primeBell, { once: true });
    return () => window.removeEventListener("pointerdown", primeBell);
  }, []);
  const toggleSound = () => {
    writeMuted(!muted);
    setMuted(!muted);
  };

  // Payment itself gets its own heading and animated check, so only this moment has a toast.
  const [toast, setToast] = useState<string | null>(null);
  const wasArrived = useRef(arrived);
  useEffect(() => {
    const changed = arrived && !wasArrived.current;
    wasArrived.current = arrived;
    if (!changed) return;
    navigator.vibrate?.(200);
    if (!muted && !document.hidden) playBell(serviceMode === "table" ? "delivered" : "ready");
    setToast(arrivedText);
    const id = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(id);
  }, [arrived, arrivedText, muted, serviceMode]);

  // Offline banner; back online re-prices the cart (the tracker re-polls on its own).
  const [online, setOnline] = useState(true);
  useEffect(() => {
    const up = () => {
      setOnline(true);
      void refresh();
    };
    const down = () => setOnline(false);
    setOnline(navigator.onLine);
    window.addEventListener("online", up);
    window.addEventListener("offline", down);
    return () => {
      window.removeEventListener("online", up);
      window.removeEventListener("offline", down);
    };
  }, [refresh]);
  const notices = <Notices toast={toast} offline={!online} offlineText={t.offline} />;

  const sections = useMemo(() => menuSections(data?.categories ?? [], data?.products ?? [], t.otherMenu), [data, t.otherMenu]);
  // About the first screen at 360 to 390 px: two rows of the two-column grid.
  const eagerIds = useMemo(() => new Set(sections.flatMap((s) => s.products).slice(0, 4).map((p) => p.id)), [sections]);
  const multiVariant = useMemo(() => new Set((data?.products ?? []).filter((p) => p.variants.length > 1).map((p) => p.id)), [data]);
  const itemCount = lines.reduce((sum, l) => sum + l.quantity, 0);

  if (state === "loading") return <div className="p-5"><DataStateBox state="loading" title={t.loadingTitle} description={t.loadingBody} /></div>;
  if (state === "missing") return <div className="p-5"><DataStateBox state="error" title={t.tableMissingTitle} description={t.tableMissingBody} /></div>;
  if (state === "maintenance") {
    return <div className="p-5"><DataStateBox state="error" title={t.maintenanceTitle} description={t.maintenanceBody} onRetry={reload} /></div>;
  }
  if (state === "failed" || !data) {
    return <div className="p-5"><DataStateBox state="error" title={t.loadFailedTitle} description={t.loadFailedBody} onRetry={reload} /></div>;
  }

  const { table, cms } = data;
  const announcement = locale === "en-US" ? cms.announcement_banner_en : cms.announcement_banner_id;
  const tagline = locale === "en-US" ? cms.hero_subtitle_en : cms.hero_subtitle_id;
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
      await initiatePayment(created.order.id, payKey, method);
    } catch (err) {
      const code = err instanceof ApiError ? (err.details as { code?: unknown } | undefined)?.code : undefined;
      setCheckoutError(t[checkoutMessageKey(code)]);
      if (code === "GUEST_SESSION_EXPIRED") void reload();
      else if (code === "QUOTE_CHANGED" || code === "UNAVAILABLE") void refresh();
    } finally {
      setPlacing(false);
    }
  };

  // A fresh attempt: a new QR after expiry, or another method; the server cancels the open one.
  const newAttempt = async (via: PayMethod) => {
    if (!order) return;
    setPicked(via);
    setPlacing(true);
    setCheckoutError(null);
    try {
      await initiatePayment(order.id, `pay_${crypto.randomUUID()}`, via);
    } catch (err) {
      const code = err instanceof ApiError ? (err.details as { code?: unknown } | undefined)?.code : undefined;
      setCheckoutError(t[checkoutMessageKey(code)]);
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
      await initiatePayment(order.id, order.payKey, method);
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
        {notices}
        <PaymentView
          payment={payment}
          phase={phase}
          moneyTaken={moneyTaken(tracked)}
          orderId={order.id}
          orderNumber={tracked?.order_number ?? order.number}
          status={tracked?.status}
          retrying={placing}
          error={checkoutError}
          methods={methods}
          onNewQr={() => void newAttempt(payment.method ?? method)}
          onSwitch={(via) => void newAttempt(via)}
          serviceMode={serviceMode}
          muted={muted}
          onToggleSound={toggleSound}
          onBack={() => setViewingPayment(false)}
          onOrderAgain={startOver}
        />
      </div>
    );
  }

  return (
    <div style={brand} className="mx-auto max-w-lg md:max-w-3xl">
      {notices}
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
        <div className="flex items-start justify-between gap-3">
          <h1 className="text-3xl font-extrabold leading-tight">{table.store_name}</h1>
          <p className="mt-1 shrink-0 rounded-full bg-muted px-3 py-1 text-sm font-semibold">
            {table.label || `${t.table} ${table.table_number}`}
          </p>
        </div>
        {tagline ? <p className="mt-1 text-sm text-subink">{tagline}</p> : null}
        {announcement ? <p className="mt-3 rounded-lg bg-muted px-3 py-2 text-sm">{announcement}</p> : null}
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

      {sections.length > 1 ? <CategoryNav sections={sections} /> : null}

      <div className="px-5">
        {sections.length === 0 ? (
          <div className="mt-4"><DataStateBox state="empty" title={t.emptyTitle} description={t.emptyBody} /></div>
        ) : (
          sections.map((section) => (
            <section key={section.id} aria-labelledby={sectionId(section.id)} className="mt-4">
              {/* scroll-mt keeps the heading clear of the sticky category strip after a jump. */}
              <h2 id={sectionId(section.id)} className="scroll-mt-16 text-lg font-bold">
                {section.name}
              </h2>
              <ul className="mt-3 grid grid-cols-2 gap-x-3 gap-y-5 md:grid-cols-3">
                {section.products.map((product) => (
                  <ProductCard
                    key={product.id}
                    product={product}
                    canOrder={table.ordering_open}
                    eager={eagerIds.has(product.id)}
                    onOpen={() => setOptionsFor(product)}
                    onQuickAdd={() =>
                      dispatch({
                        type: "add",
                        line: { productId: product.id, variantId: defaultVariant(product)?.id, name: product.name, options: [], quantity: 1, notes: "" }
                      })
                    }
                  />
                ))}
              </ul>
            </section>
          ))
        )}
      </div>

      {itemCount > 0 ? (
        <div data-cart-bar className="fixed inset-x-0 bottom-0 z-20 flex justify-center bg-surface p-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
          <button type="button" onClick={() => setCartOpen(true)} className="btn-primary mx-auto flex min-h-[56px] w-full max-w-lg items-center justify-between px-5 text-base font-bold">
            <span className="flex items-center gap-2">
              <Icon name="bag" />
              {t.viewCart} ({itemCount} {t.items})
            </span>
            <span className="tabular-nums">{quote ? formatIdr(quote.total_amount) : ""}</span>
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
          multiVariant={multiVariant}
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
          methods={methods}
          method={method}
          onMethod={setPicked}
          onPay={checkout}
          onClose={() => setCartOpen(false)}
        />
      ) : null}
    </div>
  );
}
