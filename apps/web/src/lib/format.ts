export function formatIdr(amount: number | null | undefined): string {
  const safeInt = Number.isFinite(amount) ? Math.trunc(Number(amount)) : 0;
  const formatted = new Intl.NumberFormat("id-ID", {
    maximumFractionDigits: 0,
    minimumFractionDigits: 0
  }).format(safeInt);
  return `Rp ${formatted}`;
}

export function normalizeIndoPhone(raw: string): string {
  const digits = raw.replace(/[^\d+]/g, "").trim();
  if (digits.startsWith("08")) {
    return `+62${digits.slice(1)}`;
  }
  if (digits.startsWith("628")) {
    return `+${digits}`;
  }
  return digits;
}

export function isValidIndoPhone(raw: string): boolean {
  const normalized = normalizeIndoPhone(raw);
  return /^\+628[1-9][0-9]{7,11}$/.test(normalized);
}

export function isValidTenantSlug(slug: string): boolean {
  return /^[a-z0-9-]{3,50}$/.test(slug) && !slug.startsWith("-") && !slug.endsWith("-");
}

export function cleanDigits(raw: string): string {
  return raw.replace(/\D/g, "");
}

export function isValidNib(raw: string): boolean {
  return /^\d{13}$/.test(cleanDigits(raw));
}

export function isValidNpwp(raw: string): boolean {
  return /^\d{15,16}$/.test(cleanDigits(raw));
}

export function maskTaxIdentifierPreview(raw: string): string {
  const d = cleanDigits(raw);
  if (d.length < 7) return d;
  return `${d.slice(0, 4)}${"*".repeat(Math.max(3, d.length - 8))}${d.slice(-4)}`;
}

export function createIdempotencyKey(prefix: string): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return `${prefix}-${crypto.randomUUID()}`;
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

