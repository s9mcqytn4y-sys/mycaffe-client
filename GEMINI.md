# Cassy Client Monorepo: GEMINI.md

## Ecosystem Context & Architecture
`MyCaffeClient` is the official client monorepo for **Cassy by AzureDevID** (usaha perorangan AzureDevID, Jakarta Pusat, DKI Jakarta).
It provides the complete front-facing suite for multi-tenant hospitality businesses:
- `apps/android`: **Cassy POS** native tablet application in Jetpack Compose (`com.azuredevid.cassy`) with CameraX store pairing, ESC/POS Port 9100 KDS ticket routing, dynamic ZXing QRIS generation, and Biometric Keystore security.
- `apps/web`: **Guest Table Ordering & Management PWA** in Next.js 15 App Router, React 19, Tailwind CSS v4, and real-time WebSocket connection.
- `apps/ios`: **iOS Hardware Bridge & HAL** in Objective-C for serial and network receipt printers.
- `packages/design-tokens`: Shared design tokens (*Espresso & Crema* palette, typography, elevation, spacing).

### The 7 Ecosystem Directories
1. `C:\Projects\myCaffe-backend`: Core POS engine in Go 1.24 and PostgreSQL 18 RLS (`mycaffe_app`).
3. `C:\Projects\MyCaffeClient`: Client monorepo root (this workspace).
4. `C:\Projects\MyCaffeClient\apps\android`: Cassy POS Native Android Tablet application (`com.azuredevid.cassy`).
5. `C:\Projects\MyCaffeClient\apps\web`: Next.js 15 PWA Guest Table Ordering & Owner Backoffice web client.
6. `C:\Projects\MyCaffeClient\apps\ios`: iOS hardware abstraction layer and native printer bridge.
7. `C:\Users\Acer\OneDrive\Documents\AzureDevId`: Corporate brand identity, legal licenses (HAKI, PSE), payment gateway merchant docs, and recovery keys.

## Core Engineering Principles
- **No Hardcoded Values**: All store configurations, tenant IDs, catalog seeds, tax/service rates, payment credentials, and station routes must come dynamically from the backend API or authenticated session bootstrap (`/api/v1/mobile/bootstrap`).
- **Money & Precision**: Currency amounts are integer Indonesian Rupiah (`int64` / `BIGINT`). Never use floating-point arithmetic.
- **Fail-Closed Security**: In the absence of an authenticated store binding, active session, or valid token, apps must fail-closed (`503` / locked screen).
- **Anti-Emoji Policy**: Zero emojis in source code, user interface copy, logs, or Git commit messages.
- **Brand Consistency**: Product brand is `Cassy` by AzureDevID. `MyCaffe` is the former name and survives only in technical identifiers. Visual tokens *Espresso & Crema* (`#FAF6F1`, `#2C1A12`, `#C68B59`, `#8A4F22`).
- **Clean & DRY Principle**: Modularize UI components into dedicated files. Follow SOLID principles.

## Official CLI Toolchain & Agent Plugins
- **Android CLI**: `C:\Users\Acer\AppData\AndroidCLI\android.exe` (v1.1+, managed via `android sdk`, `android emulator`, `android preview render`).
- **GitHub CLI**: `C:\Program Files\GitHub CLI\gh.exe` (issues, PRs, actions).
- **Android Debug Bridge (ADB)**: `C:\Users\Acer\scoop\shims\adb.exe` (device communication, shell input, logcat).
- **Official Android Agent Plugins**:
  - `android-cli-plugin`: `device-hub` sidecar for live device mirroring & control, and `compose-preview` sidecar for instant Compose `@Preview` rendering.

<!-- antislop:start -->
## antislop
For UI, copy, people, mobile layout, or code comments work, read `antislop.md` (core) and then the skill for the task:
- UI / visual: `skills/antislop-ui/SKILL.md`
- Copy & text: `skills/antislop-copywriting/SKILL.md`
- People: `skills/antislop-human/SKILL.md`
- Mobile / responsive: `skills/antislop-layoutmobile/SKILL.md`
- Code comments: `skills/antislop-code/SKILL.md`
<!-- antislop:end -->

