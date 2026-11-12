"use client";

import { useEffect, useRef, useState } from "react";
import { useLocale } from "@/components/Providers";

export const sectionId = (id: string) => `menu-${id}`;

/** Category tabs in the bottom dock: a tap scrolls to the heading, and the tab of the section in view stays highlighted. */
export function CategoryNav({ sections }: { sections: { id: string; name: string }[] }) {
  const { t } = useLocale();
  const [active, setActive] = useState(sections[0]?.id);
  const nav = useRef<HTMLElement>(null);

  useEffect(() => {
    const headings = sections.map((s) => document.getElementById(sectionId(s.id))).filter((h): h is HTMLElement => !!h);
    // The top third of the screen decides which section counts as current.
    const observer = new IntersectionObserver(
      (entries) => {
        const hit = entries.filter((e) => e.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0];
        if (hit) setActive(hit.target.id.slice("menu-".length));
      },
      { rootMargin: "0px 0px -65% 0px" }
    );
    headings.forEach((h) => observer.observe(h));
    return () => observer.disconnect();
  }, [sections]);

  useEffect(() => {
    nav.current?.querySelector('[aria-current="true"]')?.scrollIntoView({ block: "nearest", inline: "nearest" });
  }, [active]);

  return (
    <nav ref={nav} aria-label={t.categories} className="no-scrollbar flex snap-x gap-1.5 overflow-x-auto px-3 pt-2">
      {sections.map((s) => (
        <button
          key={s.id}
          type="button"
          aria-current={active === s.id ? "true" : undefined}
          onClick={() => {
            setActive(s.id);
            document.getElementById(sectionId(s.id))?.scrollIntoView({ block: "start" });
          }}
          className={`min-h-[44px] shrink-0 snap-start rounded-full px-4 text-sm font-semibold ${active === s.id ? "bg-brand text-brand-fg" : "bg-muted text-ink"}`}
        >
          {s.name}
        </button>
      ))}
    </nav>
  );
}
