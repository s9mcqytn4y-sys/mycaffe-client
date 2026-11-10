import type { Metadata } from "next";
import { Suspense } from "react";
import { LegalView } from "../LegalView";

export const metadata: Metadata = {
  title: "Dokumen untuk mitra gerai",
  description: "Syarat layanan, kebijakan privasi, pengembalian dana, dan perjanjian pemrosesan data bagi pemilik gerai pengguna Cassy.",
  alternates: { canonical: "/legal/mitra" },
  robots: { index: true, follow: true }
};

export default function LegalPartnerPage() {
  return (
    <Suspense>
      <LegalView audience="tenant" />
    </Suspense>
  );
}
