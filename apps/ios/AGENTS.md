# Cassy iOS Hardware HAL Bridge: AGENTS.md

## Ecosystem Context & Role
This sub-workspace is the **iOS Hardware HAL Bridge** (`apps/ios` inside `C:\Projects\MyCaffeClient`), written in Objective-C.
It exposes native serial, Bluetooth, and network receipt printer interfaces for Apple devices.

### Ecosystem Topology
1. `C:\Projects\myCaffe-backend`: Multi-tenant Go 1.24 & PostgreSQL 18 backend.
2. `C:\Projects\mycaffe-wt-sc`: Loyalty stamp card engine worktree.
3. `C:\Projects\MyCaffeClient`: Client monorepo parent root.
4. `C:\Projects\MyCaffeClient\apps\android`: Cassy POS tablet (`com.azuredevid.cassy`).
5. `C:\Projects\MyCaffeClient\apps\web`: Next.js 15 PWA Guest Table Ordering.
6. `C:\Projects\MyCaffeClient\apps\ios`: This iOS HAL printer bridge.
7. `C:\Users\Acer\OneDrive\Documents\AzureDevId`: Brand identity assets and legal merchant documentation.

## Core Guidelines
- Clean Objective-C syntax with Automatic Reference Counting (ARC).
- Memory-safe buffer management for ESC/POS binary command streams.
- Zero emojis in comments or logs.

## Verification
```powershell
powershell -ExecutionPolicy Bypass -File scripts/Verify-ObjCSyntax.ps1
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
