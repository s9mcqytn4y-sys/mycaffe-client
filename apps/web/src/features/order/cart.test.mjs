// Run with: node --experimental-strip-types src/features/order/cart.test.mjs
import assert from "node:assert/strict";
import { cartReducer, optionNames, toOrderItems } from "./cart.ts";
import { checkoutMessageKey, moneyTaken, orderPhase } from "./status.ts";

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
console.log("cart ok");
