"use client";

import { Plus, UtensilsCrossed } from "lucide-react";
import { useLocale } from "@/components/Providers";
import { formatIdr } from "@/lib/format";
import { ProductImage } from "./ProductImage";
import type { CatalogProduct } from "./types";

export function defaultVariant(product: CatalogProduct) {
  return product.variants.find((v) => v.is_default && v.available) ?? product.variants.find((v) => v.available);
}

interface ItemProps {
  product: CatalogProduct;
  canOrder: boolean;
  onOpen: () => void;
  onQuickAdd: () => void;
  eager?: boolean;
}

function useItem(product: CatalogProduct, canOrder: boolean) {
  const variant = defaultVariant(product);
  const soldOut = !product.available || !variant;
  const bundled = (product.bundle_slots?.length ?? 0) > 0;
  const needsChoice = product.variants.length > 1 || product.modifier_groups.length > 0;
  return { variant, soldOut, bundled, needsChoice, showAdd: !soldOut && !bundled && canOrder };
}

function AddButton({ product, needsChoice, onOpen, onQuickAdd }: { product: CatalogProduct; needsChoice: boolean; onOpen: () => void; onQuickAdd: () => void }) {
  const { t } = useLocale();
  return (
    <button
      type="button"
      aria-label={`${needsChoice ? t.choose : t.add} ${product.name}`}
      onClick={needsChoice ? onOpen : onQuickAdd}
      className="btn-primary h-11 w-11 shrink-0 rounded-full"
    >
      <Plus size={20} strokeWidth={2.5} />
    </button>
  );
}

function Meta({ soldOut, bundled, needsChoice }: { soldOut: boolean; bundled: boolean; needsChoice: boolean }) {
  const { t } = useLocale();
  if (soldOut) return <span className="text-xs font-semibold text-[var(--state-err-fg)]">{t.soldOut}</span>;
  if (bundled) return <span className="text-xs text-subink">{t.askStaff}</span>;
  return needsChoice ? <span className="text-xs text-subink">{t.hasOptions}</span> : null;
}

/** Photo card for sections where the outlet uploaded photos. The photo opens the details sheet. */
export function ProductCard({ product, canOrder, onOpen, onQuickAdd, eager = false }: ItemProps) {
  const { t } = useLocale();
  const item = useItem(product, canOrder);
  return (
    <li className="flex min-w-0 flex-col overflow-hidden rounded-2xl border border-subtle bg-surface">
      <button type="button" aria-label={`${t.details} ${product.name}`} onClick={onOpen} className={`relative block w-full ${item.soldOut ? "opacity-50 grayscale" : ""}`}>
        {product.image_url ? (
          <ProductImage src={product.image_url} alt="" width={320} height={320} eager={eager} className="aspect-square w-full" />
        ) : (
          <span aria-hidden="true" className="grid aspect-square w-full place-items-center bg-muted text-subink">
            <UtensilsCrossed size={40} strokeWidth={1.5} />
          </span>
        )}
      </button>
      <div className="flex flex-1 flex-col gap-1 p-3">
        <h3 className="line-clamp-2 text-[0.9375rem] font-semibold leading-snug">{product.name}</h3>
        <Meta {...item} />
        <div className="mt-auto flex items-end justify-between gap-2 pt-1">
          <p className="text-base font-bold tabular-nums">{item.variant ? formatIdr(item.variant.price) : ""}</p>
          {item.showAdd ? <AddButton product={product} needsChoice={item.needsChoice} onOpen={onOpen} onQuickAdd={onQuickAdd} /> : null}
        </div>
      </div>
    </li>
  );
}

/** Text row for sections without photos, so the menu reads as a list instead of a wall of placeholders. */
export function ProductRow({ product, canOrder, onOpen, onQuickAdd }: ItemProps) {
  const { t } = useLocale();
  const item = useItem(product, canOrder);
  return (
    <li className="flex items-center gap-3 py-3">
      <button type="button" aria-label={`${t.details} ${product.name}`} onClick={onOpen} className="min-w-0 flex-1 text-left">
        <span className="block text-[0.9375rem] font-semibold leading-snug">{product.name}</span>
        {product.description ? <span className="mt-0.5 line-clamp-2 block text-sm text-subink">{product.description}</span> : null}
        <span className="mt-1 flex items-center gap-2">
          <span className="text-base font-bold tabular-nums">{item.variant ? formatIdr(item.variant.price) : ""}</span>
          <Meta {...item} />
        </span>
      </button>
      {item.showAdd ? <AddButton product={product} needsChoice={item.needsChoice} onOpen={onOpen} onQuickAdd={onQuickAdd} /> : null}
    </li>
  );
}
