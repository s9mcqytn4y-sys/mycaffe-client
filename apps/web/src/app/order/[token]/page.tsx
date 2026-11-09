import { OrderApp } from "@/features/order/OrderApp";

export default async function GuestOrderPage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;
  return <OrderApp token={token} />;
}
