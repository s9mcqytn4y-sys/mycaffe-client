import type { Metadata } from "next";
import { Suspense } from "react";
import { LegalView } from "./LegalView";

export const metadata: Metadata = {
  title: "Ketentuan dan privasi tamu",
  description: "Pemberitahuan privasi dan ketentuan pemesanan bagi tamu yang memesan dari meja lewat kode QR Cassy.",
  alternates: { canonical: "/legal" },
  robots: { index: true, follow: true }
};

export default function LegalPage() {
  return (
    <Suspense>
      <LegalView audience="guest" />
    </Suspense>
  );
}
