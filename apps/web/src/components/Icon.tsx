// Phosphor Icons, bold weight (MIT, phosphoricons.com), copied as paths so no icon runtime ships.
const PATHS = {
  plus: "M228 128a12 12 0 0 1-12 12h-76v76a12 12 0 0 1-24 0v-76H40a12 12 0 0 1 0-24h76V40a12 12 0 0 1 24 0v76h76a12 12 0 0 1 12 12",
  minus: "M228 128a12 12 0 0 1-12 12H40a12 12 0 0 1 0-24h176a12 12 0 0 1 12 12",
  bag: "M216 36H40a20 20 0 0 0-20 20v144a20 20 0 0 0 20 20h176a20 20 0 0 0 20-20V56a20 20 0 0 0-20-20m-4 160H44V60h168ZM76 88a12 12 0 0 1 24 0a28 28 0 0 0 56 0a12 12 0 0 1 24 0a52 52 0 0 1-104 0"
} as const;

export type IconName = keyof typeof PATHS;

/** Decorative by default: the button or link around it carries the accessible name. */
export function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 256 256" aria-hidden="true" focusable="false">
      <path fill="currentColor" d={PATHS[name]} />
    </svg>
  );
}
