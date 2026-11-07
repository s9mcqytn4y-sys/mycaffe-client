# DESIGN.md: MyCaffe by AzureDevID (Unified Client Design System)

## 1. Brand Architecture & Aesthetic Direction
- **Relationship**: Endorsed Dual-Brand (`MyCaffe by AzureDevID`).
- **Operational Surfaces (`MyCaffe` Default)**: *Espresso & Crema* tactile hospitality workspace. Warm alabaster canvas (`#FAF6F1`), high-contrast espresso ink (`#2C1A12`), roasted crema accents (`#C68B59`), zero blur/glassmorphism overhead on low-power Android POS tablets, instant scanability at 2 meters for KDS kitchen screens.
- **Platform Governance Surfaces (`AzureDevID` Superadmin & Legal)**: Precision enterprise control plane. Obsidian slate dark header (`#07111F`), Royal Azure primary (`#0A4DA2`), Electric Cyan active telemetry (`#00AEEF`), crisp white data cards (`#FFFFFF` on `#F8FAFC`).
- **White-Label Tenant Override**: Tenants customize `--tenant-primary`, `--tenant-accent`, logo, and hero banner via `store_cms_configs`, validated server-side and client-side to enforce >= 4.5:1 WCAG 2.2 AA text contrast.

## 2. Semantic Color Tokens
```css
:root {
  /* MyCaffe Default (Espresso & Crema) */
  --bg-canvas: #FAF6F1;
  --bg-surface: #FFFFFF;
  --bg-muted: #F2EBE2;
  --text-primary: #2C1A12;
  --text-secondary: #5C4738;
  --border-subtle: #E6DDD3;
  --brand-primary: #2C1A12;
  --brand-accent: #C68B59;

  /* AzureDevID Platform & Superadmin */
  --azd-primary: #0A4DA2;
  --azd-cyan: #00AEEF;
  --azd-obsidian: #07111F;
  --azd-ink: #0B192C;

  /* Operational Status (POS / KDS / Tracker / Network Simulator) */
  --state-ok-bg: #DCFCE7;
  --state-ok-fg: #166534;
  --state-warn-bg: #FFEDD5;
  --state-warn-fg: #9A3412;
  --state-err-bg: #FEE2E2;
  --state-err-fg: #991B1B;
}
```

## 3. Typography, Money Formatting & i18n Rules
- **Display / Headings**: `Plus Jakarta Sans`, weights `600`, `700`, `800`.
- **Body / POS Grid / Menu**: `Inter`, weights `400`, `500`, `600` with mandatory `font-variant-numeric: tabular-nums` on every price, quantity, queue code, and timer.
- **Technical / Telemetry / Order IDs**: `JetBrains Mono`, weight `500`.
- **Money Invariant**: Integer Indonesian Rupiah formatted as `Rp 28.000` (`id-ID` dot thousands separator, no fractional cents).
- **Locales**: `id-ID` primary (natural Indonesian hospitality phrasing), `en-US` secondary toggle on Customer QR Table Order.
- **Anti-Slop Copy Rule**: Never use em-dashes in UI copy, alerts, receipts, or empty states. Keep button labels concrete (`Bayar Sekarang`, `Kirim ke Dapur`, `Uji Koneksi Printer`).

## 4. Accessibility & Touch Ergonomics (`ui-a11y` + `antislop-layoutmobile`)
- Minimum touch target: `44x44px` on mobile QR menu; `48x48px` on POS cashier keypad, modifier checkboxes, and KDS bump buttons.
- Never convey order/payment/network status by color alone; always pair semantic badge color with explicit status label + icon.
- Maintain visible `:focus-visible` `2px solid #0A4DA2` ring for keyboard and USB HID barcode scanner navigation.
