# Cassy POS Android Tablet: GEMINI.md

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

## Guidelines
- Follow Jetpack Compose 10-55-35 modular layout principles.
- Use raw TCP socket port 9100 with 2.500 ms timeout for ESC/POS printing.
- Strictly enforce integer Indonesian Rupiah (`int64` / `Long`).
- Enforce TalkBack semantics and minimum 48x48 dp touch targets.
- Zero emojis in code, XML, or UI strings.

<!-- antislop:start -->
## antislop
For UI, copy, people, mobile layout, or code comments work, read `antislop.md` (core) and then the skill for the task:
- UI / visual: `skills/antislop-ui/SKILL.md`
- Copy & text: `skills/antislop-copywriting/SKILL.md`
- People: `skills/antislop-human/SKILL.md`
- Mobile / responsive: `skills/antislop-layoutmobile/SKILL.md`
- Code comments: `skills/antislop-code/SKILL.md`
<!-- antislop:end -->

