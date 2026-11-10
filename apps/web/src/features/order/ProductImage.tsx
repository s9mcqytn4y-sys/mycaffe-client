"use client";

import { useState } from "react";

/** Fixed-size frame so a slow or broken photo never shifts the layout; a failed load leaves the muted frame. */
export function ProductImage({ src, alt, width, height, className }: { src: string; alt: string; width: number; height: number; className: string }) {
  const [failed, setFailed] = useState(false);
  return (
    <div className={`shrink-0 overflow-hidden rounded-lg bg-muted ${className}`}>
      {failed ? null : (
        <img src={src} alt={alt} width={width} height={height} loading="lazy" onError={() => setFailed(true)} className="h-full w-full object-cover" />
      )}
    </div>
  );
}
