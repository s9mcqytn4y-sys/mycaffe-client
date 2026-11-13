# Cassy POS tablet (`apps/android`)

Read the root `AGENTS.md` first, then `docs/ReleaseOnePlan.md` before building a screen. Scope and PR order live there.

## Architecture (PRD v4 Q11)
One module, strict layers per feature: `ui` (Compose + ViewModel) -> `domain` (models, repository interfaces) -> `data` (Ktor implementations). Koin for DI, one shared Ktor client, DataStore with Tink for secrets. No Paging 3 and no Retrofit in Release One.

## Rules
- Totals come from `POST /api/v1/pos/orders/quote` (`OrderQuoteRepository`). Never compute tax or service on the device.
- Theme colours must match `packages/design-tokens/tokens.json`; no Espresso & Crema values.
- Icons: Material Symbols via `androidx.compose.material.icons`, each with a `contentDescription` or `null` when decorative.
- All UI text in `res/values/strings.xml` and `res/values-en/strings.xml`.
- Touch targets 48 dp or more. Rupiah formatted as integers.
- Printing: raw TCP port 9100 ESC/POS with a 2,500 ms timeout (`PrinterPort9100Manager`), kitchen routing in `KdsTicketRouter`.

## Commands
```bash
./gradlew testDevDebugUnitTest bundleDevDebug
```

## Headless Emulator Test Workflow
1. Build APK lebih dulu tanpa Gradle daemon agar commit memory Windows tetap bebas:
   `./gradlew assembleDevDebug --no-daemon`
2. Hentikan AVD lain jika ada (`adb emu kill`), lalu start tablet AVD headless (AVD tablet API 36 mengalokasikan 4096 MB RAM):
   `emulator -avd Medium_Tablet -no-window -no-audio -no-boot-anim`
3. Tunggu boot selesai: `adb wait-for-device shell 'while [ -z "$(getprop sys.boot_completed)" ]; do sleep 1; done'`.
4. Pasang APK langsung via `adb` (jangan jalankan `./gradlew` saat emulator menyala agar tidak kehabisan commit memory):
   `adb -s <serial> install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk`
   lalu jalankan: `adb -s <serial> shell am start -W -n com.azuredevid.cassy.dev/com.azuredevid.cassy.MainActivity`.
5. Inspeksi UI & verifikasi: `android layout --device <serial> --pretty` dan `android screen capture --device <serial> --output <path>.png` (atau `adb -s <serial> exec-out screencap -p > <path>.png` jika headless GPU menghasilkan frame hitam).

