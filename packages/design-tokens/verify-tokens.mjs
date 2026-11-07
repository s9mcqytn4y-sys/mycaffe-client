import fs from "node:fs";
import path from "node:path";
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

const checks = [
  ["MyCaffe Primary Text on Canvas", tokens.themes.mycaffeEspressoCrema.textPrimary, tokens.themes.mycaffeEspressoCrema.bgCanvas, 4.5],
  ["MyCaffe Secondary Text on Canvas", tokens.themes.mycaffeEspressoCrema.textSecondary, tokens.themes.mycaffeEspressoCrema.bgCanvas, 4.5],
  ["AzureDevID Primary on Surface", tokens.themes.azureDevIdCorporate.brandPrimary, tokens.themes.azureDevIdCorporate.bgCanvas, 4.5],
  ["AzureDevID Cyan on Obsidian Dark", tokens.themes.azureDevIdCorporate.brandCyan, tokens.themes.azureDevIdCorporate.bgDarkObsidian, 4.5],
  ["Status Paid Badge", tokens.semanticStatus.paidOrReady.fg, tokens.semanticStatus.paidOrReady.bg, 4.5],
  ["Status Pending Badge", tokens.semanticStatus.pendingOrPreparing.fg, tokens.semanticStatus.pendingOrPreparing.bg, 4.5],
  ["Status Alert Badge", tokens.semanticStatus.failedOrAlert.fg, tokens.semanticStatus.failedOrAlert.bg, 4.5]
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

if (failed) process.exit(1);
console.log("All WCAG 2.2 AA, i18n, and antislop token checks passed.");
