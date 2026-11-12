/** Cassy mark from public/brand/cassy. The dark variant swaps in on data-theme="dark". */
export function CassyMonogram({ size = 40, className = "" }: { size?: number; className?: string }) {
  return <img src="/brand/cassy/CassyMonogram.svg" alt="" width={size} height={size} className={className} />;
}

export function CassyWordmark({ height = 16 }: { height?: number }) {
  const width = Math.round((height * 2669) / 911);
  return (
    <>
      <img src="/brand/cassy/CassyWordmark.svg" alt="Cassy" width={width} height={height} className="dark:hidden" />
      <img src="/brand/cassy/CassyWordmarkOnDark.svg" alt="Cassy" width={width} height={height} className="hidden dark:block" />
    </>
  );
}
