import { ThemeLocaleProvider } from "@/components/Providers";
import { SiteFooter, TopNavBar } from "@/components/Shell";
import type { Metadata, Viewport } from "next";
import "./globals.css";

// Reading this as: Multi-tenant hospitality POS, QR Table Order PWA, and SaaS Governance Console
// for Indonesian cafe owners, staff, and guests, in the Espresso & Crema + AzureDevID Control Plane
// visual language, dial ENERGY 2 / RHYTHM 2 / MOTION 1.

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_APP_URL || "https://abbdullaziz.online"),
  title: {
    default: "Cassy by AzureDevID | Kasir POS, Pesan Meja QR & Tata Kelola Kafe",
    template: "%s | Cassy by AzureDevID",
  },
  description:
    "Cassy oleh AzureDevID: Platform operasional kafe dan restoran multi-tenant, pesan meja QR tanpa antre, kasir POS tablet, layar dapur KDS, integrasi QRIS Midtrans, dan generator jaringan MikroTik.",
  keywords: [
    "kasir pos",
    "pos tablet indonesia",
    "pesan meja qr",
    "aplikasi kasir kafe",
    "kds dapur",
    "midtrans qris byok",
    "cassy pos",
    "azuredevid",
    "pos multi-tenant"
  ],
  authors: [{ name: "AzureDevID Engineering", url: "https://abbdullaziz.online" }],
  creator: "AzureDevID",
  publisher: "AzureDevID",
  manifest: "/manifest.webmanifest",
  applicationName: "Cassy by AzureDevID",
  alternates: {
    canonical: "/"
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-video-preview": -1,
      "max-image-preview": "large",
      "max-snippet": -1
    }
  },
  icons: {
    icon: [
      { url: "/Favicon16.png", sizes: "16x16", type: "image/png" },
      { url: "/Favicon32.png", sizes: "32x32", type: "image/png" },
      { url: "/Favicon48.png", sizes: "48x48", type: "image/png" },
      { url: "/Favicon.ico", sizes: "any" },
      { url: "/brand/cassy/CassyMonogramVector.svg", sizes: "any", type: "image/svg+xml" }
    ],
    apple: [
      { url: "/AppleTouchIcon180.png", sizes: "180x180", type: "image/png" },
    ],
  },
  openGraph: {
    title: "Cassy by AzureDevID | Kasir POS, Pesan Meja QR & Tata Kelola Kafe",
    description: "Cassy: Sistem Kasir POS & QR Order Meja Multi-Tenant dengan 0% Komisi & Enkripsi Kunci Mandiri (BYOK).",
    url: "https://abbdullaziz.online",
    siteName: "Cassy by AzureDevID",
    locale: "id_ID",
    type: "website",
    images: [
      {
        url: "/brand/AzureDevIdHeroBannerDark.webp",
        width: 1200,
        height: 630,
        alt: "Cassy by AzureDevID Hospitality Operating Platform"
      }
    ]
  },
  twitter: {
    card: "summary_large_image",
    title: "Cassy by AzureDevID | Kasir POS & Pesan Meja QR",
    description: "Platform operasional kafe dan restoran multi-tenant modern dengan QRIS Midtrans & KDS.",
    images: ["/brand/AzureDevIdHeroBannerDark.webp"],
    creator: "@azuredevid"
  }
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#2C1A12"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="id" data-theme="light" suppressHydrationWarning>
      <head>
        <link rel="preconnect" href="https://cdn.abbdullaziz.online" crossOrigin="anonymous" />
        <link rel="dns-prefetch" href="https://cdn.abbdullaziz.online" />
      </head>
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
