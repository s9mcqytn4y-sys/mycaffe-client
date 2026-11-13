/**
 * Client-side input sanitization & validation helpers.
 * Control characters and zero-width characters are stripped before display or submission;
 * authoritative validation still lives on the server.
 */

const CONTROL_CHARS = /[\u0000-\u001F\u007F\u200B-\u200D\uFEFF]/g;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export function stripControlChars(value: string, maxLength: number): string {
  return value.replace(CONTROL_CHARS, "").slice(0, maxLength);
}

export function normalizeSubmissionText(value: string, maxLength: number): string {
  return stripControlChars(value, maxLength * 2)
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, maxLength);
}

export function isValidGuestName(value: string): boolean {
  if (!value) return true;
  if (CONTROL_CHARS.test(value)) return false;
  return !/[<>]/.test(value);
}

export function isValidEmail(value: string): boolean {
  const cleaned = normalizeSubmissionText(value, 254);
  return EMAIL_PATTERN.test(cleaned);
}
