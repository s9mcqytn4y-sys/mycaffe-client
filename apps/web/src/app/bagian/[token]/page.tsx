import type { Metadata } from "next";
import { SharePayView } from "@/features/split/SharePayView";

// The token is a bearer link for one person's share: keep it out of search results and referrers.
export const metadata: Metadata = {
  title: "Bayar bagian",
  robots: { index: false, follow: false },
  referrer: "no-referrer"
};

export default async function SharePage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;
  return <SharePayView token={token} />;
}
