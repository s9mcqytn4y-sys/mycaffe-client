"use client";

import { useLocale } from "@/components/Providers";
import { DataStateBox } from "@/components/Shell";
import { apiFetch } from "@/lib/api";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

interface LegalDocument {
  doc_type: string;
  version: string;
  title: string;
  content_markdown: string;
  content_sha256: string;
  effective_at: string;
}

const GUEST_TYPES = ["guest_privacy_notice", "guest_order_terms"];

export function LegalView({ audience }: { audience: "guest" | "tenant" }) {
  const { t, locale } = useLocale();
  const requested = useSearchParams().get("doc");
  // Tenant documents exist in Indonesian only.
  const docLocale = audience === "tenant" ? "id-ID" : locale;
  const [docs, setDocs] = useState<LegalDocument[] | null>(null);
  const [failed, setFailed] = useState(false);

  const load = useCallback(async () => {
    setFailed(false);
    setDocs(null);
    try {
      const res = await apiFetch<{ documents?: LegalDocument[] }>(`/api/v1/public/legal?locale=${docLocale}`);
      const forGuests = (d: LegalDocument) => GUEST_TYPES.includes(d.doc_type);
      setDocs((res.documents ?? []).filter((d) => (audience === "guest" ? forGuests(d) : !forGuests(d))));
    } catch {
      setFailed(true);
    }
  }, [docLocale, audience]);

  useEffect(() => {
    void load();
  }, [load]);

  const base = audience === "guest" ? "/legal" : "/legal/mitra";
  const active = docs?.find((d) => d.doc_type === requested) ?? docs?.[0];

  return (
    <div className="mx-auto max-w-3xl px-5 py-8">
      <h1 className="text-2xl font-bold">{audience === "guest" ? t.legalGuestTitle : t.legalTenantTitle}</h1>
      <p className="mt-2 text-base text-subink">{audience === "guest" ? t.legalGuestIntro : t.legalTenantIntro}</p>
      <Link href={audience === "guest" ? "/legal/mitra" : "/legal"} className="mt-2 inline-flex min-h-[44px] items-center text-sm font-semibold underline">
        {audience === "guest" ? t.legalToTenant : t.legalToGuest}
      </Link>

      <div className="mt-6">
        {failed ? (
          <DataStateBox state="error" title={t.legalFailed} description={t.legalFailedBody} onRetry={load} />
        ) : !docs ? (
          <DataStateBox state="loading" title={t.legalLoading} description="" />
        ) : !active ? (
          <DataStateBox state="empty" title={t.legalEmpty} description={t.legalContact} />
        ) : (
          <>
            {docs.length > 1 ? (
              <nav aria-label={t.legalDocs} className="flex flex-wrap gap-2">
                {docs.map((d) => (
                  <Link
                    key={d.doc_type}
                    href={`${base}?doc=${d.doc_type}`}
                    aria-current={d === active ? "page" : undefined}
                    className={`inline-flex min-h-[44px] items-center rounded-lg px-3 text-sm font-semibold ${
                      d === active ? "bg-brand text-brand-fg" : "border border-subtle text-ink"
                    }`}
                  >
                    {d.title}
                  </Link>
                ))}
              </nav>
            ) : null}
            <article className="mt-6 rounded-xl border border-subtle bg-surface p-5">
              <h2 className="text-xl font-bold">{active.title}</h2>
              <p className="mt-1 text-sm text-subink">
                {t.legalVersion} {active.version}
                {active.effective_at ? ` | ${t.legalEffective} ${new Date(active.effective_at).toLocaleDateString(locale)}` : ""}
              </p>
              <div className="mt-5 whitespace-pre-line text-base leading-7">{active.content_markdown}</div>
              <p className="mt-6 break-all font-mono text-xs text-subink">SHA-256 {active.content_sha256}</p>
            </article>
            <p className="mt-4 text-sm text-subink">{t.legalContact}</p>
          </>
        )}
      </div>
    </div>
  );
}
