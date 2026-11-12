import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tokens = JSON.parse(fs.readFileSync(path.join(__dirname, "tokens.json"), "utf8"));
const i18n = JSON.parse(fs.readFileSync(path.join(__dirname, "i18n.json"), "utf8"));

function srgbToLinear(hexChannel) {
  const c = hexChannel / 255;
  return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
}

function luminance(hex) {
  const clean = hex.replace("#", "");
  const r = parseInt(clean.slice(0, 2), 16);
  const g = parseInt(clean.slice(2, 4), 16);
  const b = parseInt(clean.slice(4, 6), 16);
  return 0.2126 * srgbToLinear(r) + 0.7152 * srgbToLinear(g) + 0.0722 * srgbToLinear(b);
}

function contrastRatio(fg, bg) {
  const l1 = luminance(fg);
  const l2 = luminance(bg);
  const bright = Math.max(l1, l2);
  const dark = Math.min(l1, l2);
  return (bright + 0.05) / (dark + 0.05);
}

const { cassyLight: L, cassyDark: D, azureDevIdCorporate: A } = tokens.themes;
// Every text and component pair the apps actually draw (PRD v4 B2): 4.5:1 for text, 3:1 for borders and focus.
const pairs = [
  ["primary text on canvas", "textPrimary", "bgCanvas", 4.5],
  ["primary text on surface", "textPrimary", "bgSurface", 4.5],
  ["secondary text on canvas", "textSecondary", "bgCanvas", 4.5],
  ["secondary text on surface", "textSecondary", "bgSurface", 4.5],
  ["secondary text on muted", "textSecondary", "bgMuted", 4.5],
  ["text on pandan", "brandOnPrimary", "brandPrimary", 4.5],
  ["pandan text on surface", "brandPrimary", "bgSurface", 4.5],
  ["text on kunyit", "brandOnAccent", "brandAccent", 4.5],
  ["kunyit text on surface", "brandAccentText", "bgSurface", 4.5],
  ["text on guest primary", "guestOnPrimary", "guestPrimary", 4.5],
  ["strong border on surface", "borderStrong", "bgSurface", 3],
  ["strong border on canvas", "borderStrong", "bgCanvas", 3],
  ["focus ring on canvas", "focusRing", "bgCanvas", 3],
  ["focus ring on surface", "focusRing", "bgSurface", 3],
  ["paid badge", "okFg", "okBg", 4.5],
  ["pending badge", "warnFg", "warnBg", 4.5],
  ["failed badge", "errFg", "errBg", 4.5],
  ["info badge", "infoFg", "infoBg", 4.5],
  ["paid text on surface", "okFg", "bgSurface", 4.5],
  ["failed text on surface", "errFg", "bgSurface", 4.5]
];
const checks = [
  ...pairs.map(([label, fg, bg, min]) => [`Light: ${label}`, L[fg], L[bg], min]),
  ...pairs.map(([label, fg, bg, min]) => [`Dark: ${label}`, D[fg], D[bg], min]),
  ["AzureDevID Primary on Surface", A.brandPrimary, A.bgCanvas, 4.5],
  ["AzureDevID Cyan on Obsidian Dark", A.brandCyan, A.bgDarkObsidian, 4.5]
];

let failed = false;
for (const [label, fg, bg, minRatio] of checks) {
  const ratio = contrastRatio(fg, bg);
  if (ratio < minRatio) {
    console.error(`[FAIL] WCAG AA contrast ${label}: ${ratio.toFixed(2)}:1 (< ${minRatio}:1)`);
    failed = true;
  } else {
    console.log(`[PASS] WCAG AA contrast ${label}: ${ratio.toFixed(2)}:1`);
  }
}

const idKeys = Object.keys(i18n.messages["id-ID"]).sort();
const enKeys = Object.keys(i18n.messages["en-US"]).sort();
if (JSON.stringify(idKeys) !== JSON.stringify(enKeys)) {
  console.error("[FAIL] Locale key mismatch between id-ID and en-US");
  failed = true;
} else {
  console.log(`[PASS] i18n parity verified (${idKeys.length} keys across id-ID and en-US)`);
}

for (const [locale, dict] of Object.entries(i18n.messages)) {
  for (const [k, v] of Object.entries(dict)) {
    if (v.includes("\u2014")) {
      console.error(`[FAIL] Em-dash forbidden by antislop found in ${locale}.${k}`);
      failed = true;
    }
  }
}

if (tokens.appIdentifier !== "com.azuredevid.cassy") {
  console.error(`[FAIL] Unexpected appIdentifier: ${tokens.appIdentifier}`);
  failed = true;
} else {
  console.log(`[PASS] Official legal appIdentifier verified: ${tokens.appIdentifier}`);
}

// Generated platform files must match tokens.json.
const build = spawnSync(process.execPath, [path.join(__dirname, "build-tokens.mjs"), "--check"], { stdio: "inherit" });
if (build.status !== 0) failed = true;
else console.log("[PASS] Generated platform token files are in sync");

if (failed) process.exit(1);
console.log("All WCAG 2.2 AA (Light & Dark), i18n, and antislop token checks passed.");
