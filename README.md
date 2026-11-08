# Cassy Client Monorepo: Multi-Platform Hospitality Suite

[![Android CI](https://github.com/s9mcqytn4y-sys/mycaffe-client/actions/workflows/android-ci.yml/badge.svg)](https://github.com/s9mcqytn4y-sys/mycaffe-client/actions)
[![Web CI](https://github.com/s9mcqytn4y-sys/mycaffe-client/actions/workflows/web-ci.yml/badge.svg)](https://github.com/s9mcqytn4y-sys/mycaffe-client/actions)

Official multi-platform client applications for **MyCaffe by AzureDevID** (PT Azure Dev Indonesia, Jakarta Pusat, DKI Jakarta).

```
                      +-----------------------------+
                      |   MyCaffe Backend (Go/PG18) |
                      +--------------+--------------+
                                     |
              +----------------------+----------------------+
              |                      |                      |
              v                      v                      v
     +-----------------+    +-----------------+    +-----------------+
     | Cassy POS Tablet|    | Guest Order PWA |    | iOS Hardware    |
     | (apps/android)  |    | (apps/web)      |    | Bridge (apps/ios|
     +-----------------+    +-----------------+    +-----------------+
              |                      |
              v                      v
     +-----------------+    +-----------------+
     | ESC/POS Printers|    | WebSocket Real- |
     | (Port 9100 KDS) |    | time Sync Hub   |
     +-----------------+    +-----------------+
```

---

## Applications & Structure

| Package / App | Technology | Role | Status |
|:---|:---|:---|:---|
| **`apps/android`** | Kotlin 2.0, Jetpack Compose, CameraX, ZXing | **Cassy POS**: Native Android Tablet POS with dynamic KDS ticket routing, port 9100 ESC/POS printing, and offline fallback. | Production Ready |
| **`apps/web`** | Next.js 15 App Router, React 19, Tailwind CSS v4 | **Guest Ordering PWA**: White-label mobile web for QR table ordering, dynamic menu, and checkout. | Production Ready |
| **`apps/ios`** | Objective-C, Foundation, CoreBluetooth | **Hardware HAL**: Low-level receipt printing driver for iOS point-of-sale setups. | Active |
| **`packages/design-tokens`** | JSON & CSS Variables | **Design Tokens**: Official *Espresso & Crema* tokens verified for WCAG 2.2 AA contrast. | Active |

---

## Core Capabilities

### 1. Cassy POS Tablet (`apps/android`)
- **Visual Ergonomics**: 10-55-35 landscape rhythm (10% Navigation Rail, 55% Catalog Grid, 35% Order Summary Panel).
- **KDS Kitchen Ticket Routing**: Multi-printer routing by station (Barista, Hot Kitchen, Cold Pastry, Runner) with auto-redirect failover and watermark alerts.
- **Hardware Abstraction**: Network ESC/POS printer driver over raw TCP socket port 9100 with `DLE EOT 4` status query, RJ11 drawer kick pulse, and 2.500 ms connection timeout.
- **Dynamic QRIS Generation**: In-memory ZXing bitmap generation with Cart Lock, tenant-isolated WebSocket event listening, and manual Force Sync fallback.
- **Store Pairing**: CameraX 1.4.0 + ML Kit barcode scanner for zero-configuration outlet pairing via `/api/v1/mobile/bootstrap`.
- **Accessibility & i18n**: Full dual-language Indonesian (`values`) & English (`values-en`), TalkBack semantics, and minimum 48x48 dp touch targets.

### 2. Guest Table Order PWA (`apps/web`)
- **Next.js 15 App Router**: Server Components for instant catalog loading, zero client bundle bloat.
- **Real-Time Sync**: Resilient `useTenantWebSocket` hook with exponential backoff and automatic heartbeat.
- **PWA Capabilities**: Installable web application with dynamic brand manifests and Cloudflare R2 CDN preconnection.

---

## Development Setup

### Prerequisites
- Node.js >= 22.0.0 and `pnpm` >= 9.0.0
- Java JDK 17 (Eclipse Temurin or OpenJDK)
- Android SDK 34/35/36 with Android Build Tools
- Go 1.24 (for running backend pairing tests)

### Commands
```bash
# Install root dependencies
pnpm install

# Run Web PWA locally
pnpm --filter web dev

# Run Android unit tests
cd apps/android
./gradlew test

# Build Android App Bundle (AAB)
./gradlew bundleDebug
```

---

## Brand & Licensing
Operated by **AzureDevID** (PT Azure Dev Indonesia). All rights reserved.
Legal Package Identifier: `com.azuredevid.cassy`.
Visual System: *Espresso & Crema* (`#FAF6F1`, `#2C1A12`, `#C68B59`, `#8A4F22`).
