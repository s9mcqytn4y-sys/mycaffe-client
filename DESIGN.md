# DESIGN.md: MyCaffe by AzureDevID (Unified Client Design System)

> Reading this as: Multi-tenant hospitality POS, QR Table Order PWA, and SaaS Governance Console for Indonesian cafe owners, staff, and guests, in the Espresso & Crema + AzureDevID Control Plane visual language, dial ENERGY 2 / RHYTHM 2 / MOTION 1.

## 1. Brand Architecture & Official Package Identity
- **Endorsed Dual-Brand**: `MyCaffe by AzureDevID` (operated by AzureDevID, Jakarta Pusat, DKI Jakarta, Indonesia).
- **Official Legal Package & App ID**: `online.abbdullaziz.mycaffe` across Android (`namespace` and `applicationId`), iOS (`CFBundleIdentifier`), and Web App Manifest (`id`).
- **Operational Surfaces (`MyCaffe` Default)**:
  - **Light Mode (`Espresso & Crema`)**: Warm alabaster canvas (`#FAF6F1`), crisp paper cards (`#FFFFFF`), high-contrast espresso ink (`#2C1A12`, 15.45:1 contrast), roasted crema accents (`#C68B59` paired only with `#2C1A12` dark ink, and `#8A4F22` deep roast accent text on light backgrounds for >= 5.5:1 WCAG AA contrast).
  - **Dark Mode (`Roasted Obsidian`)**: Deep roasted coffee bean canvas (`#140E0B`), elevated espresso surface (`#1E1612`), warm crema text (`#FAF6F1`, 16.8:1 contrast), secondary latte mist (`#D6C6B8`, 10.6:1 contrast), warm amber highlight (`#E0A976` with `#140E0B` dark ink on primary actions).
- **Platform Governance Surfaces (`AzureDevID` Superadmin & Legal)**: Precision enterprise control plane with Obsidian header (`#07111F`), Royal Azure primary (`#0A4DA2`), and Electric Cyan telemetry (`#00AEEF` on `#07111F`, 7.48:1 contrast).
- **White-Label Tenant Override**: Tenants customize `--tenant-primary`, `--tenant-accent`, logo URL, and hero banner via `store_cms_configs`, validated for >= 4.5:1 WCAG 2.2 AA text contrast.

## 2. Semantic Color Tokens (Light & Dark Verified >= 4.5:1)
```css
:root,
[data-theme="light"] {
  --bg-canvas: #FAF6F1;
  --bg-surface: #FFFFFF;
  --bg-muted: #F2EBE2;
  --text-primary: #2C1A12;
  --text-secondary: #5C4738;
  --border-subtle: #D8CCC0;
  --border-strong: #8C7360;
  --brand-primary: #2C1A12;
  --brand-on-primary: #FAF6F1;
  --brand-accent: #C68B59;
  --brand-accent-text: #7C4318;

  --azd-primary: #0A4DA2;
  --azd-cyan: #00AEEF;
  --azd-obsidian: #07111F;
  --azd-ink: #0B192C;

  --state-ok-bg: #DCFCE7;
  --state-ok-fg: #166534;
  --state-warn-bg: #FFEDD5;
  --state-warn-fg: #9A3412;
  --state-err-bg: #FEE2E2;
  --state-err-fg: #991B1B;
  --state-info-bg: #DBEAFE;
  --state-info-fg: #1E40AF;
}

[data-theme="dark"] {
  --bg-canvas: #140E0B;
  --bg-surface: #1E1612;
  --bg-muted: #2A1F1A;
  --text-primary: #FAF6F1;
  --text-secondary: #D6C6B8;
  --border-subtle: #4A382E;
  --border-strong: #8F7564;
  --brand-primary: #F0CFAF;
  --brand-on-primary: #140E0B;
  --brand-accent: #E0A976;
  --brand-accent-text: #F0CFAF;

  --state-ok-bg: #064E3B;
  --state-ok-fg: #A7F3D0;
  --state-warn-bg: #7C2D12;
  --state-warn-fg: #FED7AA;
  --state-err-bg: #7F1D1D;
  --state-err-fg: #FECACA;
  --state-info-bg: #1E3A8A;
  --state-info-fg: #BFDBFE;
}
```

## 3. Typography, Money Formatting & i18n Rules
- **Display / Headings**: `Plus Jakarta Sans`, weights `600`, `700`, `800` for distinct Indonesian geometric warmth and legibility at 2 meters on KDS screens.
- **Body / POS Grid / Menu**: `Inter`, weights `400`, `500`, `600` with mandatory `font-variant-numeric: tabular-nums` on every price, quantity, queue code, and timer so receipt columns never jitter.
- **Technical / Telemetry / Order IDs**: `JetBrains Mono`, weight `500` reserved strictly for SHA-256 legal hashes, Midtrans order IDs, IPv4/VLAN CIDRs, and `.rsc` RouterOS scripts.
- **Money Invariant**: Integer Indonesian Rupiah formatted as `Rp 28.000` (`id-ID` dot thousands separator, zero fractional cents).
- **Locales**: `id-ID` primary (natural Indonesian hospitality phrasing), `en-US` secondary toggle across Landing, Legal, and Guest QR Table Order.
- **Anti-Slop Copy Rule (R-02, R-15, R-16)**: Zero em-dashes anywhere in code, docs, or UI copy. Zero fabricated statistics or fictional reviews. Concrete action labels (`Ajukan Pendaftaran Gerai`, `Verifikasi Kode OTP`, `Bayar QRIS Sekarang`, `Unduh Skrip MikroTik .rsc`).

## 4. Documented Design Decisions (R-31 Rationale)
- **Why this color palette**: Warm `Espresso & Crema` (`#FAF6F1` / `#2C1A12`) reduces glare under cafe track lighting during 8-hour cashier shifts, while `Roasted Obsidian` (`#140E0B` / `#FAF6F1`) protects night-barista eyesight without sacrificing >= 4.5:1 WCAG AA contrast.
- **Why this layout & RHYTHM 2**: Each route matches its real operational workflow rather than a marketing template: asymmetric operational split header on `/`, linear 3-step KYB+OTP wizard on `/onboarding`, Navigation Rail (`1180px+` iPad Landscape) + bottom/top adaptive tabs (`390px`/`768px`) on `/pos`, and sticky thumb-zone cart sheet on `/order/[token]`.
- **Why this typography**: `Plus Jakarta Sans` anchors Indonesian hospitality identity on headings, `Inter` with `tabular-nums` aligns Rupiah ledger columns, and `JetBrains Mono` isolates cryptographic hashes and MikroTik VLAN CIDRs.
- **Why this spacing & border radius**: Compact mobile padding (`16px`) expands to `24px-32px` on tablet/desktop; cards use `12px` radius, inputs/buttons use `8px` radius, and status chips use `6px` structured tags (avoiding uniform pill slop per R-11).
- **Why cards and ledger rows**: Cards group stateful hardware/payment forms and KDS tickets that act as physical paper chits, while pricing, RBAC matrices, and invoices use structured tabular ledgers.
- **Why inline SVG QR & functional status icons**: Scannable QR codes (`qrcode` SVG) serve direct guest table scanning and Midtrans QRIS payment without third-party image trackers, and compact geometric status indicators always pair with explicit text labels (never color-only).
- **Why MOTION 1**: Rapid cashier entry and low-power Android POS tablets require instant `150ms` state transitions on `:hover` and `:focus-visible` with zero scroll-jacking, bounce, or blur overhead.

## 5. Accessibility & Touch Ergonomics (`ui-a11y` + `antislop-layoutmobile`)
- Minimum touch target: `44x44px` on mobile QR menu and public pages; `48x48px` on POS cashier keypad, PIN buttons, and KDS ticket transition controls.
- Never convey order, payment, KYB, or network status by color alone; always pair semantic badge background/foreground with an explicit status prefix and text label.
- Maintain visible `:focus-visible` `2px solid #0A4DA2` (`#38BDF8` in dark mode for > 7:1 contrast on `#140E0B`) ring with `2px` offset across all keyboard-operable elements, and ensure every modal closes via `Escape`.
