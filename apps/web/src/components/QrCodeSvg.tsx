"use client";

import React, { useEffect, useState } from "react";
import QRCode from "qrcode";

interface QrCodeSvgProps {
  value: string;
  size?: number;
  label?: string;
}

export function QrCodeSvg({ value, size = 180, label = "Kode QR dapat dipindai" }: QrCodeSvgProps) {
  const [svgMarkup, setSvgMarkup] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    if (!value || !value.trim()) {
      setSvgMarkup("");
      return;
    }

    QRCode.toString(value, {
      type: "svg",
      margin: 2,
      width: size,
      errorCorrectionLevel: "M",
      color: {
        dark: "#140E0B",
        light: "#FFFFFF"
      }
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
        className="rounded-lg border border-subtle bg-muted p-3 text-xs text-ink"
      >
        {error}
      </div>
    );
  }

  if (!svgMarkup) {
    return (
      <div
        style={{ width: size, height: size }}
        className="grid place-items-center rounded-lg border border-subtle bg-white p-2 text-xs text-neutral-700"
      >
        Menyiapkan QR...
      </div>
    );
  }

  return (
    <div
      role="img"
      aria-label={label}
      style={{ width: size, height: size }}
      className="inline-block overflow-hidden rounded-lg border border-neutral-300 bg-white p-1.5 shadow-sm"
      dangerouslySetInnerHTML={{ __html: svgMarkup }}
    />
  );
}

