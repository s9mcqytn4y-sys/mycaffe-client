"use client";

import React, { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import {
  apiFetch,
  CatalogCategory,
  CatalogProduct
} from "@/lib/api";
import { formatIdr } from "@/lib/format";
import { QrCodeSvg } from "@/components/QrCodeSvg";
import { DataStateBox, StatusBadge } from "@/components/Shell";
import {
  detectWalletCapabilities,
  WalletCapabilityReport
} from "@/lib/walletCapability";

interface TableInfo {
  store_slug: string;
  table_number: string;
  is_active: boolean;
}

interface CartItem {
  product: CatalogProduct;
  quantity: number;
  notes: string;
}

interface PaymentInfo {
  payment_id: string;
  qr_string?: string;
  qr_url?: string;
  expires_at?: string;
  gross_amount: number;
}

interface ServerQuoteSummary {
  subtotal: number;
  tax: number;
  service: number;
  discount: number;
  total: number;
  quote_fingerprint: string;
}

export default function GuestOrderPage() {
  const params = useParams();
  const token = typeof params?.token === "string" ? params.token : "";

  const [tableInfo, setTableInfo] = useState<TableInfo | null>(null);
  const [guestToken, setGuestToken] = useState<string>("");
  const [guestName, setGuestName] = useState<string>("Pelanggan Meja");

  const [categories, setCategories] = useState<CatalogCategory[]>([]);
  const [products, setProducts] = useState<CatalogProduct[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>("all");

  const [cart, setCart] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Server-side quote state
  const [quote, setQuote] = useState<ServerQuoteSummary | null>(null);
  const [quoteLoading, setQuoteLoading] = useState<boolean>(false);

  // Payment & Tracking State
  const [orderId, setOrderId] = useState<string | null>(null);
  const [trackingToken, setTrackingToken] = useState<string | null>(null);
  const [paymentInfo, setPaymentInfo] = useState<PaymentInfo | null>(null);
  const [orderStatus, setOrderStatus] = useState<string>("unpaid");
  const [submittingOrder, setSubmittingOrder] = useState<boolean>(false);
  const [walletCap, setWalletCap] = useState<WalletCapabilityReport | null>(null);

  // Digital Receipt state
  const [receiptEmail, setReceiptEmail] = useState<string>("");
  const [receiptSending, setReceiptSending] = useState<boolean>(false);
  const [receiptSentMessage, setReceiptSentMessage] = useState<string | null>(null);
  const [orderNumber, setOrderNumber] = useState<string>("");

  // Privacy info modal state
  const [showPrivacyModal, setShowPrivacyModal] = useState<boolean>(false);

  useEffect(() => {
    detectWalletCapabilities().then(setWalletCap).catch(() => null);
  }, []);

  const initTable = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      // 1. Resolve table QR
      const qrRes = await apiFetch<TableInfo>(`/api/v1/public/qr/${encodeURIComponent(token)}`);
      setTableInfo(qrRes);

      // 2. Start guest session
      const sessRes = await apiFetch<{ token?: string; guest_token?: string }>(
        `/api/v1/public/qr/${encodeURIComponent(token)}/session`,
        {
          method: "POST",
          body: {
            customer_name: guestName || "Pelanggan Meja"
          }
        }
      );
      const gTok = sessRes.token || sessRes.guest_token || "";
      setGuestToken(gTok);

      // 3. Load Store Catalog
      const catRes = await apiFetch<{
        categories?: CatalogCategory[];
        products?: CatalogProduct[];
      }>(`/api/v1/public/stores/${encodeURIComponent(qrRes.store_slug)}/catalog`);

      setCategories(Array.isArray(catRes.categories) ? catRes.categories : []);
      setProducts(Array.isArray(catRes.products) ? catRes.products : []);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Gagal memverifikasi kode QR meja. Silakan panggil staf restoran."
      );
    } finally {
      setLoading(false);
    }
  }, [token, guestName]);

  useEffect(() => {
    initTable();
  }, [initTable]);

  // Recalculate quote from server whenever cart changes
  const fetchQuote = useCallback(async () => {
    if (!tableInfo || cart.length === 0) {
      setQuote(null);
      return;
    }
    setQuoteLoading(true);
    try {
      const itemsPayload = cart.map((item) => ({
        product_id: item.product.id,
        quantity: item.quantity,
        notes: item.notes || ""
      }));

      const res = await apiFetch<Record<string, unknown>>(
        `/api/v1/public/stores/${encodeURIComponent(tableInfo.store_slug)}/orders/quote`,
        {
          method: "POST",
          guestToken,
          body: { items: itemsPayload }
        }
      );

      setQuote({
        subtotal: Number(res.subtotal ?? 0),
        tax: Number(res.tax ?? res.tax_amount ?? 0),
        service: Number(res.service ?? res.service_charge ?? 0),
        discount: Number(res.discount ?? res.discount_amount ?? 0),
        total: Number(res.total ?? res.grand_total ?? res.gross_amount ?? 0),
        quote_fingerprint: String(res.quote_fingerprint ?? res.fingerprint ?? "")
      });
    } catch {
      // Fallback local estimation if offline or temporary quote failure
      const localSubtotal = cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
      setQuote({
        subtotal: localSubtotal,
        tax: 0,
        service: 0,
        discount: 0,
        total: localSubtotal,
        quote_fingerprint: ""
      });
    } finally {
      setQuoteLoading(false);
    }
  }, [tableInfo, cart, guestToken]);

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchQuote();
    }, 250);
    return () => clearTimeout(timer);
  }, [fetchQuote]);

  const addToCart = (product: CatalogProduct) => {
    setCart((prev) => {
      const idx = prev.findIndex((item) => item.product.id === product.id);
      if (idx >= 0) {
        const copy = [...prev];
        copy[idx].quantity += 1;
        return copy;
      }
      return [...prev, { product, quantity: 1, notes: "" }];
    });
  };

  const updateQuantity = (productId: string, delta: number) => {
    setCart((prev) => {
      return prev
        .map((item) => {
          if (item.product.id === productId) {
            const nextQty = item.quantity + delta;
            return nextQty > 0 ? { ...item, quantity: nextQty } : null;
          }
          return item;
        })
        .filter((item): item is CartItem => item !== null);
    });
  };

  const localSubtotal = cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
  const payableTotal = quote?.total || localSubtotal;

  const handleCheckoutQris = async () => {
    if (!tableInfo || cart.length === 0) return;
    setSubmittingOrder(true);
    setError(null);

    const idempotencyKey = typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `cart_${Date.now()}`;

    try {
      // 1. Create Guest Order with quote fingerprint
      const itemsPayload = cart.map((item) => ({
        product_id: item.product.id,
        quantity: item.quantity,
        notes: item.notes || ""
      }));

      const orderRes = await apiFetch<{
        order_id?: string;
        id?: string;
        order_number?: string;
        tracking_token?: string;
      }>("/api/v1/public/guest/orders", {
        method: "POST",
        guestToken,
        idempotencyKey,
        body: {
          items: itemsPayload,
          customer_name: guestName,
          expected_quote_fingerprint: quote?.quote_fingerprint || undefined
        }
      });

      const confirmedOrderId = orderRes.order_id || orderRes.id || "";
      const trackTok = orderRes.tracking_token || "";
      setOrderId(confirmedOrderId);
      setTrackingToken(trackTok);
      if (orderRes.order_number) {
        setOrderNumber(orderRes.order_number);
      }

      // 2. Initiate Midtrans QRIS Payment
      const payRes = await apiFetch<{
        payment_id?: string;
        id?: string;
        qr_string?: string;
        qr_url?: string;
        expires_at?: string;
        gross_amount?: number;
      }>(`/api/v1/public/guest/orders/${encodeURIComponent(confirmedOrderId)}/payments`, {
        method: "POST",
        guestToken,
        idempotencyKey: `pay_${idempotencyKey}`,
        body: {
          payment_method: "qris"
        }
      });

      setPaymentInfo({
        payment_id: payRes.payment_id || payRes.id || "",
        qr_string: payRes.qr_string || "",
        qr_url: payRes.qr_url || "",
        expires_at: payRes.expires_at,
        gross_amount: payRes.gross_amount || payableTotal
      });
      setOrderStatus("awaiting_payment");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Gagal memproses pembayaran QRIS");
    } finally {
      setSubmittingOrder(false);
    }
  };

  // Digital Receipt Email Handler
  const handleSendReceipt = async () => {
    if (!orderId || !receiptEmail) return;
    setReceiptSending(true);
    try {
      await apiFetch(`/api/v1/public/guest/orders/${encodeURIComponent(orderId)}/receipt/send`, {
        method: "POST",
        guestToken,
        body: {
          channel: "email",
          to: receiptEmail
        }
      });
      setReceiptSentMessage(`Struk digital berhasil dikirimkan ke ${receiptEmail}.`);
    } catch (err) {
      setReceiptSentMessage(err instanceof Error ? err.message : "Gagal mengirim struk.");
    } finally {
      setReceiptSending(false);
    }
  };

  // Poll Order Tracker when waiting for payment
  useEffect(() => {
    if (!trackingToken || orderStatus === "paid" || orderStatus === "cancelled") return;

    const interval = setInterval(async () => {
      try {
        const res = await apiFetch<{
          status?: string;
          payment_status?: string;
          order_number?: string;
        }>(`/api/v1/public/orders/track/${encodeURIComponent(trackingToken)}`);

        if (res.order_number) {
          setOrderNumber(res.order_number);
        }

        if (res.payment_status === "paid" || res.status === "confirmed" || res.status === "in_preparation") {
          setOrderStatus("paid");
          clearInterval(interval);
        }
      } catch {
        // Polling failure tolerated in background
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [trackingToken, orderStatus]);

  if (loading) {
    return (
      <div className="py-12">
        <DataStateBox
          state="loading"
          title="Menyiapkan Menu Meja"
          description="Memverifikasi keabsahan kode meja dan mengambil katalog gerai..."
        />
      </div>
    );
  }

  if (error && !tableInfo) {
    return (
      <div className="py-12">
        <DataStateBox
          state="error"
          title="Meja Tidak Ditemukan"
          description={error}
          onRetry={initTable}
        />
      </div>
    );
  }

  const filteredProducts =
    selectedCategory === "all"
      ? products
      : products.filter((p) => p.category_id === selectedCategory);

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      {/* Top Banner */}
      <div className="rounded-2xl border border-subtle bg-surface p-4 sm:p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-display text-xl sm:text-2xl font-bold text-ink">
                Pemesanan Meja {tableInfo?.table_number || ""}
              </h1>
              <StatusBadge tone="ok" label="Meja Aktif" />
            </div>
            <p className="mt-1 text-xs text-subink">
              Gerai: <span className="font-semibold text-ink">{tableInfo?.store_slug}</span>. Pesanan langsung diteruskan ke KDS dapur setelah pembayaran terverifikasi.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-subink">Nama Pemesan:</span>
            <input
              type="text"
              value={guestName}
              onChange={(e) => setGuestName(e.target.value)}
              className="rounded-lg border border-subtle bg-canvas px-2.5 py-1 text-xs font-semibold text-ink outline-none focus:border-brand-accent"
              placeholder="Nama Anda"
            />
          </div>
        </div>

        {/* W3C Wallet Advisory */}
        {walletCap && (walletCap.applePayReady || walletCap.googlePayReady) && (
          <div className="mt-3 flex items-center gap-2 rounded-lg bg-muted px-3 py-1.5 text-[11px] text-subink">
            <span>Kompatibilitas Dompet Digital:</span>
            {walletCap.applePayReady && (
              <span className="font-semibold text-ink">Apple Pay Tersedia</span>
            )}
            {walletCap.googlePayReady && (
              <span className="font-semibold text-ink">Google Pay Tersedia</span>
            )}
            <span>(Didukung via QRIS Nasional & Midtrans SDK)</span>
          </div>
        )}
      </div>

      {/* Main Order Layout: Menu Catalog & Cart */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Menu Catalog (Left Column) */}
        <div className="space-y-4 lg:col-span-7">
          {/* Category Filter Pills */}
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setSelectedCategory("all")}
              className={`rounded-xl px-3 py-1.5 text-xs font-semibold transition-colors ${
                selectedCategory === "all"
                  ? "bg-brand text-brand-fg"
                  : "bg-surface text-ink border border-subtle hover:bg-muted"
              }`}
            >
              Semua Menu ({products.length})
            </button>
            {categories.map((c) => (
              <button
                key={c.id}
                onClick={() => setSelectedCategory(c.id)}
                className={`rounded-xl px-3 py-1.5 text-xs font-semibold transition-colors ${
                  selectedCategory === c.id
                    ? "bg-brand text-brand-fg"
                    : "bg-surface text-ink border border-subtle hover:bg-muted"
                }`}
              >
                {c.name}
              </button>
            ))}
          </div>

          {/* Product Grid */}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {filteredProducts.map((prod) => (
              <div
                key={prod.id}
                className="flex flex-col justify-between rounded-xl border border-subtle bg-surface p-4 shadow-sm"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-display text-sm font-bold text-ink">{prod.name}</h3>
                    <span className="rounded bg-muted px-1.5 py-0.5 font-mono text-[10px] text-subink">
                      {prod.prep_station === "kitchen" ? "Dapur" : "Bar"}
                    </span>
                  </div>
                  {prod.description && (
                    <p className="mt-1 line-clamp-2 text-xs text-subink">{prod.description}</p>
                  )}
                </div>

                <div className="mt-4 flex items-center justify-between pt-2 border-t border-subtle">
                  <span className="font-display text-sm font-bold tabular-nums text-accent">
                    {formatIdr(prod.price)}
                  </span>
                  <button
                    onClick={() => addToCart(prod)}
                    className="btn-primary min-h-[36px] px-3 py-1 text-xs font-bold"
                  >
                    + Tambah
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Cart & Checkout (Right Column) */}
        <div className="space-y-4 lg:col-span-5">
          <div className="rounded-2xl border border-subtle bg-surface p-5 shadow-sm">
            <h2 className="font-display text-lg font-bold text-ink border-b border-subtle pb-3">
              Keranjang Pesanan ({cart.reduce((s, i) => s + i.quantity, 0)})
            </h2>

            {cart.length === 0 ? (
              <p className="py-8 text-center text-xs text-subink">
                Keranjang masih kosong. Pilih menu di sebelah kiri untuk memesan.
              </p>
            ) : (
              <div className="divide-y divide-subtle">
                {cart.map((item) => (
                  <div key={item.product.id} className="py-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-ink">{item.product.name}</span>
                      <span className="font-mono text-xs font-bold text-ink">
                        {formatIdr(item.product.price * item.quantity)}
                      </span>
                    </div>
                    <div className="mt-2 flex items-center justify-between">
                      <span className="text-[11px] text-subink">
                        {formatIdr(item.product.price)} / porsi
                      </span>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => updateQuantity(item.product.id, -1)}
                          className="h-6 w-6 rounded bg-muted text-xs font-bold text-ink hover:bg-subtle"
                        >
                          -
                        </button>
                        <span className="font-mono text-xs font-bold text-ink">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => updateQuantity(item.product.id, 1)}
                          className="h-6 w-6 rounded bg-muted text-xs font-bold text-ink hover:bg-subtle"
                        >
                          +
                        </button>
                      </div>
                    </div>
                  </div>
                ))}

                {/* Total Summary with Server Quote */}
                <div className="pt-4 space-y-2">
                  <div className="flex items-center justify-between text-xs text-subink">
                    <span>Subtotal Menu</span>
                    <span className="font-mono font-medium text-ink">
                      {formatIdr(quote?.subtotal || localSubtotal)}
                    </span>
                  </div>

                  {quote && quote.tax > 0 && (
                    <div className="flex items-center justify-between text-xs text-subink">
                      <span>Pajak Restoran</span>
                      <span className="font-mono font-medium text-ink">{formatIdr(quote.tax)}</span>
                    </div>
                  )}

                  {quote && quote.service > 0 && (
                    <div className="flex items-center justify-between text-xs text-subink">
                      <span>Biaya Layanan</span>
                      <span className="font-mono font-medium text-ink">{formatIdr(quote.service)}</span>
                    </div>
                  )}

                  {quote && quote.discount > 0 && (
                    <div className="flex items-center justify-between text-xs text-brand-accent">
                      <span>Potongan Diskon</span>
                      <span className="font-mono font-medium">-{formatIdr(quote.discount)}</span>
                    </div>
                  )}

                  <div className="flex items-center justify-between pt-2 border-t border-subtle">
                    <span className="font-display text-base font-bold text-ink">
                      Total Pembayaran {quoteLoading && <span className="text-xs text-subink font-normal">(verifikasi server...)</span>}
                    </span>
                    <span className="font-display text-lg font-extrabold tabular-nums text-accent">
                      {formatIdr(payableTotal)}
                    </span>
                  </div>

                  <button
                    onClick={handleCheckoutQris}
                    disabled={submittingOrder || cart.length === 0}
                    className="btn-primary min-h-[48px] w-full mt-4 py-3 text-sm font-bold shadow-md"
                  >
                    {submittingOrder ? "Membuat Kode QRIS..." : `Bayar Sekarang ${formatIdr(payableTotal)}`}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Privacy & PDP Guarantee Link */}
          <div className="text-center pt-2">
            <button
              onClick={() => setShowPrivacyModal(true)}
              className="text-[11px] text-subink hover:text-ink underline"
            >
              Kebijakan Privasi & Perlindungan Data Pribadi (UU PDP)
            </button>
          </div>
        </div>
      </div>

      {/* QRIS Modal & Payment Tracker */}
      {paymentInfo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl border border-subtle bg-surface p-6 shadow-2xl">
            <div className="text-center space-y-2">
              <h3 className="font-display text-xl font-bold text-ink">
                {orderStatus === "paid" ? "Pembayaran Terverifikasi!" : "Pindai QRIS untuk Membayar"}
              </h3>
              <p className="text-xs text-subink">
                {orderStatus === "paid"
                  ? "Pesanan Anda berhasil dikonfirmasi dan langsung masuk ke KDS dapur/bar."
                  : "Mendukung BCA, GoPay, OVO, Dana, LinkAja, ShopeePay, dan seluruh m-Banking."}
              </p>
            </div>

            {orderStatus === "paid" ? (
              <div className="my-6 text-center space-y-4">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-emerald-800 text-2xl font-bold">
                  OK
                </div>
                <div className="rounded-2xl border-2 border-emerald-500 bg-emerald-50/50 p-4 space-y-2 text-left">
                  <div className="flex items-center justify-between border-b border-emerald-200 pb-2">
                    <span className="text-xs font-semibold text-emerald-800">Nomor Antrean Pesanan</span>
                    <span className="font-mono text-xl font-black text-emerald-700">
                      #{orderNumber || "014"}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-xs text-emerald-900">
                    <span>Estimasi Waktu Penyajian:</span>
                    <span className="font-bold">8 - 12 Menit</span>
                  </div>
                  <p className="text-[11px] text-emerald-700">
                    Meja {tableInfo?.table_number} &bull; Pesanan otomatis diteruskan ke dapur & barista
                  </p>
                </div>

                {/* Struk Digital Form */}
                <div className="rounded-xl border border-subtle bg-canvas p-3 space-y-2 text-left">
                  <label className="text-[11px] font-semibold text-ink">
                    Kirim Struk Digital ke Email Anda:
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="email"
                      value={receiptEmail}
                      onChange={(e) => setReceiptEmail(e.target.value)}
                      placeholder="contoh@email.com"
                      className="input-field text-xs py-1"
                    />
                    <button
                      onClick={handleSendReceipt}
                      disabled={receiptSending || !receiptEmail}
                      className="btn-secondary px-3 text-xs font-bold"
                    >
                      {receiptSending ? "..." : "Kirim"}
                    </button>
                  </div>
                  {receiptSentMessage && (
                    <p className="text-[11px] text-ink font-medium mt-1">{receiptSentMessage}</p>
                  )}
                </div>

                <button
                  onClick={() => {
                    setPaymentInfo(null);
                    setCart([]);
                    setOrderStatus("unpaid");
                    setReceiptSentMessage(null);
                  }}
                  className="btn-primary min-h-[44px] w-full py-2 text-xs font-bold"
                >
                  Pesan Menu Lainnya
                </button>
              </div>
            ) : (
              <div className="my-6 flex flex-col items-center space-y-4">
                <div className="rounded-xl border border-subtle bg-white p-4 shadow-sm">
                  {paymentInfo.qr_string ? (
                    <QrCodeSvg value={paymentInfo.qr_string} size={220} />
                  ) : paymentInfo.qr_url ? (
                    <img
                      src={paymentInfo.qr_url}
                      alt="Midtrans QRIS Code"
                      className="h-[220px] w-[220px] object-contain"
                    />
                  ) : (
                    <div className="flex h-[220px] w-[220px] items-center justify-center text-xs text-subink">
                      Memuat barcode QRIS...
                    </div>
                  )}
                </div>

                <div className="text-center space-y-1">
                  <p className="font-display text-2xl font-black text-accent tabular-nums">
                    {formatIdr(paymentInfo.gross_amount)}
                  </p>
                  <p className="text-[11px] text-subink animate-pulse">
                    Menunggu konfirmasi pembayaran otomatis dari bank/e-wallet...
                  </p>
                </div>

                <div className="w-full flex gap-2">
                  <button
                    onClick={() => setPaymentInfo(null)}
                    className="btn-secondary min-h-[40px] w-full py-2 text-xs font-semibold"
                  >
                    Tutup Barcode
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Privacy Policy Modal */}
      {showPrivacyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-2xl border border-subtle bg-surface p-6 shadow-2xl space-y-4">
            <h3 className="font-display text-lg font-bold text-ink">
              Kebijakan Data Pribadi Tamu (UU PDP)
            </h3>
            <div className="space-y-2 text-xs text-subink leading-relaxed max-h-[60vh] overflow-y-auto">
              <p>
                Data nama pemesan dan email struk Anda hanya digunakan untuk keperluan pengantaran pesanan di meja dan pengiriman bukti transaksi resmi.
              </p>
              <p>
                Kami tidak membagikan data kontak Anda kepada pihak ketiga manapun untuk keperluan pemasaran tanpa izin Anda.
              </p>
              <p>
                Sesuai Undang-Undang Perlindungan Data Pribadi (UU PDP No. 27/2022), Anda memiliki hak untuk meminta penghapusan riwayat data identitas Anda melalui staf kasir gerai.
              </p>
            </div>
            <div className="pt-2 border-t border-subtle flex justify-end">
              <button
                onClick={() => setShowPrivacyModal(false)}
                className="btn-primary px-4 py-2 text-xs font-bold"
              >
                Saya Mengerti
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
