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
    const reduced = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    nav.current
      ?.querySelector('[aria-current="true"]')
      ?.scrollIntoView({ block: "nearest", inline: "center", behavior: reduced ? "auto" : "smooth" });
  }, [active]);

  return (
    <nav
      ref={nav}
      aria-label={t.categories}
      className="no-scrollbar flex snap-x gap-2 overflow-x-auto px-4 pt-2 [mask-image:linear-gradient(to_right,transparent,black_12px,black_calc(100%-12px),transparent)]"
    >
      {sections.map((s) => (
        <button
          key={s.id}
          type="button"
          aria-current={active === s.id ? "true" : undefined}
          onClick={() => {
            setActive(s.id);
            const reduced = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
            document.getElementById(sectionId(s.id))?.scrollIntoView({ block: "start", behavior: reduced ? "auto" : "smooth" });
          }}
          className={`min-h-[44px] shrink-0 snap-center rounded-full border px-4 text-sm font-semibold motion-safe:transition-all motion-safe:active:scale-[0.97] ${
            active === s.id
              ? "border-brand bg-brand text-brand-fg shadow-sm"
              : "border-transparent bg-muted text-ink hover:border-subtle"
          }`}
        >
          {s.name}
        </button>
      ))}
    </nav>
  );
}
