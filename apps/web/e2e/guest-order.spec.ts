import { test, expect, type Page } from "@playwright/test";

const json = (body: unknown, status = 200) => ({ status, contentType: "application/json", body: JSON.stringify(body) });

const variant = (id: string, price: number) => ({ id, name: "Reguler", price, is_default: true, available: true });

const CATALOG = {
  categories: [{ id: "coffee", name: "Kopi" }],
  products: [
    { id: "croissant", category_id: "coffee", name: "Croissant", available: true, variants: [variant("v-c", 25000)], modifier_groups: [] },
    {
      id: "latte",
      category_id: "coffee",
      name: "Caffe Latte",
      available: true,
      variants: [variant("v-l", 32000)],
      modifier_groups: [
        {
          id: "milk",
          name: "Susu",
          required: true,
          min: 1,
          max: 1,
          options: [{ id: "oat", name: "Oat", price_delta: 6000, available: true }]
        }
      ]
    },
    { id: "gone", category_id: "coffee", name: "Americano", available: false, variants: [variant("v-g", 20000)], modifier_groups: [] }
  ]
};

const QUOTE = {
  subtotal_amount: 25000,
  discount_amount: 0,
  service_charge_amount: 1250,
  tax_amount: 2500,
  total_amount: 28750,
  quote_fingerprint: "fp",
  lines: [{ product_name: "Croissant", quantity: 1, subtotal_amount: 25000 }]
};

async function mockStore(page: Page, table: Record<string, unknown> = {}, cms: Record<string, unknown> = {}) {
  await page.route("**/api/v1/public/qr/T1", (r) =>
    r.fulfill(json({ store_name: "Kopi Senja", store_slug: "senja", table_number: "A1", label: "Meja A1", ordering_open: true, ...table }))
  );
  await page.route("**/api/v1/public/qr/T1/session", (r) => r.fulfill(json({ table_number: "A1" }, 201)));
  await page.route("**/api/v1/public/stores/senja/catalog", (r) => r.fulfill(json(CATALOG)));
  await page.route("**/api/v1/public/stores/senja/cms", (r) => r.fulfill(json({ cms })));
  await page.route("**/api/v1/public/stores/senja/orders/quote", (r) => r.fulfill(json(QUOTE)));
}

test("tamu memesan, membayar QRIS, dan melihat status", async ({ page }) => {
  await mockStore(page);
  let tracked = "unpaid";
  let orderBody: Record<string, unknown> = {};
  await page.route("**/api/v1/public/guest/orders", (r) => {
    orderBody = r.request().postDataJSON();
    expect(r.request().headers()["x-csrf-protection"]).toBe("1");
    return r.fulfill(json({ order: { id: "o1", order_number: "A-014" }, tracking_token: "trk" }, 201));
  });
  await page.route("**/api/v1/public/guest/orders/o1/payments", (r) => {
    tracked = "paid";
    return r.fulfill(json({ payment: { id: "p1", qr_string: "00020101", amount: 28750 } }, 201));
  });
  await page.route("**/api/v1/public/guest/ws-ticket", (r) => r.fulfill(json({}, 503)));
  await page.route("**/api/v1/public/orders/track/trk", (r) =>
    r.fulfill(json({ status: "preparing", payment_status: tracked, order_number: "A-014" }))
  );

  await page.goto("/order/T1");
  await expect(page.getByRole("heading", { level: 1, name: "Kopi Senja" })).toBeVisible();
  await expect(page.getByText("Habis")).toBeVisible();

  await page.getByRole("button", { name: "Tambah Croissant" }).click();
  await page.getByRole("button", { name: /Lihat keranjang/ }).click();
  await expect(page.getByRole("dialog")).toContainText("Rp 28.750");

  await page.getByLabel("Nama pemesan").fill("Rani");
  await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
  expect(orderBody.customer_name).toBe("Rani");
  expect(orderBody.expected_quote_fingerprint).toBe("fp");

  await expect(page.getByRole("heading", { name: "Pembayaran diterima" })).toBeVisible({ timeout: 15000 });
  await expect(page.getByText("A-014")).toBeVisible();
  await expect(page.getByText("Sedang disiapkan")).toBeVisible();
});

test("pilihan wajib harus diisi sebelum masuk keranjang", async ({ page }) => {
  await mockStore(page);
  await page.goto("/order/T1");
  await page.getByRole("button", { name: "Pilih Caffe Latte" }).click();
  await page.getByRole("button", { name: "Masukkan keranjang" }).click();
  await expect(page.getByText("Lengkapi pilihan")).toBeVisible();
  await page.getByLabel(/Oat/).check();
  await page.getByRole("button", { name: "Masukkan keranjang" }).click();
  await expect(page.getByRole("button", { name: /Lihat keranjang \(1 item\)/ })).toBeVisible();
});

test("tombol kembali menutup sheet tanpa meninggalkan menu", async ({ page }) => {
  await mockStore(page);
  await page.goto("/order/T1");
  await page.getByRole("button", { name: "Pilih Caffe Latte" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.goBack();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByRole("heading", { level: 1, name: "Kopi Senja" })).toBeVisible();
});

test("meja tutup tetap menampilkan menu tanpa tombol pesan", async ({ page }) => {
  await mockStore(page, { ordering_open: false });
  await page.goto("/order/T1");
  await expect(page.getByText("Pemesanan lewat QR sedang tutup")).toBeVisible();
  await expect(page.getByText("Croissant")).toBeVisible();
  await expect(page.getByRole("button", { name: "Tambah Croissant" })).toHaveCount(0);
});

test("QR tidak dikenal menampilkan arahan ke staf", async ({ page }) => {
  await page.route("**/api/v1/public/qr/BAD", (r) => r.fulfill(json({ error: "x" }, 404)));
  await page.goto("/order/BAD");
  await expect(page.getByText("Meja tidak ditemukan")).toBeVisible();
});

test("warna gerai dipakai", async ({ page }) => {
  await mockStore(page, {}, { primary_hex: "#0B5D3B" });
  await page.goto("/order/T1");
  const add = page.getByRole("button", { name: "Tambah Croissant" });
  await expect(add).toHaveCSS("background-color", "rgb(11, 93, 59)");
});

test("nilai warna yang tidak valid diabaikan", async ({ page }) => {
  await mockStore(page, {}, { primary_hex: "red;}" });
  await page.goto("/order/T1");
  await expect(page.getByRole("button", { name: "Tambah Croissant" })).toHaveCSS("background-color", "rgb(44, 26, 18)");
});
