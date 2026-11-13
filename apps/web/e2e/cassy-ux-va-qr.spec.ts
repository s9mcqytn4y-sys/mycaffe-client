import fs from "node:fs/promises";
import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";
import { CATALOG, json, mockCheckout, mockStore } from "./mocks";

test.describe("Cassy UX, VA Bank Transfer, Branded QR & A11y", () => {
  test("Bank Transfer VA end-to-end (BRI -> Mandiri), copy VA, anti-zoom 16px input, and axe", async ({ page }) => {
    const paymentRequests: Array<{ method?: string; bank?: string }> = [];
    await mockStore(page, { payment_methods: ["qris", "gopay", "shopeepay", "bank_transfer"] });
    await page.route("**/api/v1/public/guest/orders", (r) =>
      r.request().method() === "GET"
        ? r.fulfill(json({ orders: [] }))
        : r.fulfill(json({ order: { id: "o-va", order_number: "A-108" }, tracking_token: "trk-va" }, 201))
    );
    await page.route("**/api/v1/public/guest/orders/o-va/payments", async (r) => {
      const payload = r.request().postDataJSON() as { method?: string; bank?: string };
      paymentRequests.push(payload);
      if (payload.bank === "mandiri") {
        return r.fulfill(
          json(
            {
              payment: {
                id: "p-va-m",
                method: "bank_transfer",
                bank: "mandiri",
                biller_code: "70012",
                bill_key: "990011223344",
                amount: 28750,
                expires_at: new Date(Date.now() + 15 * 60_000).toISOString()
              }
            },
            201
          )
        );
      }
      return r.fulfill(
        json(
          {
            payment: {
              id: "p-va-1",
              method: "bank_transfer",
              bank: payload.bank ?? "bca",
              va_number: "880012345678",
              amount: 28750,
              expires_at: new Date(Date.now() + 15 * 60_000).toISOString()
            }
          },
          201
        )
      );
    });
    await page.route("**/api/v1/public/guest/ws-ticket", (r) => r.fulfill(json({}, 503)));
    await page.route("**/api/v1/public/orders/track/trk-va", (r) =>
      r.fulfill(json({ order_number: "A-108", status: "pending_payment", payment_status: "unpaid" }))
    );

    await page.goto("/order/T1");
    await page.getByRole("button", { name: "Tambah Croissant" }).click();
    await page.getByRole("button", { name: /Lihat keranjang/ }).click();

    const guestInput = page.locator("#guest-name");
    const fontSizePx = await guestInput.evaluate((el) => parseFloat(getComputedStyle(el).fontSize));
    expect(fontSizePx).toBeGreaterThanOrEqual(16);

    await page.getByText("Transfer Bank (VA)", { exact: true }).click();
    await page.getByText("BRI", { exact: true }).click();
    await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();

    await expect(page.getByRole("heading", { name: "Transfer Bank (VA)" })).toBeVisible();
    await expect(page.getByTestId("va-number")).toHaveText("880012345678");
    expect(paymentRequests[0]).toMatchObject({ method: "bank_transfer", bank: "bri" });

    // Copy VA feedback (stub clipboard for headless reliability).
    await page.evaluate(() => {
      Object.defineProperty(navigator, "clipboard", {
        value: { writeText: async () => undefined },
        configurable: true
      });
    });
    await page.getByRole("button", { name: "Salin nomor VA" }).click();
    await expect(page.getByText("Disalin ke clipboard.")).toBeVisible();

    // Switch to Mandiri bill_key + biller_code display
    await page.getByText("Mandiri", { exact: true }).click();
    await expect(page.getByText("70012")).toBeVisible();
    await expect(page.getByText("990011223344")).toBeVisible();
    expect(paymentRequests[1]).toMatchObject({ method: "bank_transfer", bank: "mandiri" });

    const { violations } = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21aa"]).analyze();
    const serious = violations.filter((v) => v.impact === "serious" || v.impact === "critical");
    expect(serious).toEqual([]);
  });

  test("Branded Cassy QR download (SVG + PNG) and toast feedback", async ({ page }) => {
    await mockStore(page, { payment_methods: ["qris"] });
    await mockCheckout(page);
    await page.goto("/order/T1");
    await page.getByRole("button", { name: "Tambah Croissant" }).click();
    await page.getByRole("button", { name: /Lihat keranjang/ }).click();
    await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();

    await expect(page.getByRole("heading", { name: "Pindai QRIS" })).toBeVisible();

    const [svgDownload] = await Promise.all([
      page.waitForEvent("download"),
      page.getByRole("button", { name: "Simpan SVG" }).click()
    ]);
    expect(svgDownload.suggestedFilename()).toMatch(/\.svg$/);
    const svgPath = await svgDownload.path();
    expect(svgPath).toBeTruthy();
    const svgText = await fs.readFile(svgPath!, "utf-8");
    expect(svgText).toContain("Cassy");
    expect(svgText).toContain("#1F5F52");

    const [pngDownload] = await Promise.all([
      page.waitForEvent("download"),
      page.getByRole("button", { name: "Unduh QR" }).click()
    ]);
    expect(pngDownload.suggestedFilename()).toMatch(/\.png$/);
    await expect(page.getByText("Kode QR Cassy disimpan.")).toBeVisible();
  });

  test("Maintenance, error & empty illustrations + email validation", async ({ page }) => {
    await page.route("**/api/v1/public/qr/MAINT", (r) => r.fulfill(json({ code: "MAINTENANCE" }, 503)));
    await page.goto("/order/MAINT");
    await expect(page.locator('img[src="/brand/illustrations/maintenance-state.jpg"]')).toBeVisible();

    await page.route("**/api/v1/public/qr/MISSING", (r) => r.fulfill(json({ code: "NOT_FOUND" }, 404)));
    await page.goto("/order/MISSING");
    await expect(page.locator('img[src="/brand/illustrations/error-state.jpg"]')).toBeVisible();

    // Paid screen validates receipt email before network request.
    await mockStore(page, { payment_methods: ["qris"] });
    await mockCheckout(page, { status: "ready", payment_status: "paid" });
    await page.goto("/order/T1");
    await page.getByRole("button", { name: "Tambah Croissant" }).click();
    await page.getByRole("button", { name: /Lihat keranjang/ }).click();
    await page.getByRole("button", { name: "Bayar Rp 28.750" }).click();
    await expect(page.getByRole("heading", { name: "Pembayaran diterima" })).toBeVisible();

    await page.locator("#receipt-email").fill("bukan-email");
    await page.getByRole("button", { name: "Kirim" }).click();
    await expect(page.getByText("Masukkan alamat email yang benar.")).toBeVisible();
  });

  test("English locale switch renders VA & QR labels cleanly", async ({ page }) => {
    await mockStore(page, { payment_methods: ["qris", "bank_transfer"] });
    await page.addInitScript(() => localStorage.setItem("cassy_locale", "en-US"));
    await page.goto("/order/T1");
    await expect(page.getByRole("heading", { level: 1, name: "Kopi Senja" })).toBeVisible();
    await page.getByRole("button", { name: "Add Croissant" }).click();
    await page.getByRole("button", { name: /View cart/ }).click();
    await expect(page.getByText("Bank Transfer (VA)")).toBeVisible();
    expect(CATALOG.categories.length).toBe(1);
  });
});
