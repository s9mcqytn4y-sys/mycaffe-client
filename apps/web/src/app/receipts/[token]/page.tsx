import type { Metadata } from "next";
import { ReceiptView } from "@/features/receipt/ReceiptView";

// The token is a bearer link from the receipt email: keep it out of search results and referrers.
export const metadata: Metadata = {
  title: "Struk pesanan",
  robots: { index: false, follow: false },
  referrer: "no-referrer"
};

export default async function ReceiptPage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;
  return <ReceiptView token={token} />;
}
