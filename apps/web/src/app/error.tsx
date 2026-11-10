"use client";

import { useLocale } from "@/components/Providers";
import { DataStateBox } from "@/components/Shell";

// The error itself stays out of the page; Next logs it to the browser console.
export default function ErrorPage({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const { t } = useLocale();
  return (
    <div className="mx-auto max-w-md px-5 py-16">
      <DataStateBox state="error" title={t.errorTitle} description={t.errorBody} onRetry={reset} />
    </div>
  );
}
