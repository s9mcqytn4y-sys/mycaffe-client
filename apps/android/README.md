# Cassy POS Android Tablet Application

Official Native Android Tablet POS app for **MyCaffe by AzureDevID** (`com.azuredevid.cassy`).

## Target Hardware & Form Factor
- **Form Factor**: 10" to 12" Android Tablets in **Landscape** orientation ($2560 \times 1600$ @ 320 dpi / $1920 \times 1200$ @ 240 dpi).
- **Minimum SDK**: Android 8.0 (API level 26).
- **Target SDK**: Android 14+ / 16 (API level 34 - 36).
- **Architecture**: Modular 10-55-35 Jetpack Compose rhythm (`PosNavRail`, `PosCatalogGrid`, `PosOrderSummaryPanel`, `PosCashModal`, `PosQrisModal`).

## Hardware & Network Drivers
1. **Network ESC/POS Printer Driver (`PrinterPort9100Manager.kt`)**:
   - Raw TCP Socket port 9100.
   - 2.500 ms connect and write timeout with retry backoff.
   - Real-time `DLE EOT 4` paper-out status telemetry.
   - Drawer kick pulse command (`ESC p 0 25 250`).
2. **KDS Ticket Routing (`KdsTicketRouter.kt`)**:
   - Dynamic station routing (`barista`, `hot_kitchen`, `cold_pastry`, `runner`).
   - Auto-redirect failover when a station printer is offline with `*** PENGALIHAN PRINTER ***` banner.
3. **Printer Discovery (`LanPrinterDiscoveryManager.kt`)**:
   - Dual mDNS NSD (`_pdl-datastream._tcp`) + UDP subnet broadcast probe (ports 9100 & 3289).
4. **Dynamic QRIS Bitmap (`QrBitmapGenerator.kt`)**:
   - In-memory ZXing matrix rendering with high-contrast espresso ink.
   - Cart Lock during payment session.
   - WebSocket status listener with manual Force Sync fallback button.
5. **CameraX Store Pairing (`CameraQrScannerView.kt` & `StorePairingScreen.kt`)**:
   - Fast ML Kit barcode detector with state lock on valid signed QR JSON payload.

## Developer Quickstart
```bash
# Run unit tests
./gradlew test

# Assemble debug APK
./gradlew assembleDebug

# Build App Bundle (AAB)
./gradlew bundleDebug
```

