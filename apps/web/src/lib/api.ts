import type { paths } from "../types/api.generated";

export class ApiError extends Error {
  public readonly status: number;
  public readonly details?: unknown;

  constructor(status: number, message: string, details?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

export interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  bearerToken?: string;
  platformAdminKey?: string;
  guestToken?: string;
  displayToken?: string;
  idempotencyKey?: string;
  requestId?: string;
  includeCredentials?: boolean;
}

export async function apiFetch<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    Accept: "application/json",
    "X-Request-ID": opts.requestId || (typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `req_${Date.now()}`)
  };

  if (opts.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (opts.bearerToken) {
    headers["Authorization"] = `Bearer ${opts.bearerToken}`;
  }
  if (opts.displayToken) {
    headers["Authorization"] = `Display ${opts.displayToken}`;
  }
  if (opts.platformAdminKey) {
    headers["X-Platform-Admin-Key"] = opts.platformAdminKey;
  }
  if (opts.guestToken) {
    headers["X-Guest-Token"] = opts.guestToken;
  }
  if (opts.idempotencyKey) {
    headers["Idempotency-Key"] = opts.idempotencyKey;
    headers["X-Idempotency-Key"] = opts.idempotencyKey;
  }

  let res: Response;
  try {
    res = await fetch(path, {
      method: opts.method ?? "GET",
      headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
      credentials: opts.includeCredentials ? "include" : "same-origin",
      cache: "no-store"
    });
  } catch (err) {
    const reason = err instanceof Error ? err.message : "Koneksi jaringan gagal";
    throw new ApiError(0, `Server backend belum dapat dihubungi (${reason}). Pastikan layanan API berjalan di port 8080.`);
  }

  const rawText = await res.text();
  let parsed: unknown = null;
  if (rawText) {
    try {
      parsed = JSON.parse(rawText);
    } catch {
      parsed = { raw: rawText };
    }
  }

  if (!res.ok) {
    const obj = (parsed && typeof parsed === "object" ? parsed : {}) as Record<string, unknown>;
    const msg =
      (typeof obj.error === "string" && obj.error) ||
      (typeof obj.message === "string" && obj.message) ||
      `Permintaan gagal dengan kode HTTP ${res.status}`;
    throw new ApiError(res.status, msg, parsed);
  }

  return (parsed ?? ({} as T)) as T;
}

// Type-safe API caller derived from OpenAPI paths
export type ApiPaths = keyof paths;
export type HttpMethod = "get" | "post" | "put" | "patch" | "delete";

export async function typedApiFetch<
  Path extends ApiPaths,
  Method extends HttpMethod & keyof paths[Path]
>(
  path: Path,
  method: Method,
  options?: RequestOptions & {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    body?: any;
  }
): Promise<unknown> {
  return apiFetch(path as string, {
    ...options,
    method: method.toUpperCase() as RequestOptions["method"]
  });
}

// 1. Public Plans, Legal & Onboarding
export interface SubscriptionPlan {
  code: string;
  name: string;
  description: string;
  monthly_price_idr: number;
  annual_price_idr: number;
  max_tables: number;
  max_staff: number;
  max_products: number;
  features: string[];
}

export function normalizePlan(raw: Record<string, unknown>): SubscriptionPlan {
  const code = String(raw.code ?? raw.plan_code ?? "starter_umkm");
  const name = String(raw.name ?? raw.display_name ?? code);
  const description = String(raw.description ?? raw.summary ?? "");
  const monthly = Number(raw.monthly_price_idr ?? raw.price_monthly_idr ?? raw.monthly_price ?? 0);
  const annual = Number(raw.annual_price_idr ?? raw.price_annual_idr ?? raw.annual_price ?? monthly * 10);
  const maxTables = Number(raw.max_tables ?? raw.table_limit ?? 20);
  const maxStaff = Number(raw.max_staff ?? raw.staff_limit ?? 10);
  const maxProducts = Number(raw.max_products ?? raw.product_limit ?? 200);
  const rawFeatures = Array.isArray(raw.features) ? raw.features.map(String) : [];
  return {
    code,
    name,
    description,
    monthly_price_idr: monthly,
    annual_price_idr: annual,
    max_tables: maxTables,
    max_staff: maxStaff,
    max_products: maxProducts,
    features: rawFeatures
  };
}

export interface LegalDocument {
  doc_type: string;
  title: string;
  version: string;
  effective_date: string;
  content_sha256: string;
  content: string;
}

export function normalizeLegalDoc(raw: Record<string, unknown>): LegalDocument {
  return {
    doc_type: String(raw.doc_type ?? raw.code ?? raw.slug ?? "document"),
    title: String(raw.title ?? "Dokumen Hukum"),
    version: String(raw.version ?? "1.0"),
    effective_date: String(raw.effective_date ?? raw.updated_at ?? ""),
    content_sha256: String(raw.content_sha256 ?? raw.sha256 ?? ""),
    content: String(raw.content_markdown ?? raw.content ?? raw.body ?? "")
  };
}

export interface OnboardingApplicationInput {
  idempotency_key: string;
  requested_slug: string;
  business_name: string;
  business_entity_type: string;
  owner_name: string;
  owner_email: string;
  owner_phone: string;
  city_region: string;
  selected_plan_code: string;
  billing_cycle: "monthly" | "annual";
  nib: string;
  npwp: string;
  accepted_tos_sha256: string;
  accepted_pdp_sha256: string;
}

export interface TenantOnboardingApplication {
  id: string;
  requested_slug: string;
  business_name: string;
  business_entity_type?: string;
  owner_name: string;
  owner_email: string;
  owner_phone?: string;
  city_region?: string;
  selected_plan_code: string;
  billing_cycle: string;
  nib_masked?: string;
  npwp_masked?: string;
  status: string;
  email_verified?: boolean;
  provisioned_store_id?: string;
  review_notes?: string;
  created_at?: string;
}

export interface ApplyOnboardingResponse {
  application: TenantOnboardingApplication;
  is_idempotent_replay?: boolean;
  otp_delivery_channel?: string;
  dev_otp_preview?: string;
}

// 2. Platform Superadmin
export interface ProvisionedTableSummary {
  id: string;
  table_number: string;
  qr_token: string;
}

export interface ProvisionOnboardingResponse {
  application: TenantOnboardingApplication;
  store_id: string;
  store_slug: string;
  owner_user_id: string;
  owner_email: string;
  kyb_status: string;
  tables: ProvisionedTableSummary[];
}

// 3. Staff Login & POS
export interface StaffLoginResponse {
  access_token: string;
  access_token_expires_at: string;
  staff: {
    id: string;
    email: string;
    full_name: string;
    role: string;
    permissions: string[];
    store: {
      id: string;
      slug: string;
      name: string;
    };
  };
}

export interface CatalogCategory {
  id: string;
  name: string;
  display_order?: number;
}

export interface CatalogProduct {
  id: string;
  category_id: string;
  name: string;
  description?: string;
  price: number;
  sku?: string;
  is_available: boolean;
  stock_quantity?: number;
  prep_station?: string;
}

export interface StoreTable {
  id: string;
  table_number: string;
  capacity: number;
  area?: string;
  qr_token: string;
  is_active?: boolean;
}

export interface KdsTicket {
  id: string;
  order_id?: string;
  queue_number?: string;
  order_number?: string;
  table_number?: string;
  status: string;
  station?: string;
  lines?: Array<{
    name?: string;
    variant?: string;
    quantity: number;
    options?: string;
    notes?: string;
  }>;
  created_at?: string;
}

// 4. Tenant Governance, Midtrans, Network, MikroTik, Notifications & Subscriptions
export interface StorePaymentCredentialsInput {
  environment: "sandbox" | "production";
  merchant_id: string;
  client_key: string;
  server_key: string;
  is_enabled: boolean;
  fallback_static_qris_enabled: boolean;
  verify_live: boolean;
}

export interface StoreNetworkNodeInput {
  node_name: string;
  node_type:
    | "thermal_printer"
    | "kds_display"
    | "cashier_tablet"
    | "local_edge_server"
    | "queue_tv"
    | "router_mikrotik";
  connection_protocol: "tcp_escpos" | "http_lan" | "websocket" | "usb_bridge" | "bluetooth";
  local_ip: string;
  local_port: number;
  mac_address: string;
  paper_width_mm: 58 | 80;
  drawer_kick_enabled: boolean;
  health_status: "online" | "degraded" | "offline" | "unknown";
  last_latency_ms: number;
  is_active: boolean;
}

export interface StoreNetworkNode extends StoreNetworkNodeInput {
  id?: string;
  updated_at?: string;
}

export interface StoreMikrotikProfile {
  router_model: string;
  wan_primary_interface: string;
  wan_backup_interface: string;
  pos_vlan_cidr: string;
  staff_vlan_cidr: string;
  guest_vlan_cidr: string;
  pos_wifi_ssid: string;
  guest_wifi_ssid: string;
  guest_rate_limit_mbps: number;
  pos_reserved_mbps: number;
  sftp_port: number;
  winbox_port: number;
  winbox_allowed_cidr: string;
  dns_servers: string;
}

export interface MikrotikGeneratedBundle {
  profile: StoreMikrotikProfile;
  rsc_script: string;
  script_sha256: string;
  winbox_steps: string[];
  filezilla_sftp_policy: Record<string, unknown> | string;
  topology_summary?: Record<string, unknown> | string;
}

export interface StoreNotificationConfigInput {
  email_mode: "platform_default" | "custom_smtp" | "disabled";
  smtp_host: string;
  smtp_port: number;
  smtp_tls_mode: "starttls" | "implicit";
  smtp_username: string;
  smtp_password: string;
  smtp_from_email: string;
  smtp_from_name: string;
  whatsapp_mode: "platform_bridge" | "custom_webhook" | "cloud_api" | "disabled";
  whatsapp_endpoint_url: string;
  whatsapp_phone_number_id: string;
  whatsapp_token: string;
}

export interface CreatePlatformInvoiceInput {
  plan_code: "starter_umkm" | "pro_cafe" | "enterprise_whitelabel";
  billing_cycle: "monthly" | "annual";
}

/** Issues a single-use realtime ticket and returns the socket URL. Call once per connection attempt. */
export async function realtimeUrl(ticketPath: string, opts: RequestOptions): Promise<string> {
  const { ticket } = await apiFetch<{ ticket: string }>(ticketPath, { ...opts, method: "POST" });
  const scheme = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${scheme}//${window.location.host}/ws?ticket=${encodeURIComponent(ticket)}`;
}
