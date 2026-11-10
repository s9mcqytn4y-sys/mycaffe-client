import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: { absolute: "Cassy: pesan dan bayar dari meja" },
  description: "Pindai kode QR di meja gerai untuk melihat menu, memesan, dan membayar dengan QRIS dari ponsel Anda.",
  alternates: { canonical: "/" },
  robots: { index: true, follow: true }
};

export default function ScanPromptPage() {
  return (
    <div className="mx-auto flex max-w-md flex-col gap-4 px-6 py-24">
      <h1 className="font-display text-3xl font-bold text-ink">Pindai QR di meja Anda untuk memesan</h1>
      <p className="text-base leading-relaxed text-subink">
        Menu dan pembayaran terbuka dari kode QR yang tersedia di meja gerai. Kalau kodenya tidak terbaca, minta bantuan
        staf gerai.
      </p>
      <Link href="/legal" className="min-h-[44px] py-2 text-sm font-semibold text-ink underline">
        Ketentuan dan kebijakan privasi
      </Link>
    </div>
  );
}
