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
    await page.route("**/api/v1/public/legal*", (r) =>
      r.fulfill(json({ documents: [{ doc_type: "guest_privacy_notice", title: "Pemberitahuan Privasi Tamu", version: "1.0.0-draft", content_sha256: "a".repeat(64), content_markdown: "Isi." }] }))
    );
    await page.goto("/");
    await expectNoSeriousViolations(page);
    await page.goto("/legal");
    await expect(page.getByRole("heading", { name: "Pemberitahuan Privasi Tamu" })).toBeVisible();
    await expectNoSeriousViolations(page);
  });
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
