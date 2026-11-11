import { test, expect, type Page } from "@playwright/test";
import { CATALOG, json, mockCheckout, mockStore } from "./mocks";

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
  await expect(page.getByRole("list", { name: "Perjalanan pesanan" }).locator('[aria-current="step"]')).toHaveText("Sedang disiapkan");
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

async function addAndOpenCart(page: Page) {
  await page.goto("/order/T1");
  await page.getByRole("button", { name: "Tambah Croissant" }).click();
  await page.getByRole("button", { name: /Lihat keranjang/ }).click();
  await expect(page.getByRole("dialog")).toContainText("Rp 28.750");
}

test("reload di layar bayar menampilkan QR yang sama tanpa pesanan baru", async ({ page }) => {
  await mockStore(page);
  let orderPosts = 0;
  let paymentPosts = 0;
  let sessionPosts = 0;
  await page.route("**/api/v1/public/qr/T1/session", (r) => {
    sessionPosts += 1;
    return r.fulfill(json({ table_number: "A1" }, 201));
  });
  await page.route("**/api/v1/public/guest/orders", (r) => {
    if (r.request().method() === "GET") return r.fulfill(json({ orders: [{ id: "o1" }] }));
    orderPosts += 1;
    return r.fulfill(json({ order: { id: "o1", order_number: "A-015" }, tracking_token: "trk" }, 201));
  });
  await page.route("**/api/v1/public/guest/orders/o1/payments", (r) => {
    paymentPosts += 1;
    return r.fulfill(json({ payment: { id: "p1", qr_string: "00020101", amount: 28750 } }, 201));
  });
  await page.route("**/api/v1/public/guest/ws-ticket", (r) => r.fulfill(json({}, 503)));
  await page.route("**/api/v1/public/orders/track/trk", (r) => r.fulfill(json({ status: "pending_payment", payment_status: "unpaid" })));

  await addAndOpenCart(page);
  await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
  await expect(page.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();

  await page.reload();
  await expect(page.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();
  await expect(page.locator("svg").first()).toBeVisible();
  expect(orderPosts).toBe(1);
  expect(paymentPosts).toBe(1);
  expect(sessionPosts).toBe(1);

  await page.getByRole("button", { name: "Kembali ke menu" }).click();
  await expect(page.getByRole("button", { name: /Lihat keranjang/ })).toHaveCount(0);
  await page.getByRole("button", { name: "Lanjutkan pembayaran" }).click();
  await expect(page.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();
});

for (const [code, status, message] of [
  ["QUOTE_CHANGED", 409, "Harga berubah"],
  ["UNAVAILABLE", 409, "baru saja habis"],
  ["UNPAID_ORDER_LIMIT", 409, "pesanan sebelumnya"],
  ["QR_ORDERING_PAUSED", 409, "dihentikan"],
  ["STORE_CLOSED", 409, "Gerai sedang tutup"],
  ["GUEST_SESSION_EXPIRED", 401, "Tekan Bayar sekali lagi"]
] as const) {
  test(`kode ${code} memberi pesan yang tepat dan keranjang tetap utuh`, async ({ page }) => {
    await mockStore(page);
    await page.route("**/api/v1/public/guest/orders", (r) => r.fulfill(json({ code, error: "x" }, status)));
    await addAndOpenCart(page);
    await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
    await expect(page.getByRole("dialog").getByRole("alert")).toContainText(message);
    await expect(page.getByRole("dialog")).toContainText("Croissant");
  });
}

test("pesanan batal tidak tampil sebagai diterima", async ({ page }) => {
  await mockStore(page);
  await page.route("**/api/v1/public/guest/orders", (r) => r.fulfill(json({ order: { id: "o1", order_number: "A-016" }, tracking_token: "trk" }, 201)));
  await page.route("**/api/v1/public/guest/orders/o1/payments", (r) => r.fulfill(json({ payment: { id: "p1", qr_string: "00020101", amount: 28750 } }, 201)));
  await page.route("**/api/v1/public/guest/ws-ticket", (r) => r.fulfill(json({}, 503)));
  await page.route("**/api/v1/public/orders/track/trk", (r) => r.fulfill(json({ status: "cancelled", payment_status: "paid", order_number: "A-016" })));
  await addAndOpenCart(page);
  await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
  await expect(page.getByRole("heading", { name: "Pesanan dibatalkan" })).toBeVisible();
  await expect(page.getByText("pengembalian dana")).toBeVisible();
  await expect(page.getByText("Pesanan diterima")).toHaveCount(0);
});

test("tab ditutup sebelum bayar: membuka URL meja lagi menampilkan QR yang sama", async ({ context, page }) => {
  await mockStore(page);
  await mockCheckout(page);
  await addAndOpenCart(page);
  await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
  await expect(page.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();
  // The record is written in an effect after paint; close the tab only once it is stored.
  await expect
    .poll(() => page.evaluate(() => Object.keys(localStorage).some((k) => k.startsWith("cassy_active_order_"))))
    .toBe(true);
  await page.close();

  const again = await context.newPage();
  await mockStore(again);
  let orderPosts = 0;
  await mockCheckout(again);
  await again.route("**/api/v1/public/guest/orders", (r) => {
    if (r.request().method() === "GET") return r.fulfill(json({ orders: [{ id: "o1" }] }));
    orderPosts += 1;
    return r.fulfill(json({ order: { id: "o2" } }, 201));
  });
  await again.goto("/order/T1");
  await expect(again.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();
  expect(orderPosts).toBe(0);
});

test("menu satu daftar: tap kategori melompat ke bagiannya", async ({ page }) => {
  await mockStore(page);
  const breads = Array.from({ length: 8 }, (_, i) => ({
    id: `b${i}`,
    category_id: "bread",
    name: `Roti ${i + 1}`,
    available: true,
    variants: [{ id: `vb${i}`, name: "Reguler", price: 18000, is_default: true, available: true }],
    modifier_groups: []
  }));
  await page.route("**/api/v1/public/stores/senja/catalog", (r) =>
    r.fulfill(json({ categories: [...CATALOG.categories, { id: "bread", name: "Roti" }], products: [...CATALOG.products, ...breads] }))
  );
  await page.goto("/order/T1");
  await expect(page.getByRole("heading", { level: 2, name: "Kopi" })).toBeVisible();
  const nav = page.getByRole("navigation", { name: "Kategori menu" });
  await nav.getByRole("button", { name: "Roti" }).click();
  await expect(page.getByRole("heading", { level: 2, name: "Roti" })).toBeInViewport();
  await expect(nav.getByRole("button", { name: "Roti" })).toHaveAttribute("aria-current", "true");
});

test("pesanan siap: toast muncul dan langkah siap diberi jam", async ({ page }) => {
  await mockStore(page);
  let status = "preparing";
  await mockCheckout(page);
  await page.route("**/api/v1/public/orders/track/trk", (r) =>
    r.fulfill(json({ status, payment_status: "paid", order_number: "A-014" }))
  );
  await addAndOpenCart(page);
  await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
  await expect(page.getByRole("heading", { name: "Pembayaran diterima" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Pembayaran diterima" }).locator("xpath=preceding-sibling::span[1]")).toHaveCSS("animation-name", "pop");

  status = "ready";
  // Back online makes the tracker check at once instead of waiting for the 10 s poll.
  await page.evaluate(() => window.dispatchEvent(new Event("online")));
  await expect(page.getByRole("status").filter({ hasText: "Pesanan Anda siap diambil" })).toBeVisible();
  await expect(page.getByRole("list", { name: "Perjalanan pesanan" }).locator("li").nth(2)).toContainText(/\d{2}[.:]\d{2}/);
});

test("mode antar meja: empat langkah, toast saat diantar, pilihan bunyi diingat", async ({ page }) => {
  await mockStore(page, { service_mode: "table" });
  let status = "ready";
  await mockCheckout(page);
  await page.route("**/api/v1/public/orders/track/trk", (r) =>
    r.fulfill(json({ status, payment_status: "paid", order_number: "A-014" }))
  );
  await addAndOpenCart(page);
  await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
  const steps = page.getByRole("list", { name: "Perjalanan pesanan" }).locator("li");
  await expect(steps).toHaveCount(4);
  await expect(page.getByText("Status: Sedang diantar ke meja Anda")).toBeVisible();
  // Ready means "on its way" here, so the pickup toast must not fire.
  await expect(page.getByText("Pesanan Anda siap diambil")).toHaveCount(0);

  await page.getByRole("button", { name: "Matikan bunyi" }).click();
  await expect(page.getByRole("button", { name: "Nyalakan bunyi" })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem("cassy_sound_off"))).toBe("1");

  status = "served";
  await page.evaluate(() => window.dispatchEvent(new Event("online")));
  await expect(page.getByRole("status").filter({ hasText: "Pesanan sudah diantar" })).toBeVisible();
  await expect(steps.nth(3)).toHaveAttribute("aria-current", "step");
});

test("koneksi putus menampilkan banner dan hilang saat tersambung", async ({ page, context }) => {
  await mockStore(page);
  await page.goto("/order/T1");
  await expect(page.getByRole("heading", { level: 1, name: "Kopi Senja" })).toBeVisible();
  await context.setOffline(true);
  await expect(page.getByRole("alert").filter({ hasText: "Koneksi terputus" })).toBeVisible();
  await context.setOffline(false);
  await expect(page.getByText("Koneksi terputus")).toHaveCount(0);
});

test("animasi hasil bayar mati saat reduced motion", async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await mockStore(page);
  await mockCheckout(page, { status: "queued", payment_status: "paid" });
  await addAndOpenCart(page);
  await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
  const icon = page.getByRole("heading", { name: "Pembayaran diterima" }).locator("xpath=preceding-sibling::span[1]");
  await expect(icon).toHaveCSS("animation-name", "none");
});

test("judul tab tetap nama gerai dan meja setelah tamu berinteraksi", async ({ page }) => {
  await mockStore(page);
  await page.goto("/order/T1");
  await expect(page).toHaveTitle("Kopi Senja | Meja A1");
  await page.getByRole("button", { name: "Tambah Croissant" }).click();
  await page.getByRole("button", { name: /Lihat keranjang/ }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page).toHaveTitle("Kopi Senja | Meja A1");
});

test("keranjang tidak menampilkan varian tunggal", async ({ page }) => {
  await mockStore(page);
  await addAndOpenCart(page);
  await expect(page.getByRole("dialog")).not.toContainText("Reguler");
});

test("bayar dengan GoPay membuka aplikasi lalu bisa pindah ke QRIS", async ({ page }) => {
  await mockStore(page, { payment_methods: ["qris", "gopay", "shopeepay"] });
  await mockCheckout(page);
  const bodies: { method?: string; idempotency_key?: string }[] = [];
  await page.route("**/api/v1/public/guest/orders/o1/payments", (r) => {
    const body = r.request().postDataJSON();
    bodies.push(body);
    return body.method === "gopay"
      ? r.fulfill(json({ payment: { id: "p2", method: "gopay", amount: 28750, deeplink_url: "gojek://gopay/merchanttransfer?tref=1" } }, 201))
      : r.fulfill(json({ payment: { id: "p3", method: "qris", qr_string: "00020101", amount: 28750 } }, 201));
  });

  await addAndOpenCart(page);
  await page.getByRole("dialog").getByText("GoPay", { exact: true }).click();
  await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
  await expect(page.getByRole("link", { name: "Buka aplikasi untuk membayar" })).toHaveAttribute("href", "gojek://gopay/merchanttransfer?tref=1");
  expect(bodies[0].method).toBe("gopay");

  await page.getByRole("button", { name: "QRIS" }).click();
  await expect(page.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();
  expect(bodies[1].method).toBe("qris");
  expect(bodies[1].idempotency_key).not.toBe(bodies[0].idempotency_key);
});

test("deeplink berbahaya tidak dijadikan tautan", async ({ page }) => {
  await mockStore(page, { payment_methods: ["qris", "shopeepay"] });
  await mockCheckout(page);
  await page.route("**/api/v1/public/guest/orders/o1/payments", (r) =>
    r.fulfill(json({ payment: { id: "p2", method: "shopeepay", amount: 28750, deeplink_url: "javascript:alert(1)" } }, 201))
  );
  await addAndOpenCart(page);
  await page.getByRole("dialog").getByText("ShopeePay", { exact: true }).click();
  await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
  await expect(page.getByRole("heading", { name: "ShopeePay" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Buka aplikasi untuk membayar" })).toHaveCount(0);
  await expect(page.getByText("Metode ini sedang tidak tersedia")).toBeVisible();
});

test("URL yang tidak ada menampilkan arahan pindai ulang", async ({ page }) => {
  await page.goto("/tidak-ada");
  await expect(page.getByRole("heading", { name: "Halaman tidak ditemukan" })).toBeVisible();
});

test("field nama menjelaskan tujuan data dan menaut privasi", async ({ page }) => {
  await mockStore(page);
  await addAndOpenCart(page);
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByText("memanggil pesanan")).toBeVisible();
  await expect(dialog.getByRole("link", { name: "Ketentuan dan privasi" })).toHaveAttribute("href", "/legal");
});

test("keranjang: menu sama dengan pilihan beda jadi dua baris berlabel jelas sebelum total dihitung", async ({ page }) => {
  await mockStore(page);
  const latte = {
    id: "latte",
    category_id: "coffee",
    name: "Caffe Latte",
    available: true,
    variants: [
      { id: "v-r", name: "Reguler", price: 32000, is_default: true, available: true },
      { id: "v-b", name: "Besar", price: 38000, is_default: false, available: true }
    ],
    modifier_groups: [
      {
        id: "milk",
        name: "Susu",
        required: true,
        min: 1,
        max: 1,
        options: [
          { id: "oat", name: "Oat", price_delta: 6000, available: true },
          { id: "almond", name: "Almond", price_delta: 7000, available: true }
        ]
      }
    ]
  };
  await page.route("**/api/v1/public/stores/senja/catalog", (r) => r.fulfill(json({ categories: CATALOG.categories, products: [latte] })));
  // The quote never answers: the cart must still tell the two lines apart on its own.
  await page.route("**/api/v1/public/stores/senja/orders/quote", () => {});
  await page.goto("/order/T1");

  for (const [size, milk] of [["Reguler", "Oat"], ["Besar", "Almond"]]) {
    await page.getByRole("button", { name: "Pilih Caffe Latte" }).click();
    await page.getByLabel(new RegExp(size)).check();
    await page.getByLabel(new RegExp(milk)).check();
    await page.getByRole("button", { name: "Masukkan keranjang" }).click();
  }
  await page.getByRole("button", { name: /Lihat keranjang/ }).click();
  const rows = page.getByRole("dialog").getByRole("listitem");
  await expect(rows).toHaveCount(2);
  await expect(rows.nth(0)).toContainText("Reguler, Oat");
  await expect(rows.nth(1)).toContainText("Besar, Almond");
  await expect(page.getByRole("button", { name: "Tambah Caffe Latte, Besar, Almond" })).toBeVisible();
});

test.describe("footer di dasar layar 390 x 844", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  const footerBottom = (page: import("@playwright/test").Page) =>
    page.evaluate(() => Math.round(document.querySelector("footer")!.getBoundingClientRect().bottom + scrollY));

  test("halaman pendek: footer menempel di bawah", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator("footer")).toBeVisible();
    expect(await footerBottom(page)).toBe(844);
  });

  test("bar keranjang tidak menutupi tautan footer", async ({ page }) => {
    await mockStore(page);
    await page.goto("/order/T1");
    await page.getByRole("button", { name: "Tambah Croissant" }).click();
    await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
    const link = page.locator("footer").getByRole("link", { name: "Ketentuan dan privasi" });
    const bar = page.locator("[data-cart-bar]");
    const linkBox = (await link.boundingBox())!;
    const barBox = (await bar.boundingBox())!;
    expect(linkBox.y + linkBox.height).toBeLessThanOrEqual(barBox.y);
  });
});
