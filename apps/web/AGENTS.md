# MyCaffe Guest Ordering & Backoffice PWA: AGENTS.md

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

## Core Web Architecture & Guidelines
- **Next.js 15 App Router**: Server Components by default for high-speed initial rendering and SEO. Client Components (`"use client"`) only for interactive widgets (cart, modifier drawer, payment selector).
- **Responsive Layout**: Mobile-first architecture optimized for smartphone screens ($360\text{px} - 430\text{px}$) for QR table ordering, adapting gracefully to desktop for backoffice management.
- **WebSocket Resilience (`useTenantWebSocket`)**: Exponential backoff reconnect, heartbeat ping/pong interval, and strict clean-up on unmount to prevent memory leaks.
- **Brand Tokens (*Espresso & Crema*)**:
  - Light mode: Canvas `#FAF6F1`, ink `#2C1A12`, roast accent `#C68B59`, deep text `#7C4318`.
  - Dark mode: Canvas `#140E0B`, surface `#1E1612`, crema text `#FAF6F1`.
  - All text contrast verified >= 4.5:1 WCAG 2.2 AA.
- **Zero In-Place Calculation**: All order pricing, discounts, service fees, and PB1 taxes must come from the Go backend API. Next.js must never calculate pricing independently.
- **Anti-Emoji Policy**: Zero emojis anywhere in code, metadata, or UI copy.

## Developer Workflows
```bash
pnpm dev        # Run local dev server on http://localhost:3000
pnpm build      # Production Next.js build
pnpm test:e2e   # Run Playwright end-to-end tests
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
