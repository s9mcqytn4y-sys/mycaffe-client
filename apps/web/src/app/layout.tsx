import { ThemeLocaleProvider } from "@/components/Providers";
import { SiteFooter, TopNavBar } from "@/components/Shell";
import type { Metadata, Viewport } from "next";
import "./globals.css";

// Reading this as: Multi-tenant hospitality POS, QR Table Order PWA, and SaaS Governance Console
// for Indonesian cafe owners, staff, and guests, in the Espresso & Crema + AzureDevID Control Plane
// visual language, dial ENERGY 2 / RHYTHM 2 / MOTION 1.

export const metadata: Metadata = {
  title: "MyCaffe by AzureDevID | Kasir POS, Pesan Meja QR & Tata Kelola Kafe",
  description:
    "Platform operasional kafe dan restoran multi-tenant oleh AzureDevID: pesan meja QR tanpa antre, kasir POS tablet, layar dapur KDS, integrasi QRIS Midtrans, dan generator jaringan MikroTik.",
  manifest: "/manifest.webmanifest",
  applicationName: "MyCaffe by AzureDevID"
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#2C1A12"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="id" data-theme="light" suppressHydrationWarning>
      <body className="flex min-h-dvh flex-col bg-canvas text-ink antialiased">
        <ThemeLocaleProvider>
          <a
            href="#main-content"
            className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand focus:px-4 focus:py-2.5 focus:text-sm focus:font-semibold focus:text-brand-fg"
          >
            Lewati ke konten utama
          </a>
          <TopNavBar />
          <main id="main-content" className="flex-1">
            {children}
          </main>
          <SiteFooter />
        </ThemeLocaleProvider>
      </body>
    </html>
  );
}

