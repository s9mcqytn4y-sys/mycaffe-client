# Cassy iOS printer bridge (`apps/ios`)

Read the root `AGENTS.md` first.

Objective-C hardware bridge for serial, Bluetooth and network receipt printers. No UI. Class names keep the `MyCaffe` prefix as technical identifiers until the bridge ships as an SDK.

## Rules
- ARC only; bounds-checked buffers for ESC/POS byte streams.
- No emoji in comments or logs.

## Check
```powershell
powershell -ExecutionPolicy Bypass -File scripts/Verify-ObjCSyntax.ps1
```
