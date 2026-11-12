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
./gradlew test bundleDebug
```
