import type { CatalogCategory, CatalogProduct } from "./types";

export interface MenuSection {
  id: string;
  name: string;
  products: CatalogProduct[];
}

/** One list in category order; empty categories are skipped and unknown ones go last under `otherName`. */
export function menuSections(categories: CatalogCategory[], products: CatalogProduct[], otherName: string): MenuSection[] {
  const known = new Set(categories.map((c) => c.id));
  const sections = categories.map((c) => ({ id: c.id, name: c.name, products: products.filter((p) => p.category_id === c.id) }));
  sections.push({ id: "other", name: otherName, products: products.filter((p) => !known.has(p.category_id ?? "")) });
  return sections.filter((s) => s.products.length > 0);
}
