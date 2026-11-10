import type { Metadata } from "next";
import { OrderApp } from "@/features/order/OrderApp";

// No title from the layout: OrderApp names the tab after the outlet and table once the QR resolves,
// and a streamed default <title> would otherwise replace it right after hydration.
export const metadata: Metadata = { title: null };

export default async function GuestOrderPage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;
  return <OrderApp token={token} />;
}
