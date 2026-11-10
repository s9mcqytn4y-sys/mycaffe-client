"use client";

import { useEffect, useRef, useState } from "react";
import { useLocale } from "@/components/Providers";

export const sectionId = (id: string) => `menu-${id}`;

/** Sticky jump list: a tap scrolls to the heading, and the chip of the section in view stays highlighted. */
export function CategoryNav({ sections }: { sections: { id: string; name: string }[] }) {
  const { t } = useLocale();
  const [active, setActive] = useState(sections[0]?.id);
  const nav = useRef<HTMLElement>(null);

  useEffect(() => {
    const headings = sections.map((s) => document.getElementById(sectionId(s.id))).filter((h): h is HTMLElement => !!h);
    // The band just below the sticky strip decides which section counts as current.
    const observer = new IntersectionObserver(
      (entries) => {
        const hit = entries.filter((e) => e.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0];
        if (hit) setActive(hit.target.id.slice("menu-".length));
      },
      { rootMargin: "-64px 0px -60% 0px" }
    );
    headings.forEach((h) => observer.observe(h));
    return () => observer.disconnect();
  }, [sections]);

  useEffect(() => {
    nav.current?.querySelector('[aria-current="true"]')?.scrollIntoView({ block: "nearest", inline: "nearest" });
  }, [active]);

  return (
    <nav ref={nav} aria-label={t.categories} className="no-scrollbar sticky top-0 z-10 mt-3 flex gap-2 overflow-x-auto bg-canvas px-5 py-2">
      {sections.map((s) => (
        <button
          key={s.id}
          type="button"
          aria-current={active === s.id ? "true" : undefined}
          onClick={() => {
            setActive(s.id);
            document.getElementById(sectionId(s.id))?.scrollIntoView({ block: "start" });
          }}
          className={`min-h-[44px] shrink-0 rounded-lg px-3 text-sm font-semibold ${active === s.id ? "bg-brand text-brand-fg" : "text-ink"}`}
        >
          {s.name}
        </button>
      ))}
    </nav>
  );
}
