import type { Page } from "@playwright/test";

export const json = (body: unknown, status = 200) => ({ status, contentType: "application/json", body: JSON.stringify(body) });

const variant = (id: string, price: number) => ({ id, name: "Reguler", price, is_default: true, available: true });

export const CATALOG = {
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

export const QUOTE = {
  subtotal_amount: 25000,
  discount_amount: 0,
  service_charge_amount: 1250,
  tax_amount: 2500,
  total_amount: 28750,
  quote_fingerprint: "fp",
  lines: [{ product_name: "Croissant", variant_name: "Reguler", quantity: 1, subtotal_amount: 25000 }]
};

export async function mockStore(page: Page, table: Record<string, unknown> = {}, cms: Record<string, unknown> = {}) {
  await page.route("**/api/v1/public/qr/T1", (r) =>
    r.fulfill(json({ store_name: "Kopi Senja", store_slug: "senja", table_number: "A1", label: "Meja A1", ordering_open: true, ...table }))
  );
  await page.route("**/api/v1/public/qr/T1/session", (r) => r.fulfill(json({ table_number: "A1" }, 201)));
  await page.route("**/api/v1/public/stores/senja/catalog", (r) => r.fulfill(json(CATALOG)));
  await page.route("**/api/v1/public/stores/senja/cms", (r) => r.fulfill(json({ cms })));
  await page.route("**/api/v1/public/stores/senja/orders/quote", (r) => r.fulfill(json(QUOTE)));
}

/** Order and payment routes for a guest who pays; `status` drives what the tracker reports. */
export async function mockCheckout(page: Page, status = { status: "pending_payment", payment_status: "unpaid" }) {
  await page.route("**/api/v1/public/guest/orders", (r) =>
    r.request().method() === "GET"
      ? r.fulfill(json({ orders: [{ id: "o1" }] }))
      : r.fulfill(json({ order: { id: "o1", order_number: "A-014" }, tracking_token: "trk" }, 201))
  );
  await page.route("**/api/v1/public/guest/orders/o1/payments", (r) =>
    r.fulfill(json({ payment: { id: "p1", qr_string: "00020101", amount: 28750, expires_at: new Date(Date.now() + 15 * 60_000).toISOString() } }, 201))
  );
  await page.route("**/api/v1/public/guest/ws-ticket", (r) => r.fulfill(json({}, 503)));
  await page.route("**/api/v1/public/orders/track/trk", (r) => r.fulfill(json({ order_number: "A-014", ...status })));
}
