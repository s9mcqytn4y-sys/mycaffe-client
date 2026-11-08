# MyCaffe Guest Ordering & Backoffice PWA: CLAUDE.md

## Ecosystem Context & Role
This sub-workspace is the **Guest Table Ordering & Owner Backoffice PWA** (`apps/web` inside `C:\Projects\MyCaffeClient`).
Built with **Next.js 15 App Router**, React 19, TypeScript, and Tailwind CSS v4.

### Ecosystem Topology
1. `C:\Projects\myCaffe-backend`: Core Go 1.24 & PostgreSQL 18 RLS API (`mycaffe_app`).
2. `C:\Projects\mycaffe-wt-sc`: Loyalty stamp card engine worktree.
3. `C:\Projects\MyCaffeClient`: Client monorepo parent root.
4. `C:\Projects\MyCaffeClient\apps\android`: Cassy POS tablet (`com.azuredevid.cassy`).
5. `C:\Projects\MyCaffeClient\apps\web`: This Next.js 15 PWA web application.
6. `C:\Projects\MyCaffeClient\apps\ios`: iOS hardware HAL bridge.
7. `C:\Users\Acer\OneDrive\Documents\AzureDevId`: Corporate brand identity, legal licenses, and merchant documentation.

## Guidelines
- Server Components by default; minimal Client Components.
- Use `useTenantWebSocket` with exponential backoff for live order updates.
- Adhere to *Espresso & Crema* tokens (`#FAF6F1`, `#2C1A12`, `#C68B59`, `#7C4318`).
- Never perform client-side price or tax calculations.
- Zero emojis in UI or code.

<!-- antislop:start -->
## antislop
For UI, copy, people, mobile layout, or code comments work, read `antislop.md` (core) and then the skill for the task:
- UI / visual: `skills/antislop-ui/SKILL.md`
- Copy & text: `skills/antislop-copywriting/SKILL.md`
- People: `skills/antislop-human/SKILL.md`
- Mobile / responsive: `skills/antislop-layoutmobile/SKILL.md`
- Code comments: `skills/antislop-code/SKILL.md`
<!-- antislop:end -->

