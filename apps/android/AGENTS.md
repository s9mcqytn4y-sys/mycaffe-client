# Cassy POS Android Tablet: AGENTS.md

## Ecosystem Context & Role
This sub-workspace is **Cassy POS Native Android Tablet application** (`apps/android` inside `C:\Projects\MyCaffeClient`), operated under legal application ID `com.azuredevid.cassy`.

### Ecosystem Topology
1. `C:\Projects\myCaffe-backend`: Multi-tenant Go 1.24 & PostgreSQL 18 backend providing REST API and real-time WebSocket order events.
2. `C:\Projects\mycaffe-wt-sc`: Loyalty stamp card engine worktree.
3. `C:\Projects\MyCaffeClient`: Client monorepo parent root.
4. `C:\Projects\MyCaffeClient\apps\android`: This POS tablet application (Kotlin 2.0 / Jetpack Compose).
5. `C:\Projects\MyCaffeClient\apps\web`: Guest QR table ordering PWA (Next.js 15).
6. `C:\Projects\MyCaffeClient\apps\ios`: iOS hardware HAL bridge.
7. `C:\Users\Acer\OneDrive\Documents\AzureDevId`: Brand identity assets and legal merchant documentation.

## Core POS Architecture & Guidelines
- **UI Architecture (10-55-35 Rhythm)**:
  - `PosNavRail.kt`: 10% left rail for station/category switching, quick tender actions, and store indicator.
  - `PosCatalogGrid.kt`: 55% center grid displaying menu items with responsive cards and modifier dialogs.
  - `PosOrderSummaryPanel.kt`: 35% right sticky bill preview with table number, dine-in / takeaway toggle, delta round dispatch ("Kirim ke Dapur"), and tender buttons.
  - `PosCashModal.kt`: Quick tender cash modal with auto change calculator.
  - `PosQrisModal.kt`: Dynamic QRIS modal with ZXing in-memory bitmap rendering, Cart Lock, WebSocket status listener, and manual Force Sync fallback.
- **Hardware Integration & Printing**:
  - `PrinterPort9100Manager.kt`: Raw TCP socket port 9100 ESC/POS connection with 2.500 ms timeout, `tcpNoDelay = true`, `DLE EOT 4` paper-out status query, and RJ11 drawer kick pulse.
  - `KdsTicketRouter.kt`: Dynamic kitchen printer routing supporting arbitrary station IDs (Barista, Hot Kitchen, Cold Pastry, Runner) with auto-redirect failover and watermark header `*** PENGALIHAN PRINTER ***`.
  - `LanPrinterDiscoveryManager.kt`: Dual mDNS NSD + UDP subnet broadcast probe (ports 9100 & 3289).
- **Store Pairing**:
  - `CameraQrScannerView.kt` + `StorePairingScreen.kt`: CameraX 1.4.0 and ML Kit barcode analyzer for zero-config outlet pairing via `GET /api/v1/mobile/bootstrap`.
- **Accessibility & Internationalization**:
  - Full dual-language ID/EN via `stringResource(R.string.xxx)` in `res/values/strings.xml` and `res/values-en/strings.xml`.
  - WCAG 2.2 AA compliant minimum touch target of $48 \times 48\text{dp}$. TalkBack `contentDescription` on all interactive icon elements.
- **Anti-Slop & Anti-Emoji**:
  - Zero hardcoded mock strings or prices. All currency formatted via `java.text.NumberFormat` as integer IDR.
  - No emojis anywhere in code, XML, or UI strings.

## Build & Test Commands
```bash
./gradlew test            # Run unit tests (KdsTicketRouterTest, etc.)
./gradlew assembleDebug   # Assemble debug APK
./gradlew bundleDebug     # Build production/test App Bundle (AAB)
```

<!-- antislop:start -->
## antislop
For UI, copy, people, mobile layout, or code comments work, read `antislop.md` (core) and then the skill for the task:
- UI / visual: `skills/antislop-ui/SKILL.md`
- Copy & text: `skills/antislop-copywriting/SKILL.md`
- People: `skills/antislop-human/SKILL.md`
- Mobile / responsive: `skills/antislop-layoutmobile/SKILL.md`
- Code comments: `skills/antislop-code/SKILL.md`
<!-- antislop:end -->


## Rilis pertama
Baca `apps/android/docs/ReleaseOnePlan.md` sebelum mengerjakan layar baru. Semua operasional tenant berada di aplikasi ini, dan cakupan serta urutan PR ada di dokumen itu.
