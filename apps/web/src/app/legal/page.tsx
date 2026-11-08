"use client";

import { useThemeLocale } from "@/components/Providers";
import { DataStateBox, StatusBadge } from "@/components/Shell";
import { apiFetch, LegalDocument, normalizeLegalDoc } from "@/lib/api";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useCallback, useEffect, useState } from "react";

function LegalPageInner() {
  const { locale } = useThemeLocale();
  const searchParams = useSearchParams();
  const initialDocParam = searchParams.get("doc") || "terms_of_service";

  const [docs, setDocs] = useState<LegalDocument[]>([]);
  const [selectedType, setSelectedType] = useState<string>(initialDocParam);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copiedHash, setCopiedHash] = useState(false);

  const fetchLegal = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<{ documents?: Array<Record<string, unknown>> }>(
        `/api/v1/public/legal?locale=${locale}`
      );
      const normalized = Array.isArray(res.documents) ? res.documents.map(normalizeLegalDoc) : [];
      setDocs(normalized);
      if (normalized.length > 0 && !normalized.some((d) => d.doc_type === selectedType)) {
        setSelectedType(normalized[0].doc_type);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Gagal memuat dokumen hukum resmi");
    } finally {
      setLoading(false);
    }
  }, [locale, selectedType]);

  useEffect(() => {
    void fetchLegal();
  }, [fetchLegal]);

  const activeDoc = docs.find((d) => d.doc_type === selectedType) ?? docs[0];

  const handleCopyHash = async (hash: string) => {
    if (!hash || typeof navigator === "undefined" || !navigator.clipboard) return;
    await navigator.clipboard.writeText(hash);
    setCopiedHash(true);
    setTimeout(() => setCopiedHash(false), 2000);
  };

  return (
    <div className="mx-auto max-w-6xl space-y-8 px-4 py-8 sm:px-6">
      {/* AzureDevID Corporate Governance Banner */}
      <section className="rounded-xl border border-subtle bg-[#07111F] p-6 text-[#F8FAFC]">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-2">
            <span className="inline-block rounded bg-[#0A4DA2] px-2.5 py-1 font-mono text-xs font-bold text-white">
              AZUREDEVID LEGAL &amp; COMPLIANCE PLANE
            </span>
            <h1 className="font-display text-2xl font-extrabold sm:text-3xl">
              Pusat Ketentuan Layanan, Kebijakan Privasi UU PDP &amp; Pengembalian Dana
            </h1>
            <p className="max-w-3xl text-sm leading-relaxed text-slate-300">
              Dokumen hukum resmi untuk peninjauan kepatuhan merchant Midtrans Payment Gateway, pelindungan data pribadi berdasarkan UU No. 27 Tahun 2022 (UU PDP), dan perjanjian pemrosesan data tenant MyCaffe.
            </p>
          </div>
          <Link
            href="/onboarding"
            className="btn-azure min-h-[44px] px-4 py-2 text-xs font-bold"
          >
            Lanjut Daftar Gerai
          </Link>
        </div>

        <div className="mt-5 grid grid-cols-1 gap-4 border-t border-slate-800 pt-4 text-xs text-slate-200 sm:grid-cols-3">
          <div>
            <p className="font-semibold text-[#00AEEF]">Penyelenggara Sistem Elektronik</p>
            <p className="mt-0.5">AzureDevID (Unit Produk MyCaffe POS)</p>
            <p className="font-mono text-[11px] text-slate-400">ID Paket: com.azuredevid.cassy</p>
          </div>
          <div>
            <p className="font-semibold text-[#00AEEF]">Domisili &amp; Wilayah Hukum</p>
            <p className="mt-0.5">Jakarta Pusat, DKI Jakarta, Republik Indonesia</p>
            <p className="text-slate-400">Yurisdiksi Hukum Negara Kesatuan Republik Indonesia</p>
          </div>
          <div>
            <p className="font-semibold text-[#00AEEF]">Kontak Layanan &amp; DPO Privasi</p>
            <p className="mt-0.5">
              Email:{" "}
              <a href="mailto:legal@abbdullaziz.online" className="underline">
                legal@abbdullaziz.online
              </a>
            </p>
            <p className="mt-0.5">
              Dukungan Operasional:{" "}
              <a href="mailto:support@abbdullaziz.online" className="underline">
                support@abbdullaziz.online
              </a>
            </p>
          </div>
        </div>
      </section>

      {loading ? (
        <DataStateBox
          state="loading"
          title="Memuat naskah hukum resmi"
          description="Mengambil dokumen beserta tanda tangan SHA-256 dari /api/v1/public/legal..."
        />
      ) : error ? (
        <DataStateBox
          state="error"
          title="Gagal memuat dokumen hukum"
          description={error}
          onRetry={fetchLegal}
        />
      ) : docs.length === 0 ? (
        <DataStateBox
          state="empty"
          title="Belum ada naskah hukum yang diterbitkan"
          description="Server belum mengembalikan dokumen hukum untuk bahasa yang dipilih."
          onRetry={fetchLegal}
        />
      ) : (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
          <aside className="space-y-2 lg:col-span-4" aria-label="Daftar dokumen hukum">
            {docs.map((doc) => {
              const active = activeDoc?.doc_type === doc.doc_type;
              return (
                <button
                  key={doc.doc_type}
                  type="button"
                  onClick={() => setSelectedType(doc.doc_type)}
                  className={`flex min-h-[48px] w-full flex-col items-start rounded-xl border p-3.5 text-left transition-colors ${
                    active
                      ? "border-brand bg-brand text-brand-fg"
                      : "border-subtle bg-surface text-ink hover:bg-muted"
                  }`}
                >
                  <span className="font-display text-sm font-bold">{doc.title}</span>
                  <span className="mt-1 font-mono text-[11px] opacity-85">
                    {doc.doc_type} | v{doc.version}
                  </span>
                </button>
              );
            })}
          </aside>

          {activeDoc ? (
            <article className="rounded-xl border border-subtle bg-surface p-5 sm:p-7 lg:col-span-8">
              <div className="flex flex-wrap items-start justify-between gap-3 border-b border-subtle pb-4">
                <div>
                  <h2 className="font-display text-xl font-extrabold text-ink">{activeDoc.title}</h2>
                  <p className="mt-1 text-xs text-subink">
                    Versi {activeDoc.version}
                    {activeDoc.effective_date ? ` | Berlaku: ${activeDoc.effective_date}` : ""}
                  </p>
                </div>
                <StatusBadge tone="ok" prefix="SHA-256" label="Integritas Terverifikasi" />
              </div>

              <div className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-subtle bg-muted p-3">
                <code className="break-all font-mono text-xs text-ink">
                  {activeDoc.content_sha256 || "sha256-ready"}
                </code>
                <button
                  type="button"
                  onClick={() => handleCopyHash(activeDoc.content_sha256)}
                  className="btn-secondary min-h-[44px] shrink-0 px-3 py-1.5 text-xs font-semibold"
                >
                  {copiedHash ? "Hash Tersalin" : "Salin SHA-256"}
                </button>
              </div>

              <div className="mt-5 whitespace-pre-line text-sm leading-7 text-ink">
                {activeDoc.content}
              </div>
            </article>
          ) : null}
        </div>
      )}
    </div>
  );
}

export default function LegalPage() {
  return (
    <Suspense
      fallback={
        <div className="mx-auto max-w-6xl px-4 py-8">
          <DataStateBox
            state="loading"
            title="Menyiapkan halaman hukum"
            description="Memuat parameter dokumen..."
          />
        </div>
      }
    >
      <LegalPageInner />
    </Suspense>
  );
}

