# Cassy client monorepo

Single source of agent instructions for this repo. `CLAUDE.md` and `GEMINI.md` only import this file. Each app folder has its own `AGENTS.md` with app rules; read both.

## What this is
Client apps for **Cassy by AzureDevID**: ordering and POS for food and beverage businesses in Indonesia (restaurants, cafes, bakeries, food courts, cloud kitchens, kiosks). `MyCaffe` is the former name and survives only in technical identifiers.

| Path | App |
|---|---|
| `apps/web` | Guest QR ordering PWA: Next.js 15 App Router, React 19, Tailwind CSS v4 |
| `apps/android` | Cassy POS tablet, Jetpack Compose, `com.azuredevid.cassy` |
| `apps/ios` | Objective-C printer bridge, no UI |
| `packages/design-tokens` | `tokens.json`, the single source of colour for every app |

Related folders: backend `C:\Projects\myCaffe-backend` (Go, PostgreSQL 18 RLS, owns all prices, taxes and state); brand and legal vault `C:\Users\Acer\OneDrive\Documents\AzureDevId` (never touch `SecurityRecovery` or `MerchantPaymentGateway`).

Current product decisions: `docs/PrdCassyV4.md`. Open debt: `docs/TechDebt.md`. Android plan: `apps/android/docs/ReleaseOnePlan.md`.

## Rules
- **Server is the source of truth.** Prices, taxes, service charges, totals and payment state come from the API. Clients never calculate them.
- **Money is integer rupiah** (`Long` / `number` without fractions). No floating point.
- **No hardcoded tenant data.** Store config, tenant ids, catalog, rates and printer routes come from the API or the authenticated bootstrap.
- **Fail closed.** Without a valid session, store binding or token, show a locked or error state.
- **Brand (PRD v4).** Pandan `#1F5F52`, kunyit `#E9A23B`, putih tulang `#F7F5F0`, tinta `#17211E`. No brown. The outlet colour leads the guest app; without one the guest app uses neutral ink. Plus Jakarta Sans for product UI, Bricolage Grotesque for brand display.
- **Icons.** Web uses `lucide-react`; Android uses Material Symbols (`androidx.compose.material.icons`). No custom icon packs.
- **No emoji** in code, UI copy, logs or commits.
- **Copy** is everyday, polite Bahasa Indonesia with English in `values-en` / `messages.ts`. Short sentences, verb first, clear numbers.
- Small focused files, SOLID, no dead code.

## Branches and checks
Trunk-based: short `feat/*`, `fix/*`, `docs/*`, `chore/*` branches into `main` by PR. Conventional Commits. Merge only when CI is green and the owner approves.

| Area | Command |
|---|---|
| Tokens | `pnpm tokens:verify` |
| Web | `pnpm --filter @azuredevid/mycaffe-web typecheck`, `test:unit`, `test:e2e` |
| Android | `apps/android/gradlew -p apps/android test bundleDebug` |
| iOS | `pnpm ios:lint-objc` |

## Tooling
`gh` for GitHub, `adb` at `C:\Users\Acer\scoop\shims\adb.exe`, Android CLI at `C:\Users\Acer\AppData\AndroidCLI\android.exe`. Never print `.env` contents or credentials.
