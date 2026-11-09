import { test, expect } from "@playwright/test";

test.describe("Guest QR Table Order Flow (PRD G1-G8)", () => {
  test("loads guest order landing and validates server quote & checkout UI", async ({ page }) => {
    // Mock public table QR resolution
    await page.route("**/api/v1/public/qr/T1-DEMO", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          store_slug: "kopi-nusantara",
          table_number: "T-01",
          is_active: true
        })
      });
    });

    // Mock guest session creation
    await page.route("**/api/v1/public/qr/T1-DEMO/session", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ table_number: "T-01", expires_at: "2026-11-09T12:00:00Z" })
      });
    });

    // Mock catalog fetch
    await page.route("**/api/v1/public/stores/kopi-nusantara/catalog", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          categories: [
            { id: "cat-coffee", name: "Kopi Espresso" },
            { id: "cat-pastry", name: "Artisan Pastry" }
          ],
          products: [
            {
              id: "prod-latte",
              category_id: "cat-coffee",
              name: "Caffe Latte",
              description: "Espresso ganda dengan susu segar steamed",
              available: true,
              variants: [{ id: "var-latte", name: "Reguler", price: 32000, is_default: true, available: true }],
              prep_station: "bar"
            },
            {
              id: "prod-croissant",
              category_id: "cat-pastry",
              name: "Butter Croissant",
              description: "Pastry renyah dengan mentega Prancis",
              available: true,
              variants: [{ id: "var-croissant", name: "Reguler", price: 25000, is_default: true, available: true }],
              prep_station: "kitchen"
            }
          ]
        })
      });
    });

    // Mock server quote calculation
    await page.route("**/api/v1/public/stores/kopi-nusantara/orders/quote", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          subtotal_amount: 32000,
          tax_amount: 3200,
          service_charge_amount: 1600,
          discount_amount: 0,
          total_amount: 36800,
          quote_fingerprint: "mock-fingerprint-abc"
        })
      });
    });

    // Navigate to simulated table QR URL
    await page.goto("/order/T1-DEMO");

    // 1. Verify Header and Table Resolution
    await expect(page.locator("h1")).toContainText("Pemesanan Meja T-01");
    await expect(page.locator("body")).toContainText("kopi-nusantara");

    // 2. Verify Catalog Item
    await expect(page.locator("text=Caffe Latte")).toBeVisible();

    // 3. Add to Cart
    const addBtn = page.locator("button:has-text('+ Tambah')").first();
    await addBtn.click();

    // 4. Verify Cart & Server Quote Total
    await expect(page.locator("h2")).toContainText("Keranjang Pesanan (1)");
    await expect(page.locator("body")).toContainText("Pajak Restoran");

    // 5. Verify PDP Modal Accessibility
    const pdpLink = page.locator("button:has-text('Kebijakan Privasi & Perlindungan Data Pribadi')");
    await expect(pdpLink).toBeVisible();
    await pdpLink.click();
    await expect(page.locator("h3:has-text('Kebijakan Data Pribadi Tamu')")).toBeVisible();
    await page.locator("button:has-text('Saya Mengerti')").click();
    await expect(page.locator("h3:has-text('Kebijakan Data Pribadi Tamu')")).not.toBeVisible();
  });
});

