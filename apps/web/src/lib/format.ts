export function formatIdr(amount: number | null | undefined): string {
  const safeInt = Number.isFinite(amount) ? Math.trunc(Number(amount)) : 0;
  const formatted = new Intl.NumberFormat("id-ID", {
    maximumFractionDigits: 0,
    minimumFractionDigits: 0
  }).format(safeInt);
  return `Rp ${formatted}`;
}
