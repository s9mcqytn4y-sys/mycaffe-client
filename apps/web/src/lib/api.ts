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

/** nginx answers 503 {"code":"MAINTENANCE"} while the maintenance switch is on (PRD v3 N3). */
export function isMaintenance(err: unknown): boolean {
  return err instanceof ApiError && err.status === 503 && (err.details as { code?: unknown } | null)?.code === "MAINTENANCE";
}

export interface RequestOptions {
  method?: "GET" | "POST";
  body?: unknown;
}

export async function apiFetch<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const method = opts.method ?? "GET";
  const headers: Record<string, string> = { Accept: "application/json" };
  if (opts.body !== undefined) headers["Content-Type"] = "application/json";
  if (method !== "GET") headers["X-CSRF-Protection"] = "1";

  let res: Response;
  try {
    res = await fetch(path, {
      method,
      headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
      credentials: "same-origin",
      cache: "no-store"
    });
  } catch {
    throw new ApiError(0, "Tidak bisa terhubung ke server. Periksa koneksi lalu coba lagi.");
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
    const message = typeof obj.error === "string" && obj.error ? obj.error : "Permintaan gagal. Coba lagi.";
    throw new ApiError(res.status, message, parsed);
  }

  return (parsed ?? {}) as T;
}
