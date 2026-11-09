// Run with: node --experimental-strip-types src/features/order/cart.test.mjs
import assert from "node:assert/strict";
import { cartReducer, toOrderItems } from "./cart.ts";

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
console.log("cart ok");
