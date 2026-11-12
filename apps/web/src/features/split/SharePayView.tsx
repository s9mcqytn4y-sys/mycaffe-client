"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Icon } from "@/components/Icon";
import { useLocale } from "@/components/Providers";
import { QrCodeSvg } from "@/components/QrCodeSvg";
import { ApiError, apiFetch, isMaintenance } from "@/lib/api";
import { formatIdr } from "@/lib/format";

interface Share {
  store_name: string;
  amount: number;
  status: string;
}

interface ShareCharge {
  qr_string: string;
  expires_at?: string;
  amount: number;
}

const POLL_MS = 5_000;

/** One person's share of a split bill, opened from a link the orderer sent (W13). QRIS only. */
export function SharePayView({ token }: { token: string }) {
  const { t, locale } = useLocale();
  const path = `/api/v1/public/split-shares/${encodeURIComponent(token)}`;
  const [share, setShare] = useState<Share | null>(null);
  const [state, setState] = useState<"loading" | "ready" | "missing" | "maintenance" | "failed">("loading");
  const [charge, setCharge] = useState<ShareCharge | null>(null);
  const [expired, setExpired] = useState(false);
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // One key per QR: a double tap replays the same charge, a new QR after expiry gets a new key.
  const payKey = useRef(crypto.randomUUID());

  const load = useCallback(async () => {
    try {
      setShare(await apiFetch<Share>(path));
      setState("ready");
    } catch (e) {
      setState(e instanceof ApiError && e.status === 404 ? "missing" : isMaintenance(e) ? "maintenance" : "failed");
    }
  }, [path]);

  useEffect(() => {
    void load();
  }, [load]);

  const paid = share?.status === "paid";
  useEffect(() => {
    if (!charge || paid) return;
    const id = setInterval(() => {
      if (!document.hidden) void load();
    }, POLL_MS);
    return () => clearInterval(id);
  }, [charge, paid, load]);

  useEffect(() => {
    setExpired(false);
    if (!charge?.expires_at) return;
    const timer = setTimeout(() => setExpired(true), Math.max(new Date(charge.expires_at).getTime() - Date.now(), 0));
    return () => clearTimeout(timer);
  }, [charge]);

  const pay = async (fresh: boolean) => {
    if (fresh) payKey.current = crypto.randomUUID();
    setPaying(true);
    setError(null);
    try {
      setCharge(await apiFetch<ShareCharge>(`${path}/payments`, { method: "POST", body: { idempotency_key: payKey.current } }));
    } catch (e) {
      setError(isMaintenance(e) ? t.maintenanceShort : t.checkoutFailed);
    } finally {
      setPaying(false);
    }
  };

  if (state === "loading") return <p className="mx-auto max-w-md px-5 py-10 text-subink">{t.loadingTitle}</p>;
  if (state !== "ready" || !share) {
    const [title, body] =
      state === "missing"
        ? [t.shareMissingTitle, t.shareMissingBody]
        : state === "maintenance"
          ? [t.maintenanceTitle, t.maintenanceBody]
          : [t.loadFailedTitle, t.loadFailedBody];
    return (
      <section className="mx-auto max-w-md px-5 py-10">
        <h1 className="text-2xl font-bold">{title}</h1>
        <p className="mt-2 text-base">{body}</p>
        {state !== "missing" ? (
          <button type="button" onClick={() => void load()} className="btn-primary mt-6 min-h-[48px] px-6 font-bold">
            {t.retry}
          </button>
        ) : null}
      </section>
    );
  }

  if (paid) {
    return (
      <section className="mx-auto max-w-md px-5 py-10">
        <span className="inline-block text-[var(--state-ok-fg)] motion-safe:animate-pop">
          <Icon name="check" size={56} />
        </span>
        <h1 className="mt-3 text-2xl font-bold">{t.sharePaidTitle}</h1>
        <p className="mt-2 text-base">{t.sharePaidBody}</p>
      </section>
    );
  }

  return (
    <section className="mx-auto flex max-w-md flex-col items-center px-5 py-8 text-center">
      <p className="text-sm text-subink">{share.store_name}</p>
      <h1 className="text-2xl font-bold">{t.sharePageTitle}</h1>
      <p className="mt-5 text-3xl font-bold">{formatIdr(share.amount)}</p>

      {!charge ? (
        <button type="button" disabled={paying} onClick={() => void pay(false)} className="btn-primary mt-6 min-h-[56px] w-full text-base font-bold">
          {paying ? t.placing : t.sharePayCta}
        </button>
      ) : expired ? (
        <div role="alert" className="mt-6 flex flex-col items-center">
          <p className="font-semibold">{t.expired}</p>
          <button type="button" disabled={paying} onClick={() => void pay(true)} className="btn-primary mt-3 min-h-[56px] px-6 font-bold">
            {t.newQr}
          </button>
        </div>
      ) : (
        <>
          <p className="mt-2 text-sm text-subink">{t.scanHint}</p>
          <div className="mt-4 rounded-xl bg-white p-4">
            <QrCodeSvg value={charge.qr_string} size={240} />
          </div>
          {charge.expires_at ? (
            <p className="mt-4 text-sm">
              {t.payBefore}{" "}
              <strong>{new Date(charge.expires_at).toLocaleTimeString(locale, { hour: "2-digit", minute: "2-digit" })}</strong>
            </p>
          ) : null}
          <p aria-live="polite" className="mt-2 text-sm text-subink">{t.waiting}</p>
        </>
      )}
      {error ? <p role="alert" className="mt-4 text-sm font-semibold text-[var(--state-err-fg)]">{error}</p> : null}
    </section>
  );
}
