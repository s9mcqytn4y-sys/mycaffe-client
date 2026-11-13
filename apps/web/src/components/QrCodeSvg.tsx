"use client";

import React, { useEffect, useState } from "react";
import { Download } from "lucide-react";
import QRCode from "qrcode";

export interface QrDownloadMeta {
  storeName?: string;
  orderLabel?: string;
  amountLabel?: string;
  filename?: string;
  pngLabel?: string;
  svgLabel?: string;
  onDownloaded?: () => void;
}

interface QrCodeSvgProps {
  value: string;
  size?: number;
  label?: string;
  downloadMeta?: QrDownloadMeta;
}

function escapeXml(raw: string): string {
  return raw.replace(/[<>&'"]/g, (ch) => {
    switch (ch) {
      case "<":
        return "&lt;";
      case ">":
        return "&gt;";
      case "&":
        return "&amp;";
      case "'":
        return "&apos;";
      default:
        return "&quot;";
    }
  });
}

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function QrCodeSvg({ value, size = 180, label = "Kode QR dapat dipindai", downloadMeta }: QrCodeSvgProps) {
  const [svgMarkup, setSvgMarkup] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    if (!value || !value.trim()) {
      setSvgMarkup("");
      return;
    }

    // M, not H: this QR is shown on a screen with nothing over it, and fewer, larger modules scan more
    // reliably from a screenshot uploaded to an e-wallet (W1).
    QRCode.toString(value, {
      type: "svg",
      margin: 2,
      width: size,
      errorCorrectionLevel: "M",
      color: {
        dark: "#17211E",
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

  const baseName = (downloadMeta?.filename || "cassy-qris").replace(/[^a-z0-9-_]+/gi, "-").toLowerCase();

  const handleDownloadSvg = () => {
    if (!svgMarkup || !downloadMeta) return;
    const inner = svgMarkup.replace(/<svg[^>]*>/, "").replace(/<\/svg>/, "");
    const store = escapeXml(downloadMeta.storeName || "Cassy POS");
    const sub = escapeXml([downloadMeta.orderLabel, downloadMeta.amountLabel].filter(Boolean).join(" - ") || "Pembayaran QRIS");
    const branded = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="360" height="460" viewBox="0 0 360 460">
  <rect width="360" height="460" rx="24" fill="#F7F5F0" stroke="#D5D2CA" stroke-width="2"/>
  <path d="M0 24a24 24 0 0 1 24-24h312a24 24 0 0 1 24 24v64H0z" fill="#1F5F52"/>
  <circle cx="34" cy="44" r="9" fill="#E9A23B"/>
  <text x="52" y="42" font-family="Plus Jakarta Sans, Arial, sans-serif" font-size="18" font-weight="800" fill="#FFFFFF">Cassy</text>
  <text x="52" y="64" font-family="Plus Jakarta Sans, Arial, sans-serif" font-size="13" font-weight="600" fill="#E7EFEA">${store}</text>
  <rect x="40" y="108" width="280" height="280" rx="16" fill="#FFFFFF" stroke="#E2DFD7"/>
  <g transform="translate(56 124) scale(${(248 / size).toFixed(4)})">${inner}</g>
  <text x="180" y="416" text-anchor="middle" font-family="Plus Jakarta Sans, Arial, sans-serif" font-size="15" font-weight="700" fill="#17211E">${sub}</text>
  <text x="180" y="438" text-anchor="middle" font-family="Plus Jakarta Sans, Arial, sans-serif" font-size="11" fill="#4B5854">QRIS Dinamis oleh Cassy x Midtrans</text>
</svg>`;
    triggerDownload(new Blob([branded], { type: "image/svg+xml;charset=utf-8" }), `${baseName}.svg`);
    downloadMeta.onDownloaded?.();
  };

  const handleDownloadPng = async () => {
    if (!value.trim() || !downloadMeta) return;
    const width = 720;
    const height = 920;
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    ctx.fillStyle = "#F7F5F0";
    ctx.fillRect(0, 0, width, height);

    ctx.fillStyle = "#1F5F52";
    ctx.fillRect(0, 0, width, 164);

    ctx.fillStyle = "#E9A23B";
    ctx.beginPath();
    ctx.arc(64, 72, 16, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = "#FFFFFF";
    ctx.font = "800 34px Plus Jakarta Sans, Arial, sans-serif";
    ctx.fillText("Cassy", 96, 82);

    ctx.fillStyle = "#E7EFEA";
    ctx.font = "600 24px Plus Jakarta Sans, Arial, sans-serif";
    ctx.fillText(downloadMeta.storeName || "Cassy POS", 96, 122);

    ctx.fillStyle = "#FFFFFF";
    ctx.fillRect(80, 204, 560, 560);

    const qrCanvas = document.createElement("canvas");
    await QRCode.toCanvas(qrCanvas, value, {
      margin: 2,
      width: 512,
      errorCorrectionLevel: "M",
      color: { dark: "#17211E", light: "#FFFFFF" }
    });
    ctx.drawImage(qrCanvas, 104, 228, 512, 512);

    ctx.fillStyle = "#17211E";
    ctx.font = "700 28px Plus Jakarta Sans, Arial, sans-serif";
    ctx.textAlign = "center";
    const sub = [downloadMeta.orderLabel, downloadMeta.amountLabel].filter(Boolean).join(" - ") || "Pembayaran QRIS";
    ctx.fillText(sub, width / 2, 824);

    ctx.fillStyle = "#4B5854";
    ctx.font = "500 20px Plus Jakarta Sans, Arial, sans-serif";
    ctx.fillText("QRIS Dinamis oleh Cassy x Midtrans", width / 2, 868);

    canvas.toBlob((blob) => {
      if (!blob) return;
      triggerDownload(blob, `${baseName}.png`);
      downloadMeta.onDownloaded?.();
    }, "image/png");
  };

  if (error) {
    return (
      <div role="alert" className="rounded-xl border border-subtle bg-muted p-3 text-xs text-ink">
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

  return (
    <div className="flex flex-col items-center gap-3">
      <div
        role="img"
        aria-label={label}
        style={{ width: size, height: size }}
        className="relative inline-block overflow-hidden rounded-xl border border-neutral-300 bg-white p-1.5 shadow-sm"
      >
        <div className="h-full w-full" dangerouslySetInnerHTML={{ __html: svgMarkup }} />
      </div>
      {downloadMeta ? (
        <div className="flex flex-wrap justify-center gap-2">
          <button
            type="button"
            onClick={() => void handleDownloadPng()}
            className="btn-secondary inline-flex min-h-[48px] items-center gap-2 px-4 text-sm font-semibold motion-safe:transition-transform motion-safe:active:scale-[0.98]"
          >
            <Download size={16} aria-hidden="true" />
            <span>{downloadMeta.pngLabel ?? "Unduh QR"}</span>
          </button>
          <button
            type="button"
            onClick={handleDownloadSvg}
            className="btn-secondary inline-flex min-h-[48px] items-center gap-2 px-4 text-sm font-semibold motion-safe:transition-transform motion-safe:active:scale-[0.98]"
          >
            <Download size={16} aria-hidden="true" />
            <span>{downloadMeta.svgLabel ?? "Simpan SVG"}</span>
          </button>
        </div>
      ) : null}
    </div>
  );
}
