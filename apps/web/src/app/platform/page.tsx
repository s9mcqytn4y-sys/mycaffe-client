"use client";

import React, { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import {
  apiFetch,
  ProvisionOnboardingResponse,
  TenantOnboardingApplication
} from "@/lib/api";
import { cleanDigits } from "@/lib/format";
import { DataStateBox, ModalDialog, StatusBadge } from "@/components/Shell";
import { QrCodeSvg } from "@/components/QrCodeSvg";

const ADMIN_KEY_SESSION_STORAGE = "mycaffe_platform_admin_key";
const RECENT_QR_TABLES_STORAGE = "mycaffe_recent_qr_tables";

export default function PlatformSuperadminPage() {
  const [adminKeyInput, setAdminKeyInput] = useState("");
  const [adminKey, setAdminKey] = useState<string>("");
  const [statusFilter, setStatusFilter] = useState<string>("");

  const [overview, setOverview] = useState<Record<string, unknown> | null>(null);
  const [applications, setApplications] = useState<TenantOnboardingApplication[]>([]);
  const [readiness, setReadiness] = useState<Record<string, unknown> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionBanner, setActionBanner] = useState<{ tone: "ok" | "err" | "info"; text: string } | null>(null);

  // Provision Modal State
  const [provisionTarget, setProvisionTarget] = useState<TenantOnboardingApplication | null>(null);
  const [initialPin, setInitialPin] = useState("123456");
  const [tableCount, setTableCount] = useState(4);
  const [provisionNotes, setProvisionNotes] = useState("Provisi awal gerai unverified untuk uji coba POS & QR.");
  const [provisionBusy, setProvisionBusy] = useState(false);
  const [provisionResult, setProvisionResult] = useState<ProvisionOnboardingResponse | null>(null);

  // Review Application Modal State
  const [reviewTarget, setReviewTarget] = useState<TenantOnboardingApplication | null>(null);
  const [reviewStatus, setReviewStatus] = useState("under_review");
  const [reviewNotes, setReviewNotes] = useState("");
  const [reviewBusy, setReviewBusy] = useState(false);

  // Tenant KYB & Clearance Controls
  const [kybStoreId, setKybStoreId] = useState("");
  const [kybStatusValue, setKybStatusValue] = useState("verified");
  const [subStatusValue, setSubStatusValue] = useState("active");
  const [kybNotes, setKybNotes] = useState("Dokumen NIB, NPWP, dan identitas pemilik telah terverifikasi.");
  const [kybBusy, setKybBusy] = useState(false);

  const [clearanceStoreId, setClearanceStoreId] = useState("");
  const [clearanceRequestId, setClearanceRequestId] = useState("");
  const [clearanceBusy, setClearanceBusy] = useState(false);

  useEffect(() => {
    try {
      const saved = sessionStorage.getItem(ADMIN_KEY_SESSION_STORAGE);
      if (saved) {
        setAdminKey(saved);
        setAdminKeyInput(saved);
      }
    } catch {
      // Ignore sessionStorage errors
    }
  }, []);

  const loadConsoleData = useCallback(
    async (activeKey: string, filterVal: string) => {
      if (!activeKey.trim()) return;
      setLoading(true);
      setError(null);
      try {
        const query = filterVal ? `?status=${encodeURIComponent(filterVal)}` : "";
        const [ovRes, onbRes, rdyRes] = await Promise.all([
          apiFetch<Record<string, unknown>>("/api/v1/platform/overview", {
            platformAdminKey: activeKey
          }),
          apiFetch<{ applications?: TenantOnboardingApplication[] }>(
            `/api/v1/platform/onboarding${query}`,
            { platformAdminKey: activeKey }
          ),
          apiFetch<Record<string, unknown>>("/api/v1/platform/midtrans-readiness", {
            platformAdminKey: activeKey
          })
        ]);
        setOverview(ovRes);
        setApplications(Array.isArray(onbRes.applications) ? onbRes.applications : []);
        setReadiness(rdyRes);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Gagal memuat data konsol Superadmin");
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    if (adminKey) {
      void loadConsoleData(adminKey, statusFilter);
    }
  }, [adminKey, statusFilter, loadConsoleData]);

  const handleConnectKey = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = adminKeyInput.trim();
    if (!trimmed) return;
    try {
      sessionStorage.setItem(ADMIN_KEY_SESSION_STORAGE, trimmed);
    } catch {
      // Ignore storage errors
    }
    setAdminKey(trimmed);
  };

  const handleClearKey = () => {
    try {
      sessionStorage.removeItem(ADMIN_KEY_SESSION_STORAGE);
    } catch {
      // Ignore storage errors
    }
    setAdminKey("");
    setAdminKeyInput("");
    setOverview(null);
    setApplications([]);
    setReadiness(null);
  };

  const handleProvisionStore = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!provisionTarget) return;
    const pinClean = cleanDigits(initialPin);
    if (pinClean.length < 6) {
      setActionBanner({ tone: "err", text: "PIN awal pemilik wajib minimal 6 digit angka." });
      return;
    }

    setProvisionBusy(true);
    setActionBanner(null);
    try {
      const res = await apiFetch<ProvisionOnboardingResponse>(
        `/api/v1/platform/onboarding/${encodeURIComponent(provisionTarget.id)}/provision`,
        {
          method: "POST",
          platformAdminKey: adminKey,
          body: {
            initial_owner_pin: pinClean,
            table_count: Math.min(50, Math.max(1, Number(tableCount) || 4)),
            review_notes: provisionNotes.trim()
          }
        }
      );
      setProvisionResult(res);
      setKybStoreId(res.store_id);

      // Save table QR links locally so /order can offer 1-click jump during testing
      try {
        const stored = (res.tables || []).map((t) => ({
          storeSlug: res.store_slug,
          tableNumber: t.table_number,
          qrToken: t.qr_token
        }));
        localStorage.setItem(RECENT_QR_TABLES_STORAGE, JSON.stringify(stored));
      } catch {
        // Ignore storage error
      }

      setActionBanner({
        tone: "ok",
        text: `Gerai ${res.store_slug} berhasil diprovisi beserta ${(res.tables || []).length} meja QR.`
      });
      await loadConsoleData(adminKey, statusFilter);
    } catch (err) {
      setActionBanner({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal memprovisi gerai"
      });
    } finally {
      setProvisionBusy(false);
    }
  };

  const handleReviewApplication = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reviewTarget) return;
    setReviewBusy(true);
    try {
      await apiFetch(`/api/v1/platform/onboarding/${encodeURIComponent(reviewTarget.id)}/review`, {
        method: "POST",
        platformAdminKey: adminKey,
        body: {
          status: reviewStatus,
          review_notes: reviewNotes.trim()
        }
      });
      setActionBanner({
        tone: "ok",
        text: `Status pengajuan ${reviewTarget.requested_slug} diperbarui ke ${reviewStatus}.`
      });
      setReviewTarget(null);
      await loadConsoleData(adminKey, statusFilter);
    } catch (err) {
      setActionBanner({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal memperbarui status pengajuan"
      });
    } finally {
      setReviewBusy(false);
    }
  };

  const handleKybReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!kybStoreId.trim()) return;
    setKybBusy(true);
    try {
      await apiFetch(`/api/v1/platform/tenants/${encodeURIComponent(kybStoreId.trim())}/kyb-review`, {
        method: "POST",
        platformAdminKey: adminKey,
        body: {
          kyb_status: kybStatusValue,
          subscription_status: subStatusValue,
          review_notes: kybNotes.trim()
        }
      });
      setActionBanner({
        tone: "ok",
        text: `Status KYB tenant ${kybStoreId.trim()} diperbarui ke ${kybStatusValue}.`
      });
      await loadConsoleData(adminKey, statusFilter);
    } catch (err) {
      setActionBanner({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal memperbarui status KYB tenant"
      });
    } finally {
      setKybBusy(false);
    }
  };

  const handleExecuteClearance = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!clearanceStoreId.trim() || !clearanceRequestId.trim()) return;
    setClearanceBusy(true);
    try {
      await apiFetch(
        `/api/v1/platform/tenants/${encodeURIComponent(
          clearanceStoreId.trim()
        )}/clearance/${encodeURIComponent(clearanceRequestId.trim())}/execute`,
        {
          method: "POST",
          platformAdminKey: adminKey,
          body: {}
        }
      );
      setActionBanner({
        tone: "ok",
        text: `Permintaan pembersihan data (UU PDP Clearance) ${clearanceRequestId.trim()} telah dieksekusi.`
      });
      await loadConsoleData(adminKey, statusFilter);
    } catch (err) {
      setActionBanner({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal mengeksekusi account clearance"
      });
    } finally {
      setClearanceBusy(false);
    }
  };

  const readinessItems: Array<Record<string, unknown>> = Array.isArray(readiness?.checks)
    ? (readiness?.checks as Array<Record<string, unknown>>)
    : Array.isArray(readiness?.items)
    ? (readiness?.items as Array<Record<string, unknown>>)
    : [];

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-4 py-8 sm:px-6">
      {/* Obsidian Control Header */}
      <section className="rounded-xl border border-subtle bg-[#07111F] p-6 text-[#F8FAFC]">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="space-y-1.5">
            <span className="inline-block rounded bg-[#0A4DA2] px-2.5 py-1 font-mono text-xs font-bold text-white">
              AZUREDEVID CONTROL PLANE
            </span>
            <h1 className="font-display text-2xl font-extrabold sm:text-3xl">
              Konsol Tata Kelola Superadmin, Provisi Gerai &amp; KYB
            </h1>
            <p className="max-w-2xl text-sm text-slate-300">
              Kunci admin hanya disimpan sementara di <code className="font-mono">sessionStorage</code> tab peramban ini dan dikirim melalui header <code className="font-mono">X-Platform-Admin-Key</code>.
            </p>
          </div>

          {adminKey ? (
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => loadConsoleData(adminKey, statusFilter)}
                className="btn-azure min-h-[44px] px-4 py-2 text-xs font-bold"
              >
                Segarkan Data Konsol
              </button>
              <button
                type="button"
                onClick={handleClearKey}
                className="min-h-[44px] rounded-lg border border-slate-600 bg-slate-900 px-4 py-2 text-xs font-semibold text-white hover:bg-slate-800"
              >
                Kunci &amp; Hapus Sesi
              </button>
            </div>
          ) : null}
        </div>

        {!adminKey ? (
          <form onSubmit={handleConnectKey} className="mt-5 flex flex-col gap-3 sm:flex-row">
            <label htmlFor="platform-admin-key" className="sr-only">
              X-Platform-Admin-Key
            </label>
            <input
              id="platform-admin-key"
              type="password"
              required
              value={adminKeyInput}
              onChange={(e) => setAdminKeyInput(e.target.value)}
              placeholder="Masukkan X-Platform-Admin-Key..."
              className="min-h-[48px] flex-1 rounded-lg border border-slate-600 bg-slate-950 px-4 py-2 font-mono text-sm text-white"
            />
            <button
              type="submit"
              className="btn-azure min-h-[48px] px-6 py-2.5 text-sm font-bold"
            >
              Buka Konsol Platform
            </button>
          </form>
        ) : null}
      </section>

      {actionBanner ? (
        <div role="status">
          <StatusBadge tone={actionBanner.tone} label={actionBanner.text} />
        </div>
      ) : null}

      {!adminKey ? (
        <DataStateBox
          state="empty"
          title="Menunggu autentikasi X-Platform-Admin-Key"
          description="Masukkan kunci admin platform pada panel di atas untuk memuat ringkasan KPI, antrean pendaftaran gerai, dan pemeriksaan kesiapan Midtrans."
        />
      ) : loading ? (
        <DataStateBox
          state="loading"
          title="Memuat telemetri platform AzureDevID"
          description="Mengambil ringkasan overview, antrean onboarding, dan checklist Midtrans..."
        />
      ) : error ? (
        <DataStateBox
          state="error"
          title="Gagal memuat data platform"
          description={error}
          onRetry={() => loadConsoleData(adminKey, statusFilter)}
        />
      ) : (
        <>
          {/* KPI Overview */}
          <section className="space-y-3">
            <h2 className="font-display text-lg font-bold text-ink">Ringkasan Metrik Platform Aktual</h2>
            {overview && Object.keys(overview).length > 0 ? (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {Object.entries(overview)
                  .filter(([, v]) => typeof v === "number" || typeof v === "string" || typeof v === "boolean")
                  .slice(0, 8)
                  .map(([key, val]) => (
                    <div key={key} className="rounded-xl border border-subtle bg-surface p-4">
                      <p className="font-mono text-xs text-subink">{key}</p>
                      <p className="mt-1 font-display text-2xl font-extrabold tabular-nums text-ink">
                        {String(val)}
                      </p>
                    </div>
                  ))}
              </div>
            ) : (
              <DataStateBox
                state="empty"
                title="Belum ada metrik numerik"
                description="Endpoint /api/v1/platform/overview belum mengembalikan entri ringkasan."
              />
            )}
          </section>

          {/* Provisioned Store Quick Jump Receipt */}
          {provisionResult ? (
            <section className="space-y-4 rounded-xl border-2 border-[#0A4DA2] bg-surface p-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <StatusBadge tone="ok" prefix="GERAI AKTIF" label={`Store ID: ${provisionResult.store_id}`} />
                  <h2 className="mt-2 font-display text-xl font-extrabold text-ink">
                    Gerai {provisionResult.store_slug} Siap Diuji (POS &amp; Meja QR)
                  </h2>
                  <p className="text-xs text-subink">
                    Email Pemilik: <code className="font-mono">{provisionResult.owner_email}</code> | Status KYB:{" "}
                    <code className="font-mono">{provisionResult.kyb_status}</code>
                  </p>
                </div>
                <Link
                  href={`/pos?slug=${encodeURIComponent(provisionResult.store_slug)}&email=${encodeURIComponent(
                    provisionResult.owner_email
                  )}`}
                  className="btn-primary min-h-[48px] px-5 py-2.5 text-sm font-bold"
                >
                  Masuk Ruang Kasir POS ({provisionResult.store_slug})
                </Link>
              </div>

              {provisionResult.tables && provisionResult.tables.length > 0 ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  {provisionResult.tables.map((tbl) => (
                    <div
                      key={tbl.id}
                      className="flex flex-col items-center gap-2 rounded-lg border border-subtle bg-muted p-3 text-center"
                    >
                      <p className="font-display text-sm font-bold text-ink">Meja {tbl.table_number}</p>
                      <QrCodeSvg
                        value={
                          typeof window !== "undefined"
                            ? `${window.location.origin}/order/${tbl.qr_token}`
                            : `/order/${tbl.qr_token}`
                        }
                        size={132}
                        label={`QR Meja ${tbl.table_number}`}
                      />
                      <Link
                        href={`/order/${encodeURIComponent(tbl.qr_token)}`}
                        className="btn-secondary mt-1 min-h-[44px] w-full px-3 py-1.5 text-xs font-semibold"
                      >
                        Buka QR Meja {tbl.table_number}
                      </Link>
                    </div>
                  ))}
                </div>
              ) : null}
            </section>
          ) : null}

          {/* Onboarding Queue */}
          <section className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="font-display text-xl font-bold text-ink">
                  Antrean Pengajuan Onboarding Tenant
                </h2>
                <p className="text-xs text-subink">
                  Provisi gerai menetapkan PIN awal pemilik dan menerbitkan meja QR default dalam satu transaksi.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <label htmlFor="onb-filter" className="text-xs font-semibold text-subink">
                  Filter Status:
                </label>
                <select
                  id="onb-filter"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="input-field w-48"
                >
                  <option value="">Semua Status</option>
                  <option value="pending_otp">pending_otp</option>
                  <option value="otp_verified">otp_verified</option>
                  <option value="under_review">under_review</option>
                  <option value="provisioned">provisioned</option>
                  <option value="rejected">rejected</option>
                </select>
              </div>
            </div>

            {applications.length === 0 ? (
              <DataStateBox
                state="empty"
                title="Tidak ada pengajuan pada filter ini"
                description="Buat pengajuan baru melalui halaman Daftar Gerai (/onboarding) atau ubah filter status."
              />
            ) : (
              <div className="space-y-3">
                {applications.map((app) => (
                  <div
                    key={app.id}
                    className="flex flex-col justify-between gap-4 rounded-xl border border-subtle bg-surface p-4 lg:flex-row lg:items-center"
                  >
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-display text-base font-bold text-ink">
                          {app.business_name}
                        </span>
                        <span className="rounded bg-muted px-2 py-0.5 font-mono text-xs text-ink">
                          {app.requested_slug}
                        </span>
                        <StatusBadge
                          tone={
                            app.status === "provisioned"
                              ? "ok"
                              : app.status === "rejected"
                              ? "err"
                              : "warn"
                          }
                          label={app.status}
                        />
                      </div>
                      <p className="text-xs text-subink">
                        Pemilik: {app.owner_name} ({app.owner_email}) | Paket: {app.selected_plan_code} ({app.billing_cycle})
                      </p>
                      <p className="font-mono text-[11px] text-subink">
                        ID: {app.id}
                        {app.provisioned_store_id ? ` | Store ID: ${app.provisioned_store_id}` : ""}
                      </p>
                    </div>

                    <div className="flex flex-wrap gap-2">
                      {app.provisioned_store_id ? (
                        <button
                          type="button"
                          onClick={() => setKybStoreId(app.provisioned_store_id || "")}
                          className="btn-secondary min-h-[44px] px-3.5 py-2 text-xs font-semibold"
                        >
                          Isi Store ID ke Form KYB
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() => {
                            setProvisionTarget(app);
                            setInitialPin("123456");
                            setTableCount(4);
                          }}
                          className="btn-azure min-h-[44px] px-4 py-2 text-xs font-bold"
                        >
                          Provisi Gerai 1-Klik
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => {
                          setReviewTarget(app);
                          setReviewStatus("under_review");
                          setReviewNotes(app.review_notes || "");
                        }}
                        className="btn-secondary min-h-[44px] px-3.5 py-2 text-xs font-semibold"
                      >
                        Tinjau Status
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* Tenant KYB Review & Clearance Execution */}
          <section className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <form
              onSubmit={handleKybReview}
              className="space-y-4 rounded-xl border border-subtle bg-surface p-5"
            >
              <h2 className="font-display text-lg font-bold text-ink">
                Verifikasi KYB &amp; Status Langganan Tenant
              </h2>
              <p className="text-xs text-subink">
                Mengubah <code className="font-mono">kyb_status</code> ke <code className="font-mono">verified</code> membuka kunci mode Production Midtrans bagi gerai terkait.
              </p>

              <div>
                <label htmlFor="kyb-store-id" className="block text-xs font-bold text-ink">
                  Store UUID Tenant
                </label>
                <input
                  id="kyb-store-id"
                  type="text"
                  required
                  value={kybStoreId}
                  onChange={(e) => setKybStoreId(e.target.value)}
                  className="input-field mt-1 font-mono"
                  placeholder="00000000-0000-0000-0000-000000000000"
                />
              </div>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <label htmlFor="kyb-status-sel" className="block text-xs font-bold text-ink">
                    Status KYB
                  </label>
                  <select
                    id="kyb-status-sel"
                    value={kybStatusValue}
                    onChange={(e) => setKybStatusValue(e.target.value)}
                    className="input-field mt-1"
                  >
                    <option value="verified">verified (Disetujui Penuh)</option>
                    <option value="pending_review">pending_review</option>
                    <option value="unverified">unverified</option>
                    <option value="rejected">rejected</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="sub-status-sel" className="block text-xs font-bold text-ink">
                    Status Langganan
                  </label>
                  <select
                    id="sub-status-sel"
                    value={subStatusValue}
                    onChange={(e) => setSubStatusValue(e.target.value)}
                    className="input-field mt-1"
                  >
                    <option value="active">active</option>
                    <option value="trialing">trialing</option>
                    <option value="past_due">past_due</option>
                    <option value="suspended">suspended</option>
                  </select>
                </div>
              </div>

              <div>
                <label htmlFor="kyb-notes" className="block text-xs font-bold text-ink">
                  Catatan Audit KYB
                </label>
                <input
                  id="kyb-notes"
                  type="text"
                  value={kybNotes}
                  onChange={(e) => setKybNotes(e.target.value)}
                  className="input-field mt-1"
                />
              </div>

              <button
                type="submit"
                disabled={kybBusy}
                className="btn-primary min-h-[44px] px-5 py-2 text-xs font-bold"
              >
                {kybBusy ? "Menyimpan..." : "Simpan Keputusan KYB"}
              </button>
            </form>

            <form
              onSubmit={handleExecuteClearance}
              className="space-y-4 rounded-xl border border-subtle bg-surface p-5"
            >
              <h2 className="font-display text-lg font-bold text-ink">
                Eksekusi Penghapusan &amp; Anonimisasi Akun (UU PDP)
              </h2>
              <p className="text-xs text-subink">
                Mengeksekusi permintaan penutupan akun tenant dengan tetap mempertahankan arsip buku besar pajak sesuai ketentuan retensi fiskal Indonesia.
              </p>

              <div>
                <label htmlFor="clr-store-id" className="block text-xs font-bold text-ink">
                  Store UUID Tenant
                </label>
                <input
                  id="clr-store-id"
                  type="text"
                  required
                  value={clearanceStoreId}
                  onChange={(e) => setClearanceStoreId(e.target.value)}
                  className="input-field mt-1 font-mono"
                  placeholder="UUID Gerai"
                />
              </div>

              <div>
                <label htmlFor="clr-req-id" className="block text-xs font-bold text-ink">
                  ID Permintaan Clearance
                </label>
                <input
                  id="clr-req-id"
                  type="text"
                  required
                  value={clearanceRequestId}
                  onChange={(e) => setClearanceRequestId(e.target.value)}
                  className="input-field mt-1 font-mono"
                  placeholder="UUID Clearance Request"
                />
              </div>

              <button
                type="submit"
                disabled={clearanceBusy}
                className="btn-secondary min-h-[44px] px-5 py-2 text-xs font-bold"
              >
                {clearanceBusy ? "Mengeksekusi..." : "Eksekusi Account Clearance"}
              </button>
            </form>
          </section>

          {/* Midtrans Production Readiness Checklist */}
          <section className="space-y-3 rounded-xl border border-subtle bg-surface p-5">
            <h2 className="font-display text-lg font-bold text-ink">
              Daftar Periksa Kesiapan Peninjauan Bisnis Midtrans Production
            </h2>
            {readinessItems.length > 0 ? (
              <div className="divide-y divide-subtle">
                {readinessItems.map((chk, idx) => {
                  const passed = Boolean(chk.passed ?? chk.ready ?? chk.status === "ok");
                  return (
                    <div key={idx} className="flex flex-wrap items-center justify-between gap-2 py-3">
                      <div>
                        <p className="text-sm font-semibold text-ink">
                          {String(chk.title ?? chk.name ?? chk.check ?? `Pemeriksaan #${idx + 1}`)}
                        </p>
                        {chk.detail || chk.description ? (
                          <p className="text-xs text-subink">{String(chk.detail ?? chk.description)}</p>
                        ) : null}
                      </div>
                      <StatusBadge
                        tone={passed ? "ok" : "warn"}
                        label={passed ? "Terpenuhi" : "Perlu Tindakan"}
                      />
                    </div>
                  );
                })}
              </div>
            ) : readiness ? (
              <pre className="overflow-x-auto rounded-lg bg-muted p-3 font-mono text-xs text-ink">
                {JSON.stringify(readiness, null, 2)}
              </pre>
            ) : (
              <DataStateBox
                state="empty"
                title="Checklist kesiapan belum tersedia"
                description="Endpoint /api/v1/platform/midtrans-readiness belum mengembalikan item pemeriksaan."
              />
            )}
          </section>
        </>
      )}

      {/* 1-Click Provisioning Modal */}
      <ModalDialog
        open={Boolean(provisionTarget)}
        title={`Provisi Gerai: ${provisionTarget?.business_name ?? ""}`}
        onClose={() => setProvisionTarget(null)}
      >
        <form onSubmit={handleProvisionStore} className="space-y-4">
          <p className="text-sm text-subink">
            Tindakan ini membuat entitas gerai berstatus <code className="font-mono">unverified</code> (siap untuk pengujian Sandbox &amp; QRIS Statis), membuat akun pemilik, dan menerbitkan token QR meja otomatis.
          </p>

          <div>
            <label htmlFor="prov-pin" className="block text-xs font-bold text-ink">
              PIN Awal Pemilik Gerai (6-8 Digit)
            </label>
            <input
              id="prov-pin"
              type="text"
              inputMode="numeric"
              maxLength={8}
              required
              value={initialPin}
              onChange={(e) => setInitialPin(cleanDigits(e.target.value).slice(0, 8))}
              className="input-field mt-1 font-mono"
            />
          </div>

          <div>
            <label htmlFor="prov-tables" className="block text-xs font-bold text-ink">
              Jumlah Meja QR Awal
            </label>
            <input
              id="prov-tables"
              type="number"
              min={1}
              max={50}
              required
              value={tableCount}
              onChange={(e) => setTableCount(Number(e.target.value))}
              className="input-field mt-1 font-mono"
            />
          </div>

          <div>
            <label htmlFor="prov-notes" className="block text-xs font-bold text-ink">
              Catatan Provisi
            </label>
            <input
              id="prov-notes"
              type="text"
              value={provisionNotes}
              onChange={(e) => setProvisionNotes(e.target.value)}
              className="input-field mt-1"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => setProvisionTarget(null)}
              className="btn-secondary min-h-[44px] px-4 py-2 text-xs font-semibold"
            >
              Batal
            </button>
            <button
              type="submit"
              disabled={provisionBusy}
              className="btn-azure min-h-[44px] px-5 py-2 text-xs font-bold"
            >
              {provisionBusy ? "Memprovisi..." : "Provisi Sekarang"}
            </button>
          </div>
        </form>
      </ModalDialog>

      {/* Review Status Modal */}
      <ModalDialog
        open={Boolean(reviewTarget)}
        title={`Tinjau Pengajuan: ${reviewTarget?.requested_slug ?? ""}`}
        onClose={() => setReviewTarget(null)}
      >
        <form onSubmit={handleReviewApplication} className="space-y-4">
          <div>
            <label htmlFor="rev-status" className="block text-xs font-bold text-ink">
              Status Peninjauan
            </label>
            <select
              id="rev-status"
              value={reviewStatus}
              onChange={(e) => setReviewStatus(e.target.value)}
              className="input-field mt-1"
            >
              <option value="under_review">under_review</option>
              <option value="approved">approved</option>
              <option value="rejected">rejected</option>
            </select>
          </div>

          <div>
            <label htmlFor="rev-notes" className="block text-xs font-bold text-ink">
              Catatan Peninjau
            </label>
            <textarea
              id="rev-notes"
              rows={3}
              value={reviewNotes}
              onChange={(e) => setReviewNotes(e.target.value)}
              className="input-field mt-1"
            />
          </div>

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setReviewTarget(null)}
              className="btn-secondary min-h-[44px] px-4 py-2 text-xs font-semibold"
            >
              Batal
            </button>
            <button
              type="submit"
              disabled={reviewBusy}
              className="btn-primary min-h-[44px] px-5 py-2 text-xs font-bold"
            >
              {reviewBusy ? "Menyimpan..." : "Simpan Status"}
            </button>
          </div>
        </form>
      </ModalDialog>
    </div>
  );
}

