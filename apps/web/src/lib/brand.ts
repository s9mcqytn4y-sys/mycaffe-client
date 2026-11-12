function luminance(hex: string): number {
  const channel = (start: number) => {
    const c = parseInt(hex.slice(start, start + 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * channel(1) + 0.7152 * channel(3) + 0.0722 * channel(5);
}

function contrast(a: number, b: number): number {
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
}

/** CSS variables for a tenant colour, or nothing when it is malformed or too weak for button text. */
export function brandVariables(hex: string | undefined): Record<string, string> {
  if (!hex || !/^#[0-9a-fA-F]{6}$/.test(hex)) return {};
  const lum = luminance(hex);
  const onLight = contrast(lum, 1);
  const onDark = contrast(lum, luminance("#17211E"));
  const best = Math.max(onLight, onDark);
  if (best < 4.5) return {};
  const foreground = onLight >= onDark ? "#FFFFFF" : "#17211E";
  // Tailwind's colour tokens are resolved at :root, so the utilities need their own override.
  return {
    "--brand-primary": hex,
    "--brand-on-primary": foreground,
    "--color-brand": hex,
    "--color-brand-fg": foreground
  };
}
