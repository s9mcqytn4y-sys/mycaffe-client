import { LocaleProvider } from "@/components/Providers";
import { SiteFooter } from "@/components/SiteFooter";
import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import "./globals.css";

// Self-hosted (OFL, see fonts/OFL.txt) so a build never depends on reaching Google Fonts.
const font = localFont({ src: "./fonts/PlusJakartaSans-latin-wght.woff2", weight: "200 800", display: "swap" });

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_APP_URL || "https://abbdullaziz.online"),
  title: { default: "Pesan dari meja", template: "%s | Cassy" },
  description: "Pesan dan bayar dari meja gerai lewat kode QR.",
  manifest: "/manifest.webmanifest",
  applicationName: "Cassy",
  // Table QR links are private to each table, so the app stays out of search results.
  robots: { index: false, follow: false },
  icons: {
    icon: [
      { url: "/Favicon32.png", sizes: "32x32", type: "image/png" },
      { url: "/Favicon.ico", sizes: "any" }
    ],
    apple: [{ url: "/AppleTouchIcon180.png", sizes: "180x180", type: "image/png" }]
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
      <body
        className={`${font.className} flex min-h-dvh flex-col bg-canvas text-ink antialiased`}
        style={{ "--font-display": font.style.fontFamily } as React.CSSProperties}
      >
        <LocaleProvider>
          <a
            href="#main-content"
            className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand focus:px-4 focus:py-2 focus:text-brand-fg"
          >
            Lewati ke konten utama
          </a>
          <main id="main-content" className="flex-1">
            {children}
          </main>
          <SiteFooter />
        </LocaleProvider>
      </body>
    </html>
  );
}
