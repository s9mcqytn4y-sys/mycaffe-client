"use client";

import React, { useCallback, useEffect, useState } from "react";
import { apiFetch, StorePaymentCredentialsInput } from "@/lib/api";
import {
  detectWalletCapabilities,
  loadMidtransCore3dsSdk,
  WalletCapabilityReport
} from "@/lib/walletCapability";
import { DataStateBox, StatusBadge } from "@/components/Shell";

export function TabMidtransWallets({ token }: { token: string }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ tone: "ok" | "err" | "info"; text: string } | null>(null);

  const [kybStatus, setKybStatus] = useState<string>("unverified");
  const [environment, setEnvironment] = useState<"sandbox" | "production">("sandbox");
  const [merchantId, setMerchantId] = useState("");
  const [clientKey, setClientKey] = useState("");
  const [serverKey, setServerKey] = useState("");
  const [isEnabled, setIsEnabled] = useState(true);
  const [fallbackStaticQris, setFallbackStaticQris] = useState(true);
  const [verifyLive, setVerifyLive] = useState(false);
  const [savingCreds, setSavingCreds] = useState(false);

  // Official Midtrans Core 3DS JS SDK State
  const [sdkBusy, setSdkBusy] = useState(false);
  const [sdkStatus, setSdkStatus] = useState<{
    loaded: boolean;
    scriptUrl: string;
    hasGlobalObject: boolean;
  } | null>(null);

  // Apple Pay & Google Pay W3C Capability Report
  const [walletReport, setWalletReport] = useState<WalletCapabilityReport | null>(null);
  const [walletChecking, setWalletChecking] = useState(false);

  // Store CMS & Static QRIS Banner
  const [cmsTagline, setCmsTagline] = useState("Selamat menikmati sajian kopi segar dari meja Anda");
  const [cmsPrimaryHex, setCmsPrimaryHex] = useState("#2C1A12");
  const [cmsStaticQrisNote, setCmsStaticQrisNote] = useState("Tunjukkan bukti bayar QRIS Statis kepada kasir bila pembayaran otomatis sedang dalam pemeliharaan.");
  const [cmsSaving, setCmsSaving] = useState(false);

  const loadPaymentData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [kybRes, credRes, cmsRes] = await Promise.all([
        apiFetch<Record<string, unknown>>("/api/v1/store/kyb", { bearerToken: token }).catch(() => ({})),
        apiFetch<Record<string, unknown>>("/api/v1/store/payment-credentials", {
          bearerToken: token
        }).catch(() => ({})),
        apiFetch<Record<string, unknown>>("/api/v1/store/cms", { bearerToken: token }).catch(() => ({}))
      ]);

      const kybRaw = kybRes as Record<string, unknown>;
      const credRaw = credRes as Record<string, unknown>;
      const kybObj = (kybRaw.kyb && typeof kybRaw.kyb === "object" ? kybRaw.kyb : kybRaw) as Record<
        string,
        unknown
      >;
      setKybStatus(String(kybObj.kyb_status ?? "unverified"));

      const credObj = (
        credRaw.credentials && typeof credRaw.credentials === "object"
          ? credRaw.credentials
          : credRaw
      ) as Record<string, unknown>;
      if (credObj.environment === "production" || credObj.environment === "sandbox") {
        setEnvironment(credObj.environment);
      }
      if (typeof credObj.merchant_id === "string") setMerchantId(credObj.merchant_id);
      if (typeof credObj.client_key === "string") setClientKey(credObj.client_key);
      if (typeof credObj.is_enabled === "boolean") setIsEnabled(credObj.is_enabled);
      if (typeof credObj.fallback_static_qris_enabled === "boolean") {
        setFallbackStaticQris(credObj.fallback_static_qris_enabled);
      }

      const cmsRaw = cmsRes as Record<string, unknown>;
      const cmsObj = (cmsRaw.cms && typeof cmsRaw.cms === "object" ? cmsRaw.cms : cmsRaw) as Record<
        string,
        unknown
      >;
      if (typeof cmsObj.welcome_message === "string" && cmsObj.welcome_message) {
        setCmsTagline(cmsObj.welcome_message);
      }
      if (typeof cmsObj.primary_color_hex === "string" && cmsObj.primary_color_hex) {
        setCmsPrimaryHex(cmsObj.primary_color_hex);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Gagal memuat konfigurasi pembayaran Midtrans");
    } finally {
      setLoading(false);
    }
  }, [token]);

  const runWalletProbe = useCallback(async () => {
    setWalletChecking(true);
    try {
      const report = await detectWalletCapabilities();
      setWalletReport(report);
    } finally {
      setWalletChecking(false);
    }
  }, []);

  useEffect(() => {
    void loadPaymentData();
    void runWalletProbe();
  }, [loadPaymentData, runWalletProbe]);

  const handleSaveCredentials = async (e: React.FormEvent) => {
    e.preventDefault();
    if (environment === "production" && kybStatus !== "verified") {
      setFeedback({
        tone: "err",
        text: "Lingkungan Production Midtrans hanya dapat diaktifkan setelah status KYB gerai berstatus verified."
      });
      return;
    }

    const payload: StorePaymentCredentialsInput = {
      environment,
      merchant_id: merchantId.trim(),
      client_key: clientKey.trim(),
      server_key: serverKey.trim(),
      is_enabled: isEnabled,
      fallback_static_qris_enabled: fallbackStaticQris,
      verify_live: verifyLive
    };

    setSavingCreds(true);
    setFeedback(null);
    try {
      await apiFetch("/api/v1/store/payment-credentials", {
        method: "PUT",
        bearerToken: token,
        body: payload
      });
      setServerKey("");
      setFeedback({
        tone: "ok",
        text: `Kredensial Midtrans (${environment.toUpperCase()}) berhasil disimpan secara terenkripsi AES-256-GCM.`
      });
      await loadPaymentData();
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal menyimpan kredensial Midtrans"
      });
    } finally {
      setSavingCreds(false);
    }
  };

  const handleLoadMidtransSdk = async () => {
    const keyToUse = clientKey.trim() || "SB-Mid-client-DemoPreviewKey";
    setSdkBusy(true);
    setFeedback(null);
    try {
      const res = await loadMidtransCore3dsSdk(keyToUse, environment);
      setSdkStatus(res);
      setFeedback({
        tone: "ok",
        text: `SDK resmi Midtrans Core 3DS berhasil dimuat dari ${res.scriptUrl}.`
      });
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal memuat skrip midtrans-new-3ds.min.js"
      });
    } finally {
      setSdkBusy(false);
    }
  };

  const handleSaveCms = async (e: React.FormEvent) => {
    e.preventDefault();
    setCmsSaving(true);
    setFeedback(null);
    try {
      await apiFetch("/api/v1/store/cms", {
        method: "PUT",
        bearerToken: token,
        body: {
          welcome_message: cmsTagline.trim(),
          primary_color_hex: cmsPrimaryHex.trim(),
          static_qris_instructions: cmsStaticQrisNote.trim()
        }
      });
      setFeedback({ tone: "ok", text: "Tampilan CMS gerai & instruksi QRIS Statis diperbarui." });
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal memperbarui CMS gerai"
      });
    } finally {
      setCmsSaving(false);
    }
  };

  if (loading) {
    return (
      <DataStateBox
        state="loading"
        title="Memuat status kredensial Midtrans & dompet digital"
        description="Memeriksa status KYB gerai serta pengaturan Midtrans Core API..."
      />
    );
  }

  if (error) {
    return (
      <DataStateBox
        state="error"
        title="Gagal memuat pengaturan pembayaran"
        description={error}
        onRetry={loadPaymentData}
      />
    );
  }

  const prodLocked = kybStatus !== "verified";

  return (
    <div className="space-y-8">
      {feedback ? (
        <div role="status">
          <StatusBadge tone={feedback.tone} label={feedback.text} />
        </div>
      ) : null}

      {/* Midtrans BYOK Form */}
      <form
        onSubmit={handleSaveCredentials}
        className="space-y-5 rounded-xl border border-subtle bg-surface p-5 sm:p-6"
      >
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-subtle pb-3">
          <div>
            <h3 className="font-display text-lg font-bold text-ink">
              Kredensial Midtrans Core API Milik Gerai (BYOK AES-256-GCM)
            </h3>
            <p className="text-xs text-subink">
              Mode Sandbox diizinkan segera setelah provisi (<code className="font-mono">unverified</code>). Mode Production terbuka otomatis setelah KYB disetujui.
            </p>
          </div>
          <StatusBadge
            tone={kybStatus === "verified" ? "ok" : "warn"}
            prefix="STATUS KYB"
            label={kybStatus}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="mt-env" className="block text-xs font-bold text-ink">
              Lingkungan Midtrans
            </label>
            <select
              id="mt-env"
              value={environment}
              onChange={(e) =>
                setEnvironment(e.target.value === "production" ? "production" : "sandbox")
              }
              className="input-field mt-1"
            >
              <option value="sandbox">Sandbox (Uji Coba Langsung Diizinkan)</option>
              <option value="production" disabled={prodLocked}>
                Production {prodLocked ? "(Terkunci: Menunggu KYB Verified)" : "(Siap Transaksi Riil)"}
              </option>
            </select>
          </div>

          <div>
            <label htmlFor="mt-mid" className="block text-xs font-bold text-ink">
              Merchant ID Midtrans
            </label>
            <input
              id="mt-mid"
              type="text"
              required
              value={merchantId}
              onChange={(e) => setMerchantId(e.target.value)}
              placeholder="G123456789"
              className="input-field mt-1 font-mono"
            />
          </div>

          <div>
            <label htmlFor="mt-client" className="block text-xs font-bold text-ink">
              Client Key
            </label>
            <input
              id="mt-client"
              type="text"
              required
              value={clientKey}
              onChange={(e) => setClientKey(e.target.value)}
              placeholder="SB-Mid-client-..."
              className="input-field mt-1 font-mono"
            />
          </div>

          <div>
            <label htmlFor="mt-server" className="block text-xs font-bold text-ink">
              Server Key (Terenkripsi di Server, Tidak Ditampilkan Ulang)
            </label>
            <input
              id="mt-server"
              type="password"
              required
              value={serverKey}
              onChange={(e) => setServerKey(e.target.value)}
              placeholder="SB-Mid-server-..."
              className="input-field mt-1 font-mono"
            />
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 rounded-lg border border-subtle bg-muted p-4 sm:grid-cols-3">
          <label className="flex min-h-[44px] cursor-pointer items-center gap-2.5 text-xs font-semibold text-ink">
            <input
              type="checkbox"
              checked={isEnabled}
              onChange={(e) => setIsEnabled(e.target.checked)}
              className="h-5 w-5 accent-[#2C1A12]"
            />
            Aktifkan QRIS Dinamis Midtrans
          </label>

          <label className="flex min-h-[44px] cursor-pointer items-center gap-2.5 text-xs font-semibold text-ink">
            <input
              type="checkbox"
              checked={fallbackStaticQris}
              onChange={(e) => setFallbackStaticQris(e.target.checked)}
              className="h-5 w-5 accent-[#2C1A12]"
            />
            Siagakan Fallback QRIS Statis Kasir
          </label>

          <label className="flex min-h-[44px] cursor-pointer items-center gap-2.5 text-xs font-semibold text-ink">
            <input
              type="checkbox"
              checked={verifyLive}
              onChange={(e) => setVerifyLive(e.target.checked)}
              className="h-5 w-5 accent-[#2C1A12]"
            />
            Uji Ping Live ke Server Midtrans
          </label>
        </div>

        <div className="flex flex-wrap gap-3">
          <button
            type="submit"
            disabled={savingCreds}
            className="btn-primary min-h-[48px] px-5 py-2.5 text-xs font-bold"
          >
            {savingCreds ? "Menyimpan & Memverifikasi..." : "Simpan Kredensial BYOK Midtrans"}
          </button>

          <button
            type="button"
            disabled={sdkBusy}
            onClick={handleLoadMidtransSdk}
            className="btn-secondary min-h-[48px] px-4 py-2.5 text-xs font-semibold"
          >
            {sdkBusy ? "Memuat midtrans-new-3ds.min.js..." : "Muat & Uji Official Midtrans Core JS SDK"}
          </button>
        </div>

        {sdkStatus ? (
          <div className="rounded-lg border border-subtle bg-muted p-3 text-xs">
            <StatusBadge
              tone="ok"
              prefix="SDK AKTIF"
              label={`Global MidtransNew3ds: ${sdkStatus.hasGlobalObject ? "Tersedia" : "Skrip Termuat"}`}
            />
            <p className="mt-1.5 break-all font-mono text-[11px] text-subink">
              Sumber: {sdkStatus.scriptUrl}
            </p>
          </div>
        ) : null}
      </form>

      {/* W3C PaymentRequest Apple Pay & Google Pay Capability Diagnostic */}
      <section className="space-y-4 rounded-xl border border-subtle bg-surface p-5 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h3 className="font-display text-lg font-bold text-ink">
              Pemeriksa Kesiapan Perangkat W3C PaymentRequest (Apple Pay &amp; Google Pay)
            </h3>
            <p className="text-xs text-subink">
              Mendeteksi dukungan dompet kartu perangkat ini secara riil serta status regulasi domestik Bank Indonesia.
            </p>
          </div>
          <button
            type="button"
            disabled={walletChecking}
            onClick={runWalletProbe}
            className="btn-secondary min-h-[48px] px-4 py-2 text-xs font-semibold"
          >
            {walletChecking ? "Memeriksa Perangkat..." : "Periksa Ulang Kapabilitas"}
          </button>
        </div>

        {walletReport ? (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div className="rounded-lg border border-subtle bg-muted p-3.5">
                <p className="text-xs font-bold text-ink">QRIS Nasional (Bank Indonesia)</p>
                <div className="mt-2">
                  <StatusBadge tone="ok" label="Aktif Penuh (IDR Rupiah)" />
                </div>
              </div>

              <div className="rounded-lg border border-subtle bg-muted p-3.5">
                <p className="text-xs font-bold text-ink">Apple Pay (Safari / iOS / iPadOS)</p>
                <div className="mt-2">
                  <StatusBadge
                    tone={walletReport.applePayReady ? "ok" : "warn"}
                    label={
                      walletReport.applePayReady
                        ? "Tersedia di Perangkat Ini"
                        : "Peramban Ini Bukan Safari Apple Pay"
                    }
                  />
                </div>
              </div>

              <div className="rounded-lg border border-subtle bg-muted p-3.5">
                <p className="text-xs font-bold text-ink">Google Pay (W3C Card 3DS)</p>
                <div className="mt-2">
                  <StatusBadge
                    tone={walletReport.googlePayReady ? "ok" : "warn"}
                    label={
                      walletReport.googlePayReady
                        ? "Siap di Peramban Ini"
                        : "Belum Ada Kartu Aktif di Peramban"
                    }
                  />
                </div>
              </div>
            </div>

            <p className="rounded-lg border border-subtle bg-canvas p-3.5 text-xs leading-relaxed text-subink">
              <strong>Catatan Transparansi Regulasi:</strong> {walletReport.regulatoryNoteId} (API PaymentRequest:{" "}
              <code className="font-mono">{String(walletReport.paymentRequestSupported)}</code>, Secure Context HTTPS/Localhost:{" "}
              <code className="font-mono">{String(walletReport.secureContext)}</code>).
            </p>
          </div>
        ) : null}
      </section>

      {/* White-Label CMS & Static QRIS Instructions */}
      <form
        onSubmit={handleSaveCms}
        className="space-y-4 rounded-xl border border-subtle bg-surface p-5 sm:p-6"
      >
        <h3 className="font-display text-lg font-bold text-ink">
          Pesan Sambutan Halaman Meja QR &amp; Fallback QRIS Statis
        </h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="sm:col-span-2">
            <label htmlFor="cms-welcome" className="block text-xs font-bold text-ink">
              Sapaan Header Pelanggan QR
            </label>
            <input
              id="cms-welcome"
              type="text"
              value={cmsTagline}
              onChange={(e) => setCmsTagline(e.target.value)}
              className="input-field mt-1"
            />
          </div>
          <div>
            <label htmlFor="cms-hex" className="block text-xs font-bold text-ink">
              Warna Merek Gerai (Hex WCAG AA)
            </label>
            <input
              id="cms-hex"
              type="text"
              value={cmsPrimaryHex}
              onChange={(e) => setCmsPrimaryHex(e.target.value)}
              className="input-field mt-1 font-mono"
            />
          </div>
        </div>
        <div>
          <label htmlFor="cms-fallback" className="block text-xs font-bold text-ink">
            Instruksi Kasir Saat Mode QRIS Statis Aktif
          </label>
          <input
            id="cms-fallback"
            type="text"
            value={cmsStaticQrisNote}
            onChange={(e) => setCmsStaticQrisNote(e.target.value)}
            className="input-field mt-1"
          />
        </div>
        <button
          type="submit"
          disabled={cmsSaving}
          className="btn-secondary min-h-[48px] px-5 py-2 text-xs font-bold"
        >
          {cmsSaving ? "Menyimpan CMS..." : "Simpan Pengaturan Tampilan QR"}
        </button>
      </form>
    </div>
  );
}

