export interface OfflineOrderQueueItem {
  id: string;
  storeSlug: string;
  customerName: string;
  totalIdr: number;
  paymentMethod: "cash" | "static_qris";
  itemCount: number;
  createdAt: string;
  syncStatus: "pending_sync" | "synced";
}

const DB_NAME = "MyCaffeOfflineQueue";
const DB_VERSION = 1;
const STORE_NAME = "queued_orders";

function openOfflineDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === "undefined") {
      reject(new Error("IndexedDB tidak tersedia pada peramban ini."));
      return;
    }
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: "id" });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error ?? new Error("Gagal membuka IndexedDB MyCaffeOfflineQueue."));
  });
}

export async function listOfflineOrders(): Promise<OfflineOrderQueueItem[]> {
  const db = await openOfflineDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, "readonly");
    const store = tx.objectStore(STORE_NAME);
    const req = store.getAll();
    req.onsuccess = () => {
      const list = (req.result as OfflineOrderQueueItem[]) || [];
      list.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
      db.close();
      resolve(list);
    };
    req.onerror = () => {
      db.close();
      reject(req.error ?? new Error("Gagal membaca antrean offline."));
    };
  });
}

export async function enqueueOfflineOrder(item: OfflineOrderQueueItem): Promise<void> {
  const db = await openOfflineDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, "readwrite");
    tx.objectStore(STORE_NAME).put(item);
    tx.oncomplete = () => {
      db.close();
      resolve();
    };
    tx.onerror = () => {
      db.close();
      reject(tx.error ?? new Error("Gagal menyimpan transaksi offline."));
    };
  });
}

export async function markAllOfflineOrdersSynced(): Promise<number> {
  const items = await listOfflineOrders();
  const pending = items.filter((it) => it.syncStatus === "pending_sync");
  if (pending.length === 0) return 0;

  const db = await openOfflineDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, "readwrite");
    const store = tx.objectStore(STORE_NAME);
    for (const item of pending) {
      store.put({ ...item, syncStatus: "synced" });
    }
    tx.oncomplete = () => {
      db.close();
      resolve(pending.length);
    };
    tx.onerror = () => {
      db.close();
      reject(tx.error ?? new Error("Gagal memperbarui status sinkronisasi offline."));
    };
  });
}

export async function clearOfflineOrders(): Promise<void> {
  const db = await openOfflineDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, "readwrite");
    tx.objectStore(STORE_NAME).clear();
    tx.oncomplete = () => {
      db.close();
      resolve();
    };
    tx.onerror = () => {
      db.close();
      reject(tx.error ?? new Error("Gagal mengosongkan antrean offline."));
    };
  });
}

export interface StorageQuotaSnapshot {
  supported: boolean;
  usageBytes: number;
  quotaBytes: number;
}

export async function estimateBrowserStorage(): Promise<StorageQuotaSnapshot> {
  if (typeof navigator === "undefined" || !navigator.storage || typeof navigator.storage.estimate !== "function") {
    return { supported: false, usageBytes: 0, quotaBytes: 0 };
  }
  const est = await navigator.storage.estimate();
  return {
    supported: true,
    usageBytes: est.usage ?? 0,
    quotaBytes: est.quota ?? 0
  };
}

