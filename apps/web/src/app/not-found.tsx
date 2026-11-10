"use client";

import { useLocale } from "@/components/Providers";
import { DataStateBox } from "@/components/Shell";

export default function NotFound() {
  const { t } = useLocale();
  return (
    <div className="mx-auto max-w-md px-5 py-16">
      <DataStateBox state="empty" title={t.notFoundTitle} description={t.notFoundBody} />
    </div>
  );
}
