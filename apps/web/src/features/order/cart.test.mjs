// Run with: node --experimental-strip-types src/features/order/cart.test.mjs
import assert from "node:assert/strict";
import { cartReducer, optionNames, toOrderItems } from "./cart.ts";
import { checkoutMessageKey, isFinal, moneyTaken, orderPhase, progressStep, readActiveOrder, reconnectDelay, writeActiveOrder } from "./status.ts";
import { menuSections } from "./menu.ts";

const latte = { productId: "p1", variantId: "v1", name: "Latte", options: [], quantity: 1, notes: "" };
const oat = { ...latte, options: [{ optionId: "oat", subOptionIds: ["b", "a"] }] };
const oatReordered = { ...latte, options: [{ optionId: "oat", subOptionIds: ["a", "b"] }] };

let cart = cartReducer([], { type: "add", line: latte });
cart = cartReducer(cart, { type: "add", line: latte });
assert.equal(cart.length, 1);
assert.equal(cart[0].quantity, 2);

cart = cartReducer(cart, { type: "add", line: oat });
cart = cartReducer(cart, { type: "add", line: oatReordered });
assert.equal(cart.length, 2, "same selection in another order is one line");
assert.equal(cart[1].quantity, 2);

cart = cartReducer(cart, { type: "setQuantity", key: cart[0].key, quantity: 0 });
assert.equal(cart.length, 1, "quantity zero removes the line");

assert.deepEqual(toOrderItems(cart)[0].modifiers, [
  { option_id: "oat", quantity: 1, sub_addons: [{ option_id: "b", quantity: 1 }, { option_id: "a", quantity: 1 }] }
]);
assert.deepEqual(optionNames("[]"), [], "an empty snapshot shows nothing");
assert.deepEqual(optionNames(undefined), []);
assert.deepEqual(optionNames("not json"), []);
assert.deepEqual(
  optionNames('[{"option_name":"Oat","sub_addons":[{"option_name":"Extra"}]},{"option_name":"Less sugar"}]'),
  ["Oat", "Extra", "Less sugar"]
);
assert.equal(orderPhase(null), "awaiting");
assert.equal(orderPhase({ status: "pending_payment", payment_status: "failed" }), "awaiting", "a failed QR can be retried");
assert.equal(orderPhase({ status: "pending_payment", payment_status: "partially_paid" }), "awaiting");
assert.equal(orderPhase({ status: "queued", payment_status: "paid" }), "paid");
assert.equal(orderPhase({ status: "served", payment_status: "partially_refunded" }), "paid");
assert.equal(orderPhase({ status: "cancelled", payment_status: "expired" }), "cancelled");
assert.equal(orderPhase({ status: "cancelled", payment_status: "paid" }), "cancelled", "cancellation beats payment");
assert.equal(orderPhase({ status: "served", payment_status: "refunded" }), "refunded");
assert.equal(orderPhase({ status: "served", payment_status: "chargeback" }), "refunded");
assert.equal(moneyTaken({ status: "cancelled", payment_status: "paid" }), true);
assert.equal(moneyTaken({ status: "cancelled", payment_status: "expired" }), false);

assert.equal(checkoutMessageKey("QUOTE_CHANGED"), "priceChanged");
assert.equal(checkoutMessageKey("UNAVAILABLE"), "itemUnavailable");
assert.equal(checkoutMessageKey("GUEST_SESSION_EXPIRED"), "sessionExpired");
assert.equal(checkoutMessageKey("SOMETHING_NEW"), "checkoutFailed");
assert.equal(checkoutMessageKey(undefined), "checkoutFailed");

assert.equal(isFinal({ status: "served", payment_status: "paid" }), true);
assert.equal(isFinal({ status: "cancelled", payment_status: "expired" }), true);
assert.equal(isFinal({ status: "queued", payment_status: "refunded" }), true);
assert.equal(isFinal({ status: "preparing", payment_status: "paid" }), false, "the kitchen is still working");
assert.equal(isFinal(null), false);
assert.deepEqual([0, 1, 2, 3, 4, 5, 9].map(reconnectDelay), [1000, 2000, 4000, 8000, 16000, 30000, 30000]);

const store = new Map();
globalThis.localStorage = {
  getItem: (k) => store.get(k) ?? null,
  setItem: (k, v) => store.set(k, v),
  removeItem: (k) => store.delete(k)
};
const active = { order: { id: "o1", tracking: "trk", payKey: "pay_1" }, payment: null };
writeActiveOrder("T1", active, 0);
assert.deepEqual(readActiveOrder("T1", 3 * 60 * 60 * 1000), active, "still valid at exactly three hours");
assert.equal(readActiveOrder("T1", 3 * 60 * 60 * 1000 + 1), null, "older than the guest session is dropped");
assert.equal(store.size, 0, "an expired record is removed");
store.set("cassy_active_order_T2", JSON.stringify(active));
assert.equal(readActiveOrder("T2"), null, "a record without savedAt is ignored");
assert.deepEqual(["queued", "preparing", "ready", "served", "completed", undefined].map(progressStep), [0, 1, 2, 3, 3, 0]);

const product = (id, category_id) => ({ id, category_id });
const sections = menuSections(
  [{ id: "food", name: "Makanan" }, { id: "empty", name: "Kosong" }, { id: "drink", name: "Minuman" }],
  [product("tea", "drink"), product("toast", "food"), product("mystery", null), product("old", "archived")],
  "Lainnya"
);
assert.deepEqual(
  sections.map((s) => [s.name, s.products.map((p) => p.id)]),
  [["Makanan", ["toast"]], ["Minuman", ["tea"]], ["Lainnya", ["mystery", "old"]]],
  "category order kept, empty skipped, unknown last"
);
console.log("cart ok");
