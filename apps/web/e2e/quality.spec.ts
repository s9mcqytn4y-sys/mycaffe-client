import AxeBuilder from "@axe-core/playwright";
import { test, expect, type Page } from "@playwright/test";
import { json, mockCheckout, mockStore } from "./mocks";

async function expectNoSeriousViolations(page: Page) {
  const { violations } = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21aa"]).analyze();
  const serious = violations.filter((v) => v.impact === "serious" || v.impact === "critical");
  expect(serious.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(" ")).join(", ")}`)).toEqual([]);
}

async function expectNoHorizontalScroll(page: Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(0);
}

async function openCart(page: Page) {
  await page.goto("/order/T1");
  await page.getByRole("button", { name: "Tambah Croissant" }).click();
  await page.getByRole("button", { name: /Lihat keranjang/ }).click();
  await expect(page.getByRole("dialog")).toContainText("Rp 28.750");
}

test.describe("axe", () => {
  test("menu", async ({ page }) => {
    await mockStore(page);
    await page.goto("/order/T1");
    await expect(page.getByRole("heading", { level: 1, name: "Kopi Senja" })).toBeVisible();
    await expectNoSeriousViolations(page);
  });

  test("keranjang", async ({ page }) => {
    await mockStore(page);
    await openCart(page);
    await expectNoSeriousViolations(page);
  });

  test("layar bayar", async ({ page }) => {
    await mockStore(page);
    await mockCheckout(page);
    await openCart(page);
    await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
    await expect(page.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();
    await expectNoSeriousViolations(page);
  });

  test("halaman depan dan legal", async ({ page }) => {
    await mockLegal(page);
    await page.goto("/");
    await expectNoSeriousViolations(page);
    await page.goto("/legal");
    await expect(page.getByRole("heading", { level: 2, name: "Ketentuan Pemesanan Tamu" })).toBeVisible();
    await expectNoSeriousViolations(page);
  });
});

const doc = (doc_type: string, title: string) => ({
  doc_type,
  title,
  version: "1.0.0",
  effective_at: "2026-11-10T00:00:00Z",
  content_sha256: "a".repeat(64),
  content_markdown: `Isi ${title}.`
});

async function mockLegal(page: Page) {
  await page.route("**/api/v1/public/legal*", (r) =>
    r.fulfill(
      json({
        documents: [
          doc("dpa_tenant", "Perjanjian Pemrosesan Data"),
          doc("guest_order_terms", "Ketentuan Pemesanan Tamu"),
          doc("guest_privacy_notice", "Pemberitahuan Privasi Tamu"),
          doc("terms_of_service", "Syarat Layanan Mitra")
        ]
      })
    )
  );
}

test("legal tamu hanya memuat naskah tamu, mitra hanya dokumen tenant", async ({ page }) => {
  await mockLegal(page);
  await page.goto("/legal");
  const docs = page.getByRole("navigation", { name: "Daftar dokumen" });
  await expect(docs.getByRole("link")).toHaveText(["Ketentuan Pemesanan Tamu", "Pemberitahuan Privasi Tamu"]);
  await docs.getByRole("link", { name: "Pemberitahuan Privasi Tamu" }).click();
  await expect(page).toHaveURL(/\/legal\?doc=guest_privacy_notice$/);
  await expect(page.getByText("Isi Pemberitahuan Privasi Tamu.")).toBeVisible();

  await page.getByRole("link", { name: "Dokumen untuk mitra gerai" }).click();
  await expect(page).toHaveURL(/\/legal\/mitra$/);
  await expect(page.getByRole("navigation", { name: "Daftar dokumen" }).getByRole("link")).toHaveText([
    "Perjanjian Pemrosesan Data",
    "Syarat Layanan Mitra"
  ]);
});

test("sitemap memuat halaman publik saja", async ({ request }) => {
  const body = await (await request.get("/sitemap.xml")).text();
  expect(body).toContain("/legal/mitra");
  expect(body).not.toContain("/order");
  expect(await (await request.get("/robots.txt")).text()).toContain("Sitemap:");
});

test("tamu bisa memesan hanya dengan keyboard", async ({ page, browserName }) => {
  test.skip(browserName === "webkit", "WebKit skips links and buttons on Tab unless the OS setting is on");
  await mockStore(page);
  await page.goto("/order/T1");
  const add = page.getByRole("button", { name: "Tambah Croissant" });
  for (let i = 0; i < 20 && !(await add.evaluate((el) => el === document.activeElement)); i++) await page.keyboard.press("Tab");
  await expect(add).toBeFocused();
  await page.keyboard.press("Enter");

  const cart = page.getByRole("button", { name: /Lihat keranjang/ });
  await cart.focus();
  await page.keyboard.press("Enter");
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  expect(await dialog.evaluate((el) => el.contains(document.activeElement))).toBe(true);

  await page.keyboard.press("Escape");
  await expect(dialog).toHaveCount(0);
});

test.describe("reflow pada lebar 320 px", () => {
  test.use({ viewport: { width: 320, height: 640 } });

  test("menu, keranjang, dan layar bayar tanpa gulir samping", async ({ page }) => {
    await mockStore(page);
    await mockCheckout(page);
    await page.goto("/order/T1");
    await expect(page.getByRole("heading", { level: 1, name: "Kopi Senja" })).toBeVisible();
    await expectNoHorizontalScroll(page);

    await page.getByRole("button", { name: "Tambah Croissant" }).click();
    await page.getByRole("button", { name: /Lihat keranjang/ }).click();
    await expect(page.getByRole("dialog")).toContainText("Rp 28.750");
    await expectNoHorizontalScroll(page);

    await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
    await expect(page.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();
    await expectNoHorizontalScroll(page);
  });
});

test.describe("kartu QR pada layar 360 x 640", () => {
  test.use({ viewport: { width: 360, height: 640 } });

  test("nominal dan batas bayar terlihat tanpa gulir", async ({ page }) => {
    await mockStore(page);
    await mockCheckout(page);
    await page.goto("/order/T1");
    await page.getByRole("button", { name: "Tambah Croissant" }).click();
    await page.getByRole("button", { name: /Lihat keranjang/ }).click();
    await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
    await expect(page.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();
    await expect(page.getByText("Rp 28.750", { exact: true })).toBeInViewport({ ratio: 1 });
    await expect(page.getByText(/Bayar sebelum/)).toBeInViewport({ ratio: 1 });
  });
});
