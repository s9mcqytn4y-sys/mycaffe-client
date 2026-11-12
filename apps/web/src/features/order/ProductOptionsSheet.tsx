"use client";

import React, { useState } from "react";
import { useLocale } from "@/components/Providers";
import { Minus, Plus } from "lucide-react";
import { Sheet } from "@/components/Sheet";
import { formatIdr } from "@/lib/format";
import type { CartLine, SelectedOption } from "./cart";
import { ProductImage } from "./ProductImage";
import type { CatalogGroup, CatalogProduct } from "./types";

type Selection = Record<string, string[]>;

function groupMissing(group: CatalogGroup, selection: Selection): boolean {
  const need = group.required ? Math.max(group.min, 1) : group.min;
  return (selection[group.id]?.length ?? 0) < need;
}

function toggle(selection: Selection, group: CatalogGroup, optionId: string): Selection {
  const current = selection[group.id] ?? [];
  if (group.max === 1) return { ...selection, [group.id]: current[0] === optionId ? [] : [optionId] };
  if (current.includes(optionId)) return { ...selection, [group.id]: current.filter((id) => id !== optionId) };
  if (group.max > 0 && current.length >= group.max) return selection;
  return { ...selection, [group.id]: [...current, optionId] };
}

function GroupFields({
  group,
  selection,
  onChange
}: {
  group: CatalogGroup;
  selection: Selection;
  onChange: (next: Selection) => void;
}) {
  const { t } = useLocale();
  const selected = selection[group.id] ?? [];
  return (
    <fieldset className="mt-6">
      <legend className="text-base font-bold">
        {group.name}
        <span className="ml-2 text-xs font-normal text-subink">
          {group.required ? t.required : ""}
          {group.max > 1 ? ` ${t.chooseUpTo} ${group.max}` : ""}
        </span>
      </legend>
      {group.options.map((option) => (
        <label
          key={option.id}
          className="mt-2 flex min-h-[48px] cursor-pointer items-center gap-3 rounded-xl border border-subtle px-3.5 text-[0.9375rem] has-[:checked]:border-brand has-[:checked]:bg-muted has-[:disabled]:cursor-not-allowed has-[:disabled]:opacity-50"
        >
          <input
            type={group.max === 1 ? "radio" : "checkbox"}
            name={group.id}
            disabled={!option.available}
            checked={selected.includes(option.id)}
            onClick={() => onChange(toggle(selection, group, option.id))}
            onChange={() => undefined}
            className="h-5 w-5 accent-[var(--brand-primary)]"
          />
          <span className="flex-1">{option.available ? option.name : `${option.name} (${t.soldOut})`}</span>
          {option.price_delta > 0 ? <span className="tabular-nums text-subink">+{formatIdr(option.price_delta)}</span> : null}
        </label>
      ))}
    </fieldset>
  );
}

export function ProductOptionsSheet({
  product,
  onAdd,
  onClose
}: {
  product: CatalogProduct;
  onAdd: (line: Omit<CartLine, "key">) => void;
  onClose: () => void;
}) {
  const { t } = useLocale();
  const variants = product.variants.filter((v) => v.available);
  const [variantId, setVariantId] = useState((variants.find((v) => v.is_default) ?? variants[0])?.id);
  const [selection, setSelection] = useState<Selection>({});
  const [notes, setNotes] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [showError, setShowError] = useState(false);

  const activeSubGroups = (group: CatalogGroup) =>
    group.options.filter((o) => selection[group.id]?.includes(o.id)).flatMap((o) => o.sub_groups ?? []);

  const incomplete = product.modifier_groups.some(
    (g) => groupMissing(g, selection) || activeSubGroups(g).some((sub) => groupMissing(sub, selection))
  );

  const submit = () => {
    if (incomplete) {
      setShowError(true);
      return;
    }
    const options: SelectedOption[] = product.modifier_groups.flatMap((group) =>
      (selection[group.id] ?? []).map((optionId) => ({
        optionId,
        subOptionIds: (group.options.find((o) => o.id === optionId)?.sub_groups ?? []).flatMap(
          (sub) => selection[sub.id] ?? []
        )
      }))
    );
    const nameOf = (groups: CatalogGroup[], id: string): string | undefined =>
      groups.flatMap((g) => g.options).find((o) => o.id === id)?.name;
    const details = [
      product.variants.length > 1 ? product.variants.find((v) => v.id === variantId)?.name : undefined,
      ...product.modifier_groups.flatMap((group) =>
        (selection[group.id] ?? []).flatMap((optionId) => {
          const subs = group.options.find((o) => o.id === optionId)?.sub_groups ?? [];
          return [nameOf([group], optionId), ...subs.flatMap((sub) => (selection[sub.id] ?? []).map((id) => nameOf([sub], id)))];
        })
      )
    ].filter((d): d is string => Boolean(d));
    onAdd({ productId: product.id, variantId, name: product.name, options, quantity, notes: notes.trim(), details });
  };

  return (
    <Sheet title={product.name} onClose={onClose}>
      {product.image_url ? (
        <ProductImage src={product.image_url} alt={product.name} width={640} height={480} className="mb-4 aspect-[4/3] w-full rounded-2xl" />
      ) : null}
      {product.description ? <p className="text-[0.9375rem] leading-relaxed text-subink">{product.description}</p> : null}

      {product.variants.length > 1 ? (
        <fieldset className="mt-6">
          <legend className="text-base font-bold">{t.choose}</legend>
          {product.variants.map((v) => (
            <label
              key={v.id}
              className="mt-2 flex min-h-[48px] cursor-pointer items-center gap-3 rounded-xl border border-subtle px-3.5 text-[0.9375rem] has-[:checked]:border-brand has-[:checked]:bg-muted has-[:disabled]:cursor-not-allowed has-[:disabled]:opacity-50"
            >
              <input
                type="radio"
                name="variant"
                disabled={!v.available}
                checked={variantId === v.id}
                onChange={() => setVariantId(v.id)}
                className="h-5 w-5 accent-[var(--brand-primary)]"
              />
              <span className="flex-1">{v.available ? v.name : `${v.name} (${t.soldOut})`}</span>
              <span className="font-semibold tabular-nums">{formatIdr(v.price)}</span>
            </label>
          ))}
        </fieldset>
      ) : null}

      {product.modifier_groups.map((group) => (
        <div key={group.id}>
          <GroupFields group={group} selection={selection} onChange={setSelection} />
          {activeSubGroups(group).map((sub) => (
            <div key={sub.id} className="pl-8">
              <GroupFields group={sub} selection={selection} onChange={setSelection} />
            </div>
          ))}
        </div>
      ))}

      <label htmlFor="line-notes" className="mt-6 block text-base font-bold">
        {t.notes}
      </label>
      <textarea
        id="line-notes"
        value={notes}
        onChange={(e) => setNotes(e.target.value)}
        maxLength={200}
        rows={2}
        aria-describedby="line-notes-hint"
        className="input-field mt-1 w-full"
      />
      <p id="line-notes-hint" className="mt-1 text-xs text-subink">
        {t.notesHint}
      </p>

      {showError && incomplete ? (
        <p role="alert" className="mt-4 text-sm font-semibold text-[var(--state-err-fg)]">
          {t.needChoice}
        </p>
      ) : null}

      <div className="sticky bottom-0 -mx-5 mt-6 flex items-center gap-3 border-t border-subtle bg-surface px-5 pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-3">
        <div role="group" aria-label={t.quantity} className="flex items-center gap-1 rounded-full bg-muted p-1">
          <button
            type="button"
            aria-label={t.decrease}
            disabled={quantity <= 1}
            onClick={() => setQuantity((q) => q - 1)}
            className="btn-secondary h-11 w-11 rounded-full"
          >
            <Minus size={18} />
          </button>
          <span aria-live="polite" className="min-w-7 text-center text-base font-bold tabular-nums">
            {quantity}
          </span>
          <button
            type="button"
            aria-label={t.increase}
            onClick={() => setQuantity((q) => q + 1)}
            className="btn-secondary h-11 w-11 rounded-full"
          >
            <Plus size={18} />
          </button>
        </div>
        <button type="button" onClick={submit} className="btn-primary min-h-[52px] flex-1 rounded-xl text-base font-bold">
          {t.addToCart}
        </button>
      </div>
    </Sheet>
  );
}
