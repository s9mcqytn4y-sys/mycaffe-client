"use client";

import React, { useEffect, useState } from "react";
import QRCode from "qrcode";

interface QrCodeSvgProps {
  value: string;
  size?: number;
  label?: string;
  showCenterBadge?: boolean;
}

export function QrCodeSvg({
  value,
  size = 180,
  label = "Kode QR dapat dipindai",
  showCenterBadge = true,
}: QrCodeSvgProps) {
  const [svgMarkup, setSvgMarkup] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    if (!value || !value.trim()) {
      setSvgMarkup("");
      return;
    }

    // Level H (High ~30% recovery tolerance): resistant to table stains, smudges, and camera reflections
    QRCode.toString(value, {
      type: "svg",
      margin: 2,
      width: size,
      errorCorrectionLevel: "H",
      color: {
        dark: "#140E0B",
        light: "#FFFFFF",
      },
    })
      .then((svg) => {
        if (active) {
          setSvgMarkup(svg);
          setError(null);
        }
      })
      .catch(() => {
        if (active) {
          setError("Gagal membentuk SVG QR.");
        }
      });

    return () => {
      active = false;
    };
  }, [value, size]);

  if (error) {
    return (
      <div
        role="alert"
        className="rounded-xl border border-subtle bg-muted p-3 text-xs text-ink"
      >
        {error}
      </div>
    );
  }

  if (!svgMarkup) {
    return (
      <div
        style={{ width: size, height: size }}
        className="grid place-items-center rounded-xl border border-subtle bg-white p-2 text-xs text-neutral-700"
      >
        Menyiapkan QR...
      </div>
    );
  }

  const badgeSize = Math.round(size * 0.22);

  return (
    <div
      role="img"
      aria-label={label}
      style={{ width: size, height: size }}
      className="relative inline-block overflow-hidden rounded-xl border border-neutral-300 bg-white p-1.5 shadow-sm"
    >
      <div
        className="h-full w-full"
        dangerouslySetInnerHTML={{ __html: svgMarkup }}
      />
      {showCenterBadge && (
        <div
          style={{
            width: badgeSize,
            height: badgeSize,
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
          }}
          className="pointer-events-none absolute flex items-center justify-center rounded-md border border-[#0A4DA2]/20 bg-white shadow-sm"
          aria-hidden="true"
        >
          <span className="font-mono text-[10px] font-bold tracking-tight text-[#0A4DA2]">
            AD
          </span>
        </div>
      )}
    </div>
  );
}

