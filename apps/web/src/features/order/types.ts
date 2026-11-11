export interface CatalogOption {
  id: string;
  name: string;
  price_delta: number;
  available: boolean;
  sub_groups?: CatalogGroup[];
}

export interface CatalogGroup {
  id: string;
  name: string;
  required: boolean;
  min: number;
  max: number;
  options: CatalogOption[];
}

export interface CatalogVariant {
  id: string;
  name: string;
  price: number;
  is_default: boolean;
  available: boolean;
}

export interface CatalogProduct {
  id: string;
  category_id: string | null;
  name: string;
  description?: string;
  image_url?: string | null;
  available: boolean;
  sold_out_reason?: string | null;
  variants: CatalogVariant[];
  modifier_groups: CatalogGroup[];
  bundle_slots?: unknown[];
}

export interface CatalogCategory {
  id: string;
  name: string;
}

export interface TableInfo {
  store_name: string;
  store_slug: string;
  table_number: string;
  label: string;
  ordering_open: boolean;
  closed_reason?: string;
  // Guest methods the store enabled; older APIs omit it, which means QRIS only.
  payment_methods?: PayMethod[];
  // How the order reaches the guest; older APIs omit it, which means pickup.
  service_mode?: ServiceMode;
}

export type ServiceMode = "pickup" | "table";

export type PayMethod = "qris" | "gopay" | "shopeepay";

export interface StoreCms {
  primary_hex?: string;
  hero_banner_url?: string;
  hero_subtitle_id?: string;
  hero_subtitle_en?: string;
  announcement_banner_id?: string;
  announcement_banner_en?: string;
  hide_powered_by_badge?: boolean;
}

export interface QuoteLine {
  product_name: string;
  variant_name?: string;
  selected_options?: string;
  quantity: number;
  subtotal_amount: number;
}

export interface Quote {
  subtotal_amount: number;
  discount_amount: number;
  service_charge_amount: number;
  tax_amount: number;
  total_amount: number;
  quote_fingerprint: string;
  lines: QuoteLine[];
}

export interface PaymentInfo {
  id: string;
  method?: PayMethod;
  qr_string?: string;
  deeplink_url?: string;
  expires_at?: string;
  amount: number;
}
