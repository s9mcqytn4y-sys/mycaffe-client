# Cassy POS Android Tablet: CLAUDE.md

## Ecosystem Context & Role
This sub-workspace is **Cassy POS Native Android Tablet application** (`apps/android` inside `C:\Projects\MyCaffeClient`), operated under legal application ID `com.azuredevid.cassy`.

### Ecosystem Topology
1. `C:\Projects\myCaffe-backend`: Multi-tenant Go 1.24 & PostgreSQL 18 backend.
2. `C:\Projects\mycaffe-wt-sc`: Loyalty stamp card engine worktree.
3. `C:\Projects\MyCaffeClient`: Client monorepo parent root.
4. `C:\Projects\MyCaffeClient\apps\android`: This POS tablet application (Kotlin 2.0 / Jetpack Compose).
5. `C:\Projects\MyCaffeClient\apps\web`: Guest QR table ordering PWA (Next.js 15).
6. `C:\Projects\MyCaffeClient\apps\ios`: iOS hardware HAL bridge.
7. `C:\Users\Acer\OneDrive\Documents\AzureDevId`: Brand identity assets and legal merchant documentation.

## Core POS Architecture & Guidelines
- **UI Architecture (10-55-35 Rhythm)**: `PosNavRail.kt` (10%), `PosCatalogGrid.kt` (55%), `PosOrderSummaryPanel.kt` (35%), `PosCashModal.kt`, `PosQrisModal.kt`.
- **Hardware Integration**: Port 9100 TCP socket (`PrinterPort9100Manager.kt`, 2.500 ms timeout), dynamic station routing (`KdsTicketRouter.kt`), dual mDNS + UDP printer discovery (`LanPrinterDiscoveryManager.kt`).
- **Store Pairing**: CameraX 1.4.0 + ML Kit QR scanner (`CameraQrScannerView.kt` + `StorePairingScreen.kt`).
- **Accessibility & i18n**: All strings via `stringResource(R.string.xxx)` in `values/strings.xml` and `values-en/strings.xml`. Touch targets >= 48dp.
- **Anti-Emoji Policy**: Zero emojis in source code, XML strings, or UI.

## Testing & Quality Commands
```bash
./gradlew test            # Unit tests
./gradlew bundleDebug     # App bundle build
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
