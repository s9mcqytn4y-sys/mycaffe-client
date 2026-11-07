"use client";

import React, { Suspense, useCallback, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  apiFetch,
  ApplyOnboardingResponse,
  LegalDocument,
  normalizeLegalDoc,
  normalizePlan,
  OnboardingApplicationInput,
  SubscriptionPlan,
  TenantOnboardingApplication
} from "@/lib/api";
import {
  cleanDigits,
  createIdempotencyKey,
  formatIdr,
  isValidIndoPhone,
  isValidNib,
  isValidNpwp,
  isValidTenantSlug,
  maskTaxIdentifierPreview,
  normalizeIndoPhone
} from "@/lib/format";
import { DataStateBox, StatusBadge } from "@/components/Shell";

function OnboardingWizardInner() {
  const searchParams = useSearchParams();
  const queryPlan = searchParams.get("plan") || "starter_umkm";
  const queryCycle = searchParams.get("cycle") === "annual" ? "annual" : "monthly";

  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [legalDocs, setLegalDocs] = useState<LegalDocument[]>([]);
  const [metaLoading, setMetaLoading] = useState(true);
  const [metaError, setMetaError] = useState<string | null>(null);

  // Step 1 Form State
  const [requestedSlug, setRequestedSlug] = useState("kopi-senja-jakarta");
  const [businessName, setBusinessName] = useState("Kopi Senja Jakarta");
  const [entityType, setEntityType] = useState("perorangan_umkm");
  const [cityRegion, setCityRegion] = useState("Jakarta Pusat, DKI Jakarta");
  const [ownerName, setOwnerName] = useState("");
  const [ownerEmail, setOwnerEmail] = useState("");
  const [ownerPhone, setOwnerPhone] = useState("0812");
  const [selectedPlanCode, setSelectedPlanCode] = useState(queryPlan);
  const [billingCycle, setBillingCycle] = useState<"monthly" | "annual">(queryCycle);
  const [nib, setNib] = useState("");
  const [npwp, setNpwp] = useState("");
  const [acceptTos, setAcceptTos] = useState(false);
  const [acceptPdp, setAcceptPdp] = useState(false);

  const [submitLoading, setSubmitLoading] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState(() => createIdempotencyKey("onb"));

  // Step 2 & 3 State
  const [application, setApplication] = useState<TenantOnboardingApplication | null>(null);
  const [deliveryChannel, setDeliveryChannel] = useState<string>("email");
  const [devOtpPreview, setDevOtpPreview] = useState<string | null>(null);
  const [otpCode, setOtpCode] = useState("");
  const [otpLoading, setOtpLoading] = useState(false);
  const [otpFeedback, setOtpFeedback] = useState<{ tone: "ok" | "err" | "info"; text: string } | null>(null);
  const [resendCooldown, setResendCooldown] = useState(0);

  const loadMeta = useCallback(async () => {
    setMetaLoading(true);
    setMetaError(null);
    try {
      const [planRes, legalRes] = await Promise.all([
        apiFetch<{ plans?: Array<Record<string, unknown>> }>("/api/v1/public/plans"),
        apiFetch<{ documents?: Array<Record<string, unknown>> }>("/api/v1/public/legal?locale=id-ID")
      ]);
      const pList = Array.isArray(planRes.plans) ? planRes.plans.map(normalizePlan) : [];
      const lList = Array.isArray(legalRes.documents) ? legalRes.documents.map(normalizeLegalDoc) : [];
      setPlans(pList);
      setLegalDocs(lList);
      if (pList.length > 0 && !pList.some((p) => p.code === selectedPlanCode)) {
        setSelectedPlanCode(pList[0].code);
      }
    } catch (err) {
      setMetaError(err instanceof Error ? err.message : "Gagal memuat data paket dan dokumen hukum");
    } finally {
      setMetaLoading(false);
    }
  }, [selectedPlanCode]);

  useEffect(() => {
    void loadMeta();
  }, [loadMeta]);

  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => {
      setResendCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  const tosDoc = legalDocs.find((d) => d.doc_type === "terms_of_service");
  const pdpDoc = legalDocs.find((d) => d.doc_type === "privacy_policy_pdp");

  const handleApplySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    const slugClean = requestedSlug.trim().toLowerCase();
    if (!isValidTenantSlug(slugClean)) {
      setSubmitError("Slug gerai wajib 3 hingga 50 karakter huruf kecil, angka, atau tanda hubung tengah.");
      return;
    }
    if (businessName.trim().length < 3 || ownerName.trim().length < 2) {
      setSubmitError("Nama usaha dan nama lengkap pemilik wajib diisi dengan benar.");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(ownerEmail.trim())) {
      setSubmitError("Alamat email pemilik tidak valid.");
      return;
    }
    if (!isValidIndoPhone(ownerPhone)) {
      setSubmitError("Nomor telepon Indonesia wajib diawali 08 atau +628 (10 hingga 14 digit).");
      return;
    }
    if (!isValidNib(nib)) {
      setSubmitError("NIB (Nomor Induk Berusaha) wajib tepat 13 digit angka.");
      return;
    }
    if (!isValidNpwp(npwp)) {
      setSubmitError("NPWP wajib tepat 15 atau 16 digit angka sesuai format DJP.");
      return;
    }
    if (!tosDoc?.content_sha256 || !pdpDoc?.content_sha256) {
      setSubmitError("Hash SHA-256 dokumen hukum belum tersedia. Muat ulang halaman sebelum melanjutkan.");
      return;
    }
    if (!acceptTos || !acceptPdp) {
      setSubmitError("Anda wajib menyetujui Ketentuan Layanan dan Kebijakan Privasi UU PDP.");
      return;
    }

    const payload: OnboardingApplicationInput = {
      idempotency_key: idempotencyKey,
      requested_slug: slugClean,
      business_name: businessName.trim(),
      business_entity_type: entityType,
      owner_name: ownerName.trim(),
      owner_email: ownerEmail.trim().toLowerCase(),
      owner_phone: normalizeIndoPhone(ownerPhone),
      city_region: cityRegion.trim(),
      selected_plan_code: selectedPlanCode,
      billing_cycle: billingCycle,
      nib: cleanDigits(nib),
      npwp: cleanDigits(npwp),
      accepted_tos_sha256: tosDoc.content_sha256,
      accepted_pdp_sha256: pdpDoc.content_sha256
    };

    setSubmitLoading(true);
    try {
      const res = await apiFetch<ApplyOnboardingResponse>("/api/v1/public/onboarding/apply", {
        method: "POST",
        body: payload
      });
      setApplication(res.application);
      setDeliveryChannel(res.otp_delivery_channel || "email");
      setDevOtpPreview(res.dev_otp_preview || null);
      setResendCooldown(60);
      setStep(2);
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "Pengajuan pendaftaran gagal diproses");
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!application) return;
    const cleanOtp = cleanDigits(otpCode);
    if (cleanOtp.length !== 6) {
      setOtpFeedback({ tone: "err", text: "Kode OTP wajib tepat 6 digit angka." });
      return;
    }

    setOtpLoading(true);
    setOtpFeedback(null);
    try {
      const res = await apiFetch<{ application: TenantOnboardingApplication }>(
        `/api/v1/public/onboarding/${encodeURIComponent(application.id)}/verify-otp`,
        {
          method: "POST",
          body: { otp_code: cleanOtp }
        }
      );
      setApplication(res.application);
      setStep(3);
    } catch (err) {
      setOtpFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Kode OTP tidak valid atau telah kedaluwarsa"
      });
    } finally {
      setOtpLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (!application || resendCooldown > 0) return;
    setOtpLoading(true);
    setOtpFeedback(null);
    try {
      const res = await apiFetch<{ status: string; otp_expires_at?: string; dev_otp_preview?: string }>(
        `/api/v1/public/onboarding/${encodeURIComponent(application.id)}/resend-otp`,
        { method: "POST", body: {} }
      );
      if (res.dev_otp_preview) {
        setDevOtpPreview(res.dev_otp_preview);
      }
      setResendCooldown(60);
      setOtpFeedback({
        tone: "ok",
        text: `Kode OTP baru telah dikirim ulang (${res.status}).`
      });
    } catch (err) {
      setOtpFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal mengirim ulang OTP"
      });
    } finally {
      setOtpLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-8 px-4 py-8 sm:px-6">
      <div className="space-y-2">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <StatusBadge tone="info" prefix={`LANGKAH ${step}/3`} label="Pendaftaran Mandiri Gerai Baru" />
          <Link href="/legal" className="text-xs font-semibold underline text-accent-ink">
            Lihat Naskah Hukum &amp; SHA-256
          </Link>
        </div>
        <h1 className="font-display text-2xl font-extrabold text-ink sm:text-3xl">
          Pendaftaran Tenant MyCaffe &amp; Verifikasi KYB Awal
        </h1>
        <p className="text-sm leading-relaxed text-subink">
          Lengkapi identitas usaha, nomor NIB dan NPWP untuk kepatuhan pajak Indonesia, lalu verifikasi kepemilikan email menggunakan kode OTP 6 digit.
        </p>
      </div>

      {/* Stepper Indicator */}
      <ol className="grid grid-cols-1 gap-2.5 sm:grid-cols-3" aria-label="Tahapan pendaftaran">
        {[
          { num: 1, label: "1. Profil Usaha & Paket" },
          { num: 2, label: "2. Verifikasi OTP Email" },
          { num: 3, label: "3. Bukti Pengajuan Aktif" }
        ].map((item) => {
          const active = step === item.num;
          const done = step > item.num;
          return (
            <li
              key={item.num}
              className={`rounded-lg border px-4 py-3 text-xs font-bold ${
                active
                  ? "border-brand bg-brand text-brand-fg"
                  : done
                  ? "badge-ok border-transparent"
                  : "border-subtle bg-surface text-subink"
              }`}
            >
              {done ? `[SELESAI] ${item.label}` : item.label}
            </li>
          );
        })}
      </ol>

      {metaLoading ? (
        <DataStateBox
          state="loading"
          title="Menyiapkan formulir pendaftaran"
          description="Memuat paket langganan dan hash SHA-256 dokumen hukum dari server..."
        />
      ) : metaError ? (
        <DataStateBox
          state="error"
          title="Data prasyarat pendaftaran belum tersedia"
          description={metaError}
          onRetry={loadMeta}
        />
      ) : step === 1 ? (
        <form
          onSubmit={handleApplySubmit}
          className="space-y-6 rounded-xl border border-subtle bg-surface p-5 sm:p-7"
        >
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="onb-slug" className="block text-xs font-bold text-ink">
                Slug URL Gerai (3-50 huruf kecil/angka/strip)
              </label>
              <input
                id="onb-slug"
                type="text"
                required
                value={requestedSlug}
                onChange={(e) => setRequestedSlug(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ""))}
                className="input-field mt-1 font-mono"
                placeholder="kopi-senja-jakarta"
              />
              <p className="mt-1 text-xs text-subink">
                Pratinjau URL: <code className="font-mono">/pos?slug={requestedSlug || "..."}</code>
              </p>
            </div>

            <div>
              <label htmlFor="onb-biz" className="block text-xs font-bold text-ink">
                Nama Resmi Gerai / Merek Kafe
              </label>
              <input
                id="onb-biz"
                type="text"
                required
                value={businessName}
                onChange={(e) => setBusinessName(e.target.value)}
                className="input-field mt-1"
                placeholder="Kopi Senja Jakarta"
              />
            </div>

            <div>
              <label htmlFor="onb-entity" className="block text-xs font-bold text-ink">
                Bentuk Badan Usaha
              </label>
              <select
                id="onb-entity"
                value={entityType}
                onChange={(e) => setEntityType(e.target.value)}
                className="input-field mt-1"
              >
                <option value="perorangan_umkm">Usaha Perorangan / UMKM</option>
                <option value="pt_perseroan">PT (Perseroan Terbatas)</option>
                <option value="pt_perorangan">PT Perorangan</option>
                <option value="cv_komanditer">CV (Persekutuan Komanditer)</option>
                <option value="koperasi">Koperasi</option>
              </select>
            </div>

            <div>
              <label htmlFor="onb-city" className="block text-xs font-bold text-ink">
                Kota / Provinsi Operasional
              </label>
              <input
                id="onb-city"
                type="text"
                required
                value={cityRegion}
                onChange={(e) => setCityRegion(e.target.value)}
                className="input-field mt-1"
                placeholder="Jakarta Selatan, DKI Jakarta"
              />
            </div>

            <div>
              <label htmlFor="onb-owner" className="block text-xs font-bold text-ink">
                Nama Lengkap Pemilik / Penanggung Jawab
              </label>
              <input
                id="onb-owner"
                type="text"
                required
                value={ownerName}
                onChange={(e) => setOwnerName(e.target.value)}
                className="input-field mt-1"
                placeholder="Budi Santoso"
              />
            </div>

            <div>
              <label htmlFor="onb-email" className="block text-xs font-bold text-ink">
                Email Pemilik (Tujuan Kode OTP)
              </label>
              <input
                id="onb-email"
                type="email"
                required
                value={ownerEmail}
                onChange={(e) => setOwnerEmail(e.target.value)}
                className="input-field mt-1"
                placeholder="pemilik@kopisenja.id"
              />
            </div>

            <div>
              <label htmlFor="onb-phone" className="block text-xs font-bold text-ink">
                Nomor WhatsApp / Seluler Aktif
              </label>
              <input
                id="onb-phone"
                type="tel"
                required
                value={ownerPhone}
                onChange={(e) => setOwnerPhone(e.target.value)}
                className="input-field mt-1 font-mono"
                placeholder="081234567890"
              />
              <p className="mt-1 text-xs text-subink">
                Normalisasi E.164: <code className="font-mono">{normalizeIndoPhone(ownerPhone)}</code>
              </p>
            </div>

            <div>
              <label htmlFor="onb-plan" className="block text-xs font-bold text-ink">
                Paket Langganan &amp; Siklus Tagihan
              </label>
              <div className="mt-1 flex gap-2">
                <select
                  id="onb-plan"
                  value={selectedPlanCode}
                  onChange={(e) => setSelectedPlanCode(e.target.value)}
                  className="input-field flex-1"
                >
                  {plans.map((p) => (
                    <option key={p.code} value={p.code}>
                      {p.name} ({formatIdr(billingCycle === "annual" ? p.annual_price_idr : p.monthly_price_idr)})
                    </option>
                  ))}
                </select>
                <select
                  aria-label="Siklus tagihan"
                  value={billingCycle}
                  onChange={(e) => setBillingCycle(e.target.value === "annual" ? "annual" : "monthly")}
                  className="input-field w-36"
                >
                  <option value="monthly">Bulanan</option>
                  <option value="annual">Tahunan</option>
                </select>
              </div>
            </div>

            <div>
              <label htmlFor="onb-nib" className="block text-xs font-bold text-ink">
                NIB OSS (13 Digit Angka)
              </label>
              <input
                id="onb-nib"
                type="text"
                inputMode="numeric"
                maxLength={13}
                required
                value={nib}
                onChange={(e) => setNib(cleanDigits(e.target.value).slice(0, 13))}
                className="input-field mt-1 font-mono"
                placeholder="1234567890123"
              />
              <p className="mt-1 text-xs text-subink">
                Pratinjau tersamar: <code className="font-mono">{nib ? maskTaxIdentifierPreview(nib) : "belum diisi"}</code> ({cleanDigits(nib).length}/13)
              </p>
            </div>

            <div>
              <label htmlFor="onb-npwp" className="block text-xs font-bold text-ink">
                NPWP Badan / Pribadi (15 atau 16 Digit)
              </label>
              <input
                id="onb-npwp"
                type="text"
                inputMode="numeric"
                maxLength={16}
                required
                value={npwp}
                onChange={(e) => setNpwp(cleanDigits(e.target.value).slice(0, 16))}
                className="input-field mt-1 font-mono"
                placeholder="0123456789012345"
              />
              <p className="mt-1 text-xs text-subink">
                Pratinjau tersamar: <code className="font-mono">{npwp ? maskTaxIdentifierPreview(npwp) : "belum diisi"}</code> ({cleanDigits(npwp).length} digit)
              </p>
            </div>
          </div>

          {/* Explicit ToS & UU PDP SHA-256 Checkboxes */}
          <div className="space-y-3 rounded-lg border border-subtle bg-muted p-4">
            <p className="text-xs font-bold text-ink">
              Persetujuan Kontrak Elektronik &amp; Pelindungan Data Pribadi (UU No. 27 Tahun 2022)
            </p>

            <label className="flex min-h-[44px] cursor-pointer items-start gap-3 text-sm text-ink">
              <input
                type="checkbox"
                checked={acceptTos}
                onChange={(e) => setAcceptTos(e.target.checked)}
                className="mt-1 h-5 w-5 shrink-0 accent-[#2C1A12]"
              />
              <span>
                Saya telah membaca dan menyetujui{" "}
                <Link href="/legal?doc=terms_of_service" target="_blank" className="font-semibold underline">
                  Syarat &amp; Ketentuan Layanan MyCaffe
                </Link>{" "}
                <span className="block font-mono text-[11px] text-subink">
                  SHA-256: {tosDoc?.content_sha256 ? `${tosDoc.content_sha256.slice(0, 28)}...` : "tidak ditemukan"}
                </span>
              </span>
            </label>

            <label className="flex min-h-[44px] cursor-pointer items-start gap-3 text-sm text-ink">
              <input
                type="checkbox"
                checked={acceptPdp}
                onChange={(e) => setAcceptPdp(e.target.checked)}
                className="mt-1 h-5 w-5 shrink-0 accent-[#2C1A12]"
              />
              <span>
                Saya menyetujui pemrosesan data pendaftaran sesuai{" "}
                <Link href="/legal?doc=privacy_policy_pdp" target="_blank" className="font-semibold underline">
                  Kebijakan Privasi UU PDP
                </Link>{" "}
                <span className="block font-mono text-[11px] text-subink">
                  SHA-256: {pdpDoc?.content_sha256 ? `${pdpDoc.content_sha256.slice(0, 28)}...` : "tidak ditemukan"}
                </span>
              </span>
            </label>
          </div>

          {submitError ? (
            <div role="alert">
              <StatusBadge tone="err" label={submitError} />
            </div>
          ) : null}

          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIdempotencyKey(createIdempotencyKey("onb"))}
              className="btn-secondary min-h-[44px] px-3.5 py-2 text-xs font-semibold"
            >
              Segarkan Kunci Idempotensi
            </button>
            <button
              type="submit"
              disabled={submitLoading}
              className="btn-primary min-h-[48px] px-6 py-2.5 text-sm font-bold"
            >
              {submitLoading ? "Mengirim Pengajuan & OTP..." : "Ajukan Pendaftaran & Kirim OTP"}
            </button>
          </div>
        </form>
      ) : step === 2 && application ? (
        <form
          onSubmit={handleVerifyOtp}
          className="space-y-5 rounded-xl border border-subtle bg-surface p-5 sm:p-7"
        >
          <div className="space-y-1">
            <StatusBadge tone="info" prefix="OTP DIKIRIM" label={`Kanal pengiriman: ${deliveryChannel}`} />
            <h2 className="pt-2 font-display text-xl font-bold text-ink">
              Masukkan Kode OTP 6 Digit untuk {application.owner_email}
            </h2>
            <p className="text-sm text-subink">
              ID Pengajuan: <code className="font-mono">{application.id}</code> | Slug:{" "}
              <code className="font-mono">{application.requested_slug}</code>
            </p>
          </div>

          {devOtpPreview ? (
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-subtle bg-muted p-3.5">
              <div className="text-xs text-ink">
                <span className="font-bold">Pratinjau OTP Lingkungan Demo/Sandbox:</span>{" "}
                <code className="rounded bg-surface px-2 py-0.5 font-mono text-sm font-extrabold">
                  {devOtpPreview}
                </code>
              </div>
              <button
                type="button"
                onClick={() => setOtpCode(devOtpPreview)}
                className="btn-secondary min-h-[44px] px-3 py-1.5 text-xs font-semibold"
              >
                Gunakan Kode Ini
              </button>
            </div>
          ) : null}

          <div>
            <label htmlFor="otp-input" className="block text-xs font-bold text-ink">
              Kode Verifikasi OTP (6 Digit)
            </label>
            <input
              id="otp-input"
              type="text"
              inputMode="numeric"
              maxLength={6}
              required
              value={otpCode}
              onChange={(e) => setOtpCode(cleanDigits(e.target.value).slice(0, 6))}
              className="input-field mt-1 max-w-xs font-mono text-xl font-bold tracking-[0.3em]"
              placeholder="123456"
            />
          </div>

          {otpFeedback ? (
            <div role="status">
              <StatusBadge tone={otpFeedback.tone} label={otpFeedback.text} />
            </div>
          ) : null}

          <div className="flex flex-wrap items-center gap-3 pt-2">
            <button
              type="submit"
              disabled={otpLoading}
              className="btn-primary min-h-[48px] px-6 py-2.5 text-sm font-bold"
            >
              {otpLoading ? "Memverifikasi OTP..." : "Verifikasi Kode OTP"}
            </button>

            <button
              type="button"
              disabled={otpLoading || resendCooldown > 0}
              onClick={handleResendOtp}
              className="btn-secondary min-h-[48px] px-4 py-2.5 text-xs font-semibold"
            >
              {resendCooldown > 0
                ? `Kirim Ulang OTP (${resendCooldown} dtk)`
                : "Kirim Ulang Kode OTP"}
            </button>
          </div>
        </form>
      ) : application ? (
        <section className="space-y-5 rounded-xl border border-subtle bg-surface p-5 sm:p-7">
          <StatusBadge tone="ok" prefix="TERVERIFIKASI" label={`Status Pengajuan: ${application.status}`} />
          <h2 className="font-display text-2xl font-extrabold text-ink">
            Email Terverifikasi: Pengajuan Gerai {application.business_name} Siap Diprovisi
          </h2>
          <p className="text-sm leading-relaxed text-subink">
            Pengajuan Anda telah tercatat di antrean peninjauan AzureDevID. Untuk pengujian langsung end-to-end, buka Konsol Superadmin untuk memprovisi gerai beserta meja QR dalam satu klik.
          </p>

          <dl className="grid grid-cols-1 gap-3 rounded-lg border border-subtle bg-muted p-4 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-xs text-subink">ID Pengajuan</dt>
              <dd className="font-mono text-xs font-semibold text-ink">{application.id}</dd>
            </div>
            <div>
              <dt className="text-xs text-subink">Slug Gerai</dt>
              <dd className="font-mono text-xs font-semibold text-ink">{application.requested_slug}</dd>
            </div>
            <div>
              <dt className="text-xs text-subink">NIB / NPWP Tersamar</dt>
              <dd className="font-mono text-xs text-ink">
                {application.nib_masked || maskTaxIdentifierPreview(nib)} /{" "}
                {application.npwp_masked || maskTaxIdentifierPreview(npwp)}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-subink">Paket &amp; Siklus</dt>
              <dd className="font-semibold text-ink">
                {application.selected_plan_code} ({application.billing_cycle})
              </dd>
            </div>
          </dl>

          <div className="flex flex-wrap gap-3">
            <Link
              href="/platform"
              className="btn-azure min-h-[48px] px-5 py-2.5 text-sm font-bold"
            >
              Buka Konsol Superadmin (Provisi Gerai)
            </Link>
            <Link
              href={`/pos?slug=${encodeURIComponent(application.requested_slug)}`}
              className="btn-secondary min-h-[48px] px-5 py-2.5 text-sm font-semibold"
            >
              Ke Halaman Login Kasir POS
            </Link>
          </div>
        </section>
      ) : null}
    </div>
  );
}

export default function OnboardingPage() {
  return (
    <Suspense
      fallback={
        <div className="mx-auto max-w-4xl px-4 py-8">
          <DataStateBox
            state="loading"
            title="Memuat halaman pendaftaran"
            description="Menyiapkan parameter pendaftaran gerai..."
          />
        </div>
      }
    >
      <OnboardingWizardInner />
    </Suspense>
  );
}

