"use client";

import React, { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import {
  apiFetch,
  CatalogCategory,
  CatalogProduct,
  KdsTicket,
  fetchKdsTickets,
  normalizeCatalogProducts,
  StoreTable
} from "@/lib/api";
import { formatIdr } from "@/lib/format";
import { DataStateBox, StatusBadge } from "@/components/Shell";
import { QrCodeSvg } from "@/components/QrCodeSvg";

const RECENT_QR_TABLES_STORAGE = "mycaffe_recent_qr_tables";

export function TabCatalogTablesKds({
  token,
  storeSlug
}: {
  token: string;
  storeSlug: string;
}) {
  const [categories, setCategories] = useState<CatalogCategory[]>([]);
  const [products, setProducts] = useState<CatalogProduct[]>([]);
  const [tables, setTables] = useState<StoreTable[]>([]);
  const [tickets, setTickets] = useState<KdsTicket[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ tone: "ok" | "err" | "info"; text: string } | null>(null);

  // Category Form
  const [catName, setCatName] = useState("Kopi Signature");
  const [catOrder, setCatOrder] = useState(1);
  const [catBusy, setCatBusy] = useState(false);

  // Product Form
  const [prodCategoryId, setProdCategoryId] = useState("");
  const [prodName, setProdName] = useState("Es Kopi Susu Gula Aren");
  const [prodDesc, setProdDesc] = useState("Espresso single origin dengan susu segar dan gula aren organik");
  const [prodPrice, setProdPrice] = useState(28000);
  const [prodSku, setProdSku] = useState("KOP-001");
  const [prodStock, setProdStock] = useState(100);
  const [prodStation, setProdStation] = useState<"bar" | "kitchen">("bar");
  const [prodBusy, setProdBusy] = useState(false);

  // Table Form
  const [tableNumber, setTableNumber] = useState("T-05");
  const [tableCapacity, setTableCapacity] = useState(4);
  const [tableArea, setTableArea] = useState("Indoor");
  const [tableBusy, setTableBusy] = useState(false);

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [catRes, tblRes, kdsRes] = await Promise.all([
        apiFetch<{ categories?: CatalogCategory[]; products?: CatalogProduct[] }>(
          "/api/v1/pos/catalog",
          { bearerToken: token }
        ),
        apiFetch<{ tables?: StoreTable[] }>("/api/v1/pos/tables", { bearerToken: token }),
        fetchKdsTickets(token).catch(() => [] as KdsTicket[])
      ]);

      const cats = Array.isArray(catRes.categories) ? catRes.categories : [];
      const prods = normalizeCatalogProducts(catRes.products);
      const tbls = Array.isArray(tblRes.tables) ? tblRes.tables : [];
      const tks = kdsRes;

      setCategories(cats);
      setProducts(prods);
      setTables(tbls);
      setTickets(tks);

      if (cats.length > 0 && !prodCategoryId) {
        setProdCategoryId(cats[0].id);
      }

      if (tbls.length > 0) {
        try {
          localStorage.setItem(
            RECENT_QR_TABLES_STORAGE,
            JSON.stringify(
              tbls.map((t) => ({
                storeSlug,
                tableNumber: t.table_number,
                qrToken: t.qr_token
              }))
            )
          );
        } catch {
          // Ignore localStorage quota
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Gagal memuat katalog dan meja QR");
    } finally {
      setLoading(false);
    }
  }, [token, storeSlug, prodCategoryId]);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!catName.trim()) return;
    setCatBusy(true);
    setFeedback(null);
    try {
      const created = await apiFetch<{ category?: CatalogCategory; id?: string }>(
        "/api/v1/catalog/categories",
        {
          method: "POST",
          bearerToken: token,
          body: {
            name: catName.trim(),
            display_order: Number(catOrder) || 1
          }
        }
      );
      const newId = created.category?.id ?? created.id;
      if (newId) setProdCategoryId(newId);
      setFeedback({ tone: "ok", text: `Kategori "${catName.trim()}" berhasil ditambahkan.` });
      await loadAll();
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal membuat kategori menu"
      });
    } finally {
      setCatBusy(false);
    }
  };

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    const targetCat = prodCategoryId || categories[0]?.id;
    if (!targetCat) {
      setFeedback({ tone: "err", text: "Buat minimal satu kategori terlebih dahulu." });
      return;
    }
    setProdBusy(true);
    setFeedback(null);
    try {
      await apiFetch("/api/v1/catalog/products", {
        method: "POST",
        bearerToken: token,
        body: {
          category_id: targetCat,
          name: prodName.trim(),
          description: prodDesc.trim(),
          price: Math.max(0, Math.trunc(Number(prodPrice) || 0)),
          sku: prodSku.trim(),
          is_available: true,
          stock_quantity: Math.max(0, Math.trunc(Number(prodStock) || 0)),
          prep_station: prodStation
        }
      });
      setFeedback({ tone: "ok", text: `Produk "${prodName.trim()}" berhasil disimpan ke katalog.` });
      await loadAll();
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal menyimpan produk baru"
      });
    } finally {
      setProdBusy(false);
    }
  };

  const handleCreateTable = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tableNumber.trim()) return;
    setTableBusy(true);
    setFeedback(null);
    try {
      await apiFetch("/api/v1/pos/tables", {
        method: "POST",
        bearerToken: token,
        body: {
          table_number: tableNumber.trim(),
          capacity: Math.max(1, Math.trunc(Number(tableCapacity) || 4)),
          area: tableArea.trim() || "Indoor"
        }
      });
      setFeedback({ tone: "ok", text: `Meja QR ${tableNumber.trim()} berhasil diterbitkan.` });
      await loadAll();
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal membuat meja QR"
      });
    } finally {
      setTableBusy(false);
    }
  };

  const handleTransitionTicket = async (ticketId: string, toStatus: string) => {
    setFeedback(null);
    try {
      await apiFetch(`/api/v1/kds/tickets/${encodeURIComponent(ticketId)}/transition`, {
        method: "POST",
        bearerToken: token,
        body: { to_status: toStatus }
      });
      setFeedback({ tone: "ok", text: `Status tiket dapur diperbarui ke ${toStatus}.` });
      await loadAll();
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal mengubah status tiket KDS"
      });
    }
  };

  if (loading) {
    return (
      <DataStateBox
        state="loading"
        title="Memuat katalog kasir, meja QR, dan tiket dapur"
        description="Mengambil data operasional gerai dari /api/v1/pos/catalog, /pos/tables, dan /kds/tickets..."
      />
    );
  }

  if (error) {
    return (
      <DataStateBox
        state="error"
        title="Gagal memuat data kasir"
        description={error}
        onRetry={loadAll}
      />
    );
  }

  return (
    <div className="space-y-8">
      {feedback ? (
        <div role="status">
          <StatusBadge tone={feedback.tone} label={feedback.text} />
        </div>
      ) : null}

      {/* Create Category & Product Forms */}
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <form
          onSubmit={handleCreateCategory}
          className="space-y-4 rounded-xl border border-subtle bg-surface p-5 xl:col-span-4"
        >
          <h3 className="font-display text-base font-bold text-ink">1. Tambah Kategori Menu</h3>
          <div>
            <label htmlFor="cat-name" className="block text-xs font-bold text-ink">
              Nama Kategori
            </label>
            <input
              id="cat-name"
              type="text"
              required
              value={catName}
              onChange={(e) => setCatName(e.target.value)}
              className="input-field mt-1"
            />
          </div>
          <div>
            <label htmlFor="cat-order" className="block text-xs font-bold text-ink">
              Urutan Tampil
            </label>
            <input
              id="cat-order"
              type="number"
              min={1}
              value={catOrder}
              onChange={(e) => setCatOrder(Number(e.target.value))}
              className="input-field mt-1 font-mono"
            />
          </div>
          <button
            type="submit"
            disabled={catBusy}
            className="btn-primary min-h-[48px] w-full px-4 py-2 text-xs font-bold"
          >
            {catBusy ? "Menyimpan..." : "Simpan Kategori"}
          </button>
        </form>

        <form
          onSubmit={handleCreateProduct}
          className="space-y-4 rounded-xl border border-subtle bg-surface p-5 xl:col-span-8"
        >
          <h3 className="font-display text-base font-bold text-ink">2. Tambah Produk Menu (Harga Integer IDR)</h3>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label htmlFor="prod-cat" className="block text-xs font-bold text-ink">
                Kategori
              </label>
              <select
                id="prod-cat"
                value={prodCategoryId}
                onChange={(e) => setProdCategoryId(e.target.value)}
                className="input-field mt-1"
              >
                {categories.length === 0 ? (
                  <option value="">Buat kategori terlebih dahulu</option>
                ) : (
                  categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))
                )}
              </select>
            </div>

            <div>
              <label htmlFor="prod-name" className="block text-xs font-bold text-ink">
                Nama Menu
              </label>
              <input
                id="prod-name"
                type="text"
                required
                value={prodName}
                onChange={(e) => setProdName(e.target.value)}
                className="input-field mt-1"
              />
            </div>

            <div>
              <label htmlFor="prod-price" className="block text-xs font-bold text-ink">
                Harga Satuan (Rupiah Penuh)
              </label>
              <input
                id="prod-price"
                type="number"
                min={0}
                step={500}
                required
                value={prodPrice}
                onChange={(e) => setProdPrice(Number(e.target.value))}
                className="input-field mt-1 font-mono"
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label htmlFor="prod-sku" className="block text-xs font-bold text-ink">
                  SKU
                </label>
                <input
                  id="prod-sku"
                  type="text"
                  required
                  value={prodSku}
                  onChange={(e) => setProdSku(e.target.value)}
                  className="input-field mt-1 font-mono"
                />
              </div>
              <div>
                <label htmlFor="prod-station" className="block text-xs font-bold text-ink">
                  Stasiun KDS
                </label>
                <select
                  id="prod-station"
                  value={prodStation}
                  onChange={(e) => setProdStation(e.target.value === "kitchen" ? "kitchen" : "bar")}
                  className="input-field mt-1"
                >
                  <option value="bar">Bar / Barista</option>
                  <option value="kitchen">Dapur Panas</option>
                </select>
              </div>
            </div>

            <div className="sm:col-span-2">
              <label htmlFor="prod-desc" className="block text-xs font-bold text-ink">
                Deskripsi Menu &amp; Stok Awal
              </label>
              <div className="mt-1 flex flex-col gap-2 sm:flex-row">
                <input
                  id="prod-desc"
                  type="text"
                  value={prodDesc}
                  onChange={(e) => setProdDesc(e.target.value)}
                  className="input-field flex-1"
                />
                <input
                  aria-label="Stok awal"
                  type="number"
                  min={0}
                  value={prodStock}
                  onChange={(e) => setProdStock(Number(e.target.value))}
                  className="input-field w-full font-mono sm:w-32"
                />
              </div>
            </div>
          </div>

          <button
            type="submit"
            disabled={prodBusy}
            className="btn-primary min-h-[48px] px-5 py-2 text-xs font-bold"
          >
            {prodBusy ? "Menyimpan Menu..." : `Tambahkan ke Katalog (${formatIdr(prodPrice)})`}
          </button>
        </form>
      </div>

      {/* Live Catalog Ledger */}
      <section className="space-y-3">
        <div className="flex items-center justify-between gap-2">
          <h3 className="font-display text-lg font-bold text-ink">
            Daftar Menu Aktif ({products.length} item dalam {categories.length} kategori)
          </h3>
          <button
            type="button"
            onClick={loadAll}
            className="btn-secondary min-h-[44px] px-3.5 py-1.5 text-xs font-semibold"
          >
            Segarkan Katalog
          </button>
        </div>

        {products.length === 0 ? (
          <DataStateBox
            state="empty"
            title="Katalog menu gerai masih kosong"
            description="Tambahkan kategori dan menu pertama Anda melalui formulir di atas agar muncul pada layar QR meja tamu."
          />
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {products.map((prod) => (
              <div key={prod.id} className="rounded-xl border border-subtle bg-surface p-4">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-display text-sm font-bold text-ink">{prod.name}</p>
                    <p className="font-mono text-[11px] text-subink">
                      {prod.sku || "SKU"} | Stasiun: {prod.prep_station || "bar"}
                    </p>
                  </div>
                  <StatusBadge
                    tone={prod.is_available ? "ok" : "warn"}
                    label={prod.is_available ? "Tersedia" : "Habis"}
                  />
                </div>
                {prod.description ? (
                  <p className="mt-2 text-xs leading-relaxed text-subink">{prod.description}</p>
                ) : null}
                <p className="mt-3 font-display text-base font-extrabold tabular-nums text-ink">
                  {formatIdr(prod.price)}
                </p>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* QR Tables Generator & Scannable SVG Cards */}
      <section className="space-y-4">
        <div className="flex flex-col justify-between gap-4 rounded-xl border border-subtle bg-surface p-5 lg:flex-row lg:items-end">
          <div>
            <h3 className="font-display text-lg font-bold text-ink">
              Manajemen Meja QR &amp; Tautan Simulasi Pelanggan
            </h3>
            <p className="text-xs text-subink">
              Setiap kartu merender SVG kode QR murni di peramban tanpa pelacak gambar pihak ketiga.
            </p>
          </div>

          <form onSubmit={handleCreateTable} className="flex flex-wrap items-end gap-2">
            <div>
              <label htmlFor="tbl-num" className="block text-xs font-bold text-ink">
                Nomor Meja
              </label>
              <input
                id="tbl-num"
                type="text"
                required
                value={tableNumber}
                onChange={(e) => setTableNumber(e.target.value)}
                className="input-field mt-1 w-28 font-mono"
              />
            </div>
            <div>
              <label htmlFor="tbl-cap" className="block text-xs font-bold text-ink">
                Kursi
              </label>
              <input
                id="tbl-cap"
                type="number"
                min={1}
                max={24}
                value={tableCapacity}
                onChange={(e) => setTableCapacity(Number(e.target.value))}
                className="input-field mt-1 w-20 font-mono"
              />
            </div>
            <div>
              <label htmlFor="tbl-area" className="block text-xs font-bold text-ink">
                Area
              </label>
              <input
                id="tbl-area"
                type="text"
                value={tableArea}
                onChange={(e) => setTableArea(e.target.value)}
                className="input-field mt-1 w-32"
              />
            </div>
            <button
              type="submit"
              disabled={tableBusy}
              className="btn-primary min-h-[48px] px-4 py-2 text-xs font-bold"
            >
              {tableBusy ? "Membuat..." : "Buat Meja QR"}
            </button>
          </form>
        </div>

        {tables.length === 0 ? (
          <DataStateBox
            state="empty"
            title="Belum ada meja QR pada gerai ini"
            description="Buat meja pertama menggunakan formulir di atas untuk menerbitkan QR pemesanan tamu."
          />
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {tables.map((tbl) => {
              const orderPath = `/order/${encodeURIComponent(tbl.qr_token)}`;
              const fullQrUrl =
                typeof window !== "undefined"
                  ? `${window.location.origin}${orderPath}`
                  : orderPath;
              return (
                <div
                  key={tbl.id}
                  className="flex flex-col items-center gap-2.5 rounded-xl border border-subtle bg-surface p-4 text-center"
                >
                  <div className="flex w-full items-center justify-between">
                    <span className="font-display text-base font-extrabold text-ink">
                      Meja {tbl.table_number}
                    </span>
                    <span className="rounded bg-muted px-2 py-0.5 text-xs text-subink">
                      {tbl.area || "Indoor"} ({tbl.capacity})
                    </span>
                  </div>
                  <QrCodeSvg
                    value={fullQrUrl}
                    size={148}
                    label={`QR Pemesanan Meja ${tbl.table_number}`}
                  />
                  <code className="w-full truncate rounded bg-muted px-2 py-1 font-mono text-[11px] text-subink">
                    {tbl.qr_token}
                  </code>
                  <Link
                    href={orderPath}
                    className="btn-primary min-h-[48px] w-full px-3 py-2 text-xs font-bold"
                  >
                    Buka Simulasi QR Tamu
                  </Link>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* Live Kitchen Display System (KDS) Ticket Board */}
      <section className="space-y-3">
        <div className="flex items-center justify-between gap-2">
          <div>
            <h3 className="font-display text-lg font-bold text-ink">
              Papan Tiket Dapur &amp; Barista (Live KDS)
            </h3>
            <p className="text-xs text-subink">
              Tombol transisi berukuran minimal 48px untuk layar sentuh dapur.
            </p>
          </div>
          <button
            type="button"
            onClick={loadAll}
            className="btn-secondary min-h-[48px] px-4 py-2 text-xs font-semibold"
          >
            Muat Tiket KDS
          </button>
        </div>

        {tickets.length === 0 ? (
          <DataStateBox
            state="empty"
            title="Antrean dapur saat ini bersih"
            description="Pesanan tamu yang telah dibayar atau dikonfirmasi kasir akan langsung muncul sebagai tiket dapur di sini."
          />
        ) : (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            {tickets.map((tk) => (
              <div key={tk.id} className="space-y-3 rounded-xl border border-subtle bg-surface p-4">
                <div className="flex items-center justify-between gap-2 border-b border-subtle pb-2">
                  <div>
                    <p className="font-mono text-sm font-extrabold text-ink">
                      {tk.queue_number || tk.order_number || tk.id.slice(0, 8)}
                    </p>
                    <p className="text-xs text-subink">
                      Meja: {tk.table_number || "-"} | Stasiun: {tk.station || "bar"}
                    </p>
                  </div>
                  <StatusBadge
                    tone={tk.status === "ready" ? "ok" : "warn"}
                    label={tk.status}
                  />
                </div>

                {Array.isArray(tk.lines) && tk.lines.length > 0 ? (
                  <ul className="space-y-1 text-sm text-ink">
                    {tk.lines.map((it, i) => (
                      <li key={i} className="flex justify-between gap-2">
                        <span>
                          <strong className="tabular-nums">{it.quantity}x</strong>{" "}
                          {it.name || "Item"}
                        </span>
                        {it.notes ? <span className="text-xs text-subink">({it.notes})</span> : null}
                      </li>
                    ))}
                  </ul>
                ) : null}

                <div className="flex flex-wrap gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => handleTransitionTicket(tk.id, "preparing")}
                    className="btn-secondary min-h-[48px] flex-1 px-3 py-2 text-xs font-bold"
                  >
                    Siapkan
                  </button>
                  <button
                    type="button"
                    onClick={() => handleTransitionTicket(tk.id, "ready")}
                    className="btn-primary min-h-[48px] flex-1 px-3 py-2 text-xs font-bold"
                  >
                    Siap Saji
                  </button>
                  <button
                    type="button"
                    onClick={() => handleTransitionTicket(tk.id, "served")}
                    className="btn-secondary min-h-[48px] flex-1 px-3 py-2 text-xs font-bold"
                  >
                    Selesai
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

