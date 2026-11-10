# PRD: WPA Siap Pilot

Pemilik: Abdul Aziz (Director). Status: draf untuk disetujui. Tanggal: 10 November 2026.
Lanjutan dari `docs/RoadmapPilotOne.md`. W2 sampai W5 sudah live di demo (`sha-c8b9af1`).

## Ringkasan

Alur pesan dan bayar WPA sudah berjalan di demo, tetapi belum layak dibawa ke kafe pilot karena tiga hal: CI tidak menjalankan test apa pun, halaman legal menampilkan dokumen SaaS tenant berjudul MyCaffe kepada tamu, dan pesanan yang belum dibayar hilang dari jangkauan tamu saat tab ditutup. Rilis ini menutup ketiganya, merapikan tech debt yang ditemukan di audit, lalu menambah tiga perbaikan layar yang dilihat setiap tamu: menu satu daftar (W9), garis status pesanan (W10), dan foto produk yang stabil (W12).

## Keputusan yang sudah dikunci

| # | Keputusan | Alasan |
|---|---|---|
| Q1 | SEO hanya untuk `/` dan `/legal`. `/order/*` tetap `noindex` | Token meja bersifat privat. Pemasaran Cassy ada di situs terpisah, karena di layar tamu Cassy hanya muncul di footer |
| Q2 | Naskah tamu menjadi dua jenis dokumen baru: `guest_privacy_notice` dan `guest_order_terms`. Dokumen tenant pindah ke `/legal/mitra` | ToS dan privasi tenant dipakai sebagai persetujuan onboarding, jadi tidak boleh diubah maknanya. Untuk data tamu, kafe adalah pengendali data dan AzureDevID pemroses |
| Q3 | Cakupan UI: W9, W10, W12. W8 (pencarian) dan W11 (halaman struk) ditunda. W13 tetap terkunci sampai A9 | Ketiganya menyentuh layar yang dilihat setiap tamu dan cukup memakai komponen yang ada |
| Q4 | Pesanan aktif disimpan di `localStorage` sampai pesanan selesai, batal, atau sesi tamu habis. Keranjang tetap di `sessionStorage` | Cookie `__Secure-mc_guest` bertahan sampai sesi habis, jadi pesanan bisa dibuka lagi setelah tab ditutup |

## Temuan audit

Tidak ada marker `shortcut:` di `apps/web`. Semua temuan berikut didapat dari membaca kode dan memeriksa demo.

| ID | Lokasi | Masalah | Ditangani di |
|---|---|---|---|
| D1 | `.github/workflows` | Playwright dan `cart.test.mjs` tidak dijalankan CI | W7 |
| D2 | `src/app/legal/page.tsx` | Warna `#07111F`, `#00AEEF`, `slate-*` di luar token tamu; teks hardcoded bahasa Indonesia | L1 |
| D3 | `src/app/legal/page.tsx` | `fetchLegal` bergantung pada `selectedType`, jadi setiap klik tab mengambil ulang data. Ada import `Link` tak terpakai dan placeholder `sha256-ready` | L1 |
| D4 | `src/features/order/useOrderData.ts` | Polling 10 detik tidak berhenti di status akhir; WebSocket tidak pernah reconnect | T1 |
| D5 | `src/features/order/OrderApp.tsx` | Setiap tap "QR baru" membuat charge baru. Perlu bukti backend menolak bayar ganda dari QR lama | T2 |
| D6 | `api.ts`, `Shell.tsx`, `layout.tsx` | Pesan fallback, "Coba lagi", skip link, dan `lang="id"` tidak ikut locale | T3 |
| D7 | `src/app` | Tidak ada `not-found.tsx` dan `error.tsx` | P5, P6 |
| D8 | Dead code | `StatusBadge`, `icon-192.svg`, `icon-512.svg`, `SiteWebManifest.json` | T4 |
| D9 | `src/app/layout.tsx` | Semua halaman berjudul "Pesan dari meja"; `metadataBase` fallback ke apex | SEO |
| D10 | `nginx.demo.conf` | Demo tanpa CSP, HSTS, dan Permissions-Policy, sementara prod punya | T5 |
| D11 | `src/app/layout.tsx` | `next/font/google` diunduh saat build dan sudah gagal sekali di CI | T6 |

## Cakupan

### P0: wajib sebelum pilot

| ID | Kebutuhan | Kriteria terima |
|---|---|---|
| W7 | CI menjalankan `cart.test.mjs` dan Playwright Chromium serta WebKit (proyek `mobile-safari`) terhadap build produksi. Ditambah cek axe serious, navigasi keyboard penuh, dan zoom 200% | Job baru wajib hijau sebelum merge; laporan axe 0 pelanggaran serious |
| W1 | Tombol simpan QR sebagai PNG dan petunjuk unggah dari galeri e-wallet | Uji manual minimal 3 e-wallet lebih dulu; unduhan tertangkap Playwright |
| W6 | Uji pindai QR dengan logo tengah di aplikasi nyata; hapus logo bila ada yang gagal | Minimal 3 aplikasi berhasil |
| R1 | Pesanan aktif di `localStorage` (Q4) | E2E: tutup konteks browser, buka lagi URL meja, QR yang sama tampil; catatan terhapus setelah status `served` atau `cancelled` |
| L1 | Halaman legal tamu dan mitra (lihat bagian Legal) | Tidak ada kata MyCaffe terlihat; dua bahasa; token Espresso & Crema saja |
| T1 | Progres pesanan berhenti polling di status akhir; WebSocket reconnect dengan backoff eksponensial 1, 2, 4, 8, maksimal 30 detik | Unit test jadwal backoff; E2E tidak ada request track setelah `served` |
| T2 | Verifikasi backend: pembayaran dari QR lama setelah QR baru dibuat tidak menggandakan pelunasan. Bila belum aman, perbaiki di backend dulu | Test integrasi Go menunjukkan satu pelunasan |
| T3 | Semua teks UI lewat `messages.ts`; `lang` di `<html>` mengikuti locale | Grep teks Indonesia di luar `messages.ts` kosong, kecuali naskah legal dari API |
| T4 | Hapus dead code D8 | Build dan test hijau |
| T5 | Header keamanan demo disamakan dengan prod | `curl -I` menunjukkan CSP, HSTS, Permissions-Policy |
| T6 | Font di-self-host lewat `next/font/local` | Build tanpa akses jaringan berhasil |

### P1: masuk rilis ini

| ID | Kebutuhan | Kriteria terima |
|---|---|---|
| W9 | Menu satu daftar dengan heading per kategori. Strip kategori menjadi penunjuk lompat dan menyorot kategori yang sedang terlihat | E2E: tap kategori menggulir ke heading-nya; gulir manual memindah sorotan |
| W10 | Garis status: menunggu bayar, diterima, disiapkan, siap | `aria-live="polite"` mengumumkan perubahan; unit test pemetaan status |
| W12 | Foto detail di sheet produk; placeholder rasio tetap saat foto belum ada atau gagal | CLS di bawah 0,1 di Lighthouse mobile |
| U1 | Polish dari roadmap: scrollbar strip kategori disembunyikan, label varian hanya bila lebih dari satu, deskripsi seed berbahasa Indonesia, foto Iced Latte dan V60 diganti, nominal dan batas waktu terlihat tanpa gulir di 360 px | Screenshot 360 px dan 1280 px di PR |

### Bukan cakupan

W8 pencarian, W11 halaman struk dan riwayat, W13 bagi tagihan, mode gelap untuk tamu, PWA offline, dependensi baru (Zustand, GSAP, Sonner, pustaka komponen).

## Halaman

| Rute | Render | Indeks | Isi |
|---|---|---|---|
| `/` | Server | Ya | Ajakan pindai QR, tautan legal. Judul "Cassy: pesan dari meja" |
| `/order/[token]` | Server shell, isi client | Tidak | Menu, keranjang, pembayaran, status. Judul memakai nama gerai lewat `generateMetadata` |
| `/legal` | Server, data dari `/public/legal` | Ya | Dokumen tamu: `guest_privacy_notice`, `guest_order_terms` |
| `/legal/mitra` | Server | Ya | Dokumen tenant: ToS, privasi, refund, DPA |
| `not-found` (P5) | Server | Tidak | "Halaman tidak ditemukan" dua bahasa, ajakan pindai ulang QR |
| `error` (P6) | Client boundary | Tidak | Pesan gagal umum dan tombol muat ulang; tidak menampilkan stack |

`/legal` dirender di server supaya mesin pencari membaca isinya dan tidak ada layar loading. Pilihan dokumen memakai query `?doc=` dengan tautan biasa, bukan state client, sehingga setiap dokumen punya URL sendiri dan tidak ada fetch ulang per klik.

## Komponen dan lifecycle

Setiap komponen data punya empat keadaan yang wajib dirancang: memuat, kosong, gagal, siap. `DataStateBox` dipakai untuk tiga keadaan pertama.

| Komponen | Keadaan dan pemicu | Aturan lifecycle |
|---|---|---|
| `OrderApp` | memuat, meja hilang (404), gagal, tutup, menu, bayar, selesai | Satu sumber fase: `orderPhase(tracked)`. Pemulihan pesanan berjalan sekali per mount |
| `CategoryNav` (baru, W9) | aktif mengikuti `IntersectionObserver` pada heading | Observer dilepas saat unmount; tap memakai `scrollIntoView` dengan offset tinggi strip; scrollbar disembunyikan, fokus keyboard tetap terlihat |
| `ProductRow` | tersedia, pilih opsi, habis, paket (tanya staf) | Gambar memakai kotak rasio 1:1 berukuran tetap sebelum foto termuat |
| `ProductOptionsSheet` | foto, varian, opsi, catatan | Varian disembunyikan bila hanya satu; foto detail rasio 4:3 dengan placeholder |
| `CartSheet` | kosong, menghitung, gagal hitung, siap bayar, mengirim | Error checkout tetap di sheet; keranjang tidak dikosongkan saat gagal |
| `Sheet` | terbuka, tertutup lewat tombol, Escape, atau Back | `showModal` saat mount; satu entri history per sheet; fokus kembali ke pemicu saat tutup |
| `PaymentView` | menunggu, kedaluwarsa, lunas, batal, refund | Timer kedaluwarsa dibersihkan saat unmount atau fase berubah |
| `OrderProgress` (baru, W10) | menunggu bayar, diterima, disiapkan, siap | Tampil di layar lunas; hanya mengumumkan perubahan, bukan setiap render |
| `useOrderProgress` | polling, socket, berhenti | Berhenti total di status akhir; reconnect dengan backoff (T1) |
| `SiteFooter` | normal | Teks "Ditenagai Cassy", tautan legal, ganti bahasa; tidak tertutup bilah keranjang |

## Layout

| Lebar | Aturan |
|---|---|
| 360 px | Acuan utama. Gutter 20 px. Kartu QR, nominal, dan batas waktu terlihat tanpa gulir. Target sentuh minimal 44 px, tombol utama 56 px |
| 390 sampai 767 px | Sama dengan 360, kolom tunggal maksimal 512 px di tengah |
| 768 px ke atas | Kolom menu tetap maksimal 512 px; sheet tampil sebagai panel tengah dengan sudut membulat di semua sisi |
| Semua | Bilah keranjang `fixed` di bawah dengan `env(safe-area-inset-bottom)`; strip kategori `sticky` di atas; transisi 150 ms sesuai `DESIGN.md`, dimatikan bila `prefers-reduced-motion` |

Warna tenant hanya dipakai bila kontras AA terpenuhi terhadap teks di atasnya; bila gagal, kembali ke token Espresso & Crema. Halaman legal tidak memakai warna tenant maupun `azd-*`.

## SEO

- `metadataBase` diambil dari `NEXT_PUBLIC_APP_URL` tanpa fallback ke apex. Build gagal bila variabel tidak ada di produksi.
- `robots` per halaman: `/` dan `/legal*` diindeks; `/order/*`, `not-found`, dan `error` `noindex`. `robots.ts` tetap melarang `/order/` dan `/api/`.
- `sitemap.ts` berisi `/`, `/legal`, `/legal/mitra`.
- Judul dan deskripsi unik per halaman; `alternates.canonical` untuk tiap URL yang diindeks; `alternates.languages` untuk `id-ID` dan `en-US` pada halaman legal.
- Open Graph tanpa gambar Cassy di `/order/*`, supaya pratinjau tautan meja tidak menonjolkan Cassy di atas gerai.
- Header `Referrer-Policy: strict-origin-when-cross-origin` dipertahankan, sehingga token meja tidak bocor ke CDN atau tautan luar.

## Legal

### Backend

- Migrasi additive baru memperluas `chk_legal_documents_type` dengan `guest_privacy_notice` dan `guest_order_terms`, dan menambah konstanta di `internal/domain/governance.go`. Migrasi yang sudah merge tidak diubah.
- Seed versi `1.0.0-draft` untuk kedua jenis dalam `id-ID` dan `en-US`.
- Versi baru `terms_of_service` dengan judul memakai Cassy. Baris lama dibiarkan tidak aktif, tidak dihapus.
- `GET /public/legal` tidak berubah. Pemisahan tamu dan mitra dilakukan di web berdasarkan `doc_type`.

### Isi naskah tamu (draf, menunggu penasihat hukum)

| Dokumen | Pokok isi |
|---|---|
| `guest_privacy_notice` | Data yang dikumpulkan: nama panggilan, email struk opsional, cookie sesi meja. Tujuan: memanggil pesanan dan mengirim struk. Peran: kafe pengendali, AzureDevID pemroses. Masa simpan nama dan email ditetapkan Director (S7). Hak subjek data menurut UU 27/2022 dan kontak kafe serta `legal@abbdullaziz.online` |
| `guest_order_terms` | Harga dan pajak dari gerai; pembayaran QRIS lewat Midtrans atas nama gerai; pesanan diproses setelah lunas; pembatalan dan pengembalian dana ditangani kafe; batas tanggung jawab AzureDevID sebagai penyedia perangkat lunak |

## Endpoint

| Endpoint | Perubahan |
|---|---|
| `GET /public/legal` | Tidak berubah; data bertambah dua jenis dokumen |
| `GET /public/orders/track/:token` | Tidak berubah; frekuensi panggilan turun karena polling berhenti di status akhir |
| `POST /public/guest/orders/:id/payments` | Tidak berubah; T2 menambah test integrasi untuk QR lama |
| Lainnya | Tidak berubah |

## Urutan kerja

1. Backend: migrasi dan seed legal, test T2. Satu PR.
2. Web PR A: W7 dan T6 (CI bisa dipercaya lebih dulu).
3. Web PR B: R1, T1, T3, T4, P5, P6.
4. Web PR C: L1 dan SEO.
5. Web PR D: W9, W10, W12, U1.
6. Infra: T5 pada `nginx.demo.conf`.
7. W1 dan W6 setelah uji e-wallet manual oleh Director.

## Metrik

| Metrik | Target |
|---|---|
| Pesanan `pending_payment` yang kedaluwarsa tanpa dibuka lagi | Turun dibanding sebelum R1, diukur selama pilot |
| Lighthouse mobile `/order/*` | Performa 90, aksesibilitas 100, CLS di bawah 0,1 |
| Pelanggaran axe serious | 0 |
| Request track per pesanan selesai | Berhenti setelah status akhir |

## Risiko

| Risiko | Mitigasi |
|---|---|
| Naskah tamu belum ditinjau penasihat hukum saat pilot dimulai | Versi `-draft` tampil dengan tanggal; review masuk Tugas Director |
| WebKit di CI lebih lambat atau flaky | Jalankan hanya pada pull request, dengan retry 1 kali |
| Safari menghapus `localStorage` situs yang lama tidak dikunjungi | Dampak kecil karena sesi tamu pendek; cookie tetap sumber kebenaran |
| T2 menemukan celah bayar ganda | Rilis web ditahan sampai perbaikan backend merge |
