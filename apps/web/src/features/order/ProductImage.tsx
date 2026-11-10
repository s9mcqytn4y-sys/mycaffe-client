"use client";

import { useState } from "react";

/**
 * Fixed-size frame so a slow or broken photo never shifts the layout; a failed load leaves the muted frame.
 * `eager` is for photos in the first screen, so the largest content is not held back by lazy loading.
 */
export function ProductImage({
  src,
  alt,
  width,
  height,
  className,
  eager = false
}: {
  src: string;
  alt: string;
  width: number;
  height: number;
  className: string;
  eager?: boolean;
}) {
  const [failed, setFailed] = useState(false);
  return (
    <div className={`shrink-0 overflow-hidden bg-muted ${className}`}>
      {failed ? null : (
        <img
          src={src}
          alt={alt}
          width={width}
          height={height}
          loading={eager ? "eager" : "lazy"}
          fetchPriority={eager ? "high" : "auto"}
          onError={() => setFailed(true)}
          className="h-full w-full object-cover"
        />
      )}
    </div>
  );
}
