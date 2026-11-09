# Review WPA dan bedah PR 4 (2026-11-10)

Status: draf evaluasi, belum ada kode baru. Bukti: **[V]** dicek di kode atau demo, **[A]** asumsi atau estimasi.

## 1. Ringkasan

WPA sudah benar arahnya, tapi belum layak tayang. Empat hal menghalangi pilot, dan satu di antaranya soal uang:

1. Tenant tanpa key Midtrans sendiri kemungkinan menagih lewat key platform, karena `payment_initiate.go` hanya mengganti gateway bila ada kredensial tenant. Di produksi ini bertentangan dengan ADR-009, sebab dana tamu masuk akun AzureDevID. [V untuk alur kode, belum diuji end to end]
2. Keempat dokumen legal di demo hanya 124 sampai 215 karakter, dan isi kebijakan privasi cuma satu kalimat teknis. [V]
3. Pesanan split yang sebagian lunas tidak punya layar penyelesaian, karena kasir Android belum ada. [V]
4. Demo belum punya kredensial Midtrans tenant, jadi `central-cafe` belum teruji sebagai BYOK. [V]

Rekomendasi stack: tidak perlu Zustand, GSAP, atau Sonner sekarang.

## 2. Review kreatif

Kekuatan yang dipertahankan: satu tugas per layar, brand gerai sebagai aksen, teks tombol spesifik.

| Temuan | Prioritas | Tindakan |
|---|---|---|
| Menu tanpa foto terlihat seperti formulir | Tinggi | Foto 1:1 di tiap kartu, seed foto |
| Tidak ada pencarian, menu panjang melelahkan | Tinggi | Pencarian lokal di atas kategori |
| Kategori hanya memfilter, posisi hilang saat ganti | Sedang | Satu daftar panjang dengan heading, strip kategori melompat ke bagian |
| Header hanya teks | Sedang | Banner gerai bila ada, tinggi tetap agar tidak bergeser |
| Halaman legal masih bergaya konsol gelap dan warna AzureDevID | Sedang | Halaman baca biasa dengan token dasar |
| Bar keranjang menutupi footer legal | Sedang | Padding bawah sudah ada, perlu diuji di layar kecil |
| Status bayar hanya teks | Sedang | Langkah jelas: menunggu, diterima, disiapkan, siap |
| `Plus Jakarta Sans` memakai 3 bobot | Rendah | Cukup 400 dan 700 |
| Keyboard penuh dan zoom 200% belum diuji | Tinggi | Masuk gerbang rilis |

## 3. Sistem visual

**Token tambahan.** Warna gerai tetap satu aksen. Yang perlu distandarkan:

| Kelompok | Nilai |
|---|---|
| Spasi | 4, 8, 12, 16, 24, 32, 48 |
| Radius | Tombol dan input 8, kartu 12, sheet 16 |
| Tipe | 12, 14, 16, 20, 28, tinggi baris 1,5 untuk isi |
| Elevasi | Hanya sheet dan bar keranjang |
| Gerak | 120ms umpan balik, 200ms sheet, nol saat `prefers-reduced-motion` |
| Sentuh | Minimal 44, tombol bayar 56 |

**Elemen UI yang dirancang:** kartu menu, strip kategori, kolom pencarian, sheet opsi, sheet keranjang, bar keranjang, pemilih metode bayar, kartu QRIS, garis status pesanan, baris bagian patungan, banner koneksi.

**Layout.** Satu kolom maksimal 512px. Urutan menu: banner, nama gerai dan meja, pengumuman, pencarian, strip kategori lengket, daftar per kategori, bar keranjang.

**Aset.**

| Aset | Rasio dan ukuran | Format | Tampil kapan |
|---|---|---|---|
| Foto produk | 1:1, 800px, kartu 96px | WebP | Lazy, kecuali 4 pertama |
| Foto detail | 4:3, 1200px | WebP | Saat sheet dibuka |
| Banner gerai | 8:3, 1280px | WebP | Prioritas tinggi di header |
| Logo gerai | 1:1, 256px | WebP atau SVG | Header, kolom CMS belum ada |
| Addon | Tanpa foto | | Baris teks |
| Pengganti | Blok warna netral dengan rasio sama | CSS | Saat foto belum ada atau gagal |

Ukuran selalu dideklarasikan agar tidak ada pergeseran layout. Foto demo mencatat sumber dan lisensi di manifest.

## 4. Stack

| Pustaka | Keputusan | Alasan |
|---|---|---|
| Zustand 5 | Tidak | Keranjang satu layar sudah cukup dengan `useReducer` dan `sessionStorage`. Dokumentasi Zustand sendiri melarang store global di Next.js, jadi tetap butuh provider |
| GSAP 3 | Tidak | Kebutuhan hanya transisi sheet dan umpan balik, cukup CSS. Akan menambah JS di halaman yang dibuka lewat data seluler. Pertimbangkan lagi bila ada animasi berurutan yang benar-benar dibutuhkan |
| Sonner | Tidak sekarang | Status bayar dan error harus tetap terlihat di tempat, bukan hilang setelah 4 detik. Bila butuh, satu wilayah `aria-live` sudah cukup |
| next-intl | Tidak | Dua bahasa tanpa rute per bahasa cukup dengan kamus yang ada. PRD perlu diralat |
| Next.js 15 | Tetap | Stabil di 15.5.29. Komponen `use client` terlalu luas, tapi data menu perlu sesi tamu |

Batas bundle dan LCP belum diukur.

## 5. Optimasi fitur

| Fitur | Usulan |
|---|---|
| Pencarian | Filter lokal nama dan deskripsi, tanpa beda besar kecil huruf, hasil kosong menawarkan hapus kata kunci |
| Kategori | Lompat ke bagian, kategori aktif mengikuti posisi gulir |
| Kartu menu | Foto, nama, deskripsi dua baris, harga, satu tombol. Label habis menggantikan tombol |
| Detail | Sheet dengan foto, opsi, catatan, jumlah. Total baris dari server setelah masuk keranjang |
| Drawer | `<dialog>` sudah benar. Tambah tarik untuk menutup hanya bila diminta |
| Metode bayar | Tampilkan hanya yang aktif untuk gerai. Saat ini tamu hanya QRIS [V] |
| Status bayar | Menunggu, kedaluwarsa, gagal, diterima, sebagian (split), dan QRIS dibuat ulang |

## 6. Checklist endpoint tamu

| Endpoint | Backend | Dipakai WPA |
|---|---|---|
| `GET /public/qr/:token` | Ada | Ya |
| `POST /public/qr/:token/session` | Ada | Ya |
| `GET /public/stores/:slug/catalog` | Ada | Ya |
| `GET /public/stores/:slug/cms` | Ada | Ya |
| `POST /public/stores/:slug/orders/quote` | Ada | Ya |
| `POST /public/guest/orders` | Ada | Ya |
| `POST /public/guest/orders/:id/payments` | Ada, QRIS saja | Ya |
| `POST /public/guest/orders/:id/receipt/send` | Ada | Ya |
| `POST /public/guest/ws-ticket`, `GET /ws` | Ada | Ya |
| `GET /public/orders/track/:token` | Ada | Ya |
| `GET /public/guest/orders` | Ada | Belum, riwayat tamu |
| `GET /public/receipts/:token` | Ada | Belum, tidak ada halaman struk |
| `GET /public/legal` | Ada | Ya |
| Rute split tamu | **Belum** | Belum |

Sisa rute `plans`, `onboarding`, dan `queue` tidak dipakai WPA dan menjadi milik Android atau layar TV.

## 7. Legal dan persetujuan

| Item | Kondisi | Tindakan |
|---|---|---|
| Kebijakan privasi, S&K, refund, DPA | Hanya kalimat singkat [V] | Naskah lengkap, perlu ahli hukum |
| Banner cookie | Tidak ada, cookie hanya sesi yang diperlukan dan pilihan bahasa [V] | Tidak perlu banner selama tanpa analitik. Konfirmasi ke ahli hukum |
| Pemberitahuan data tamu | Nama dan email dikirim tanpa pemberitahuan di tempat [V] | Kalimat tujuan dan tautan privasi di dekat field |
| Klaim enkripsi di teks legal | Terlalu teknis dan menjanjikan | Tulis ulang |
| Identitas penyelenggara | Usaha perorangan | Konsisten di semua dokumen |
| Peran data | AzureDevID sebagai prosesor untuk gerai [A] | Tentukan di DPA dan PP 33/2026 |

## 8. Sequence split dan pay-first

```text
Pemesan     WPA            API                 Midtrans       Teman
  | buat pesanan pay_first  |                     |             |
  | bagi tagihan ---------->| 1 split full batal  |             |
  |                         | N split + token     |             |
  |<-- tautan per bagian ---|                     |             |
  | bagikan tautan -------------------------------------------->|
  |                         |<---- buka tautan ------------------|
  |                         |-- QRIS bagian ----->|             |
  |                         |<-- webhook lunas ---|             |
  |                         | partially_paid, stop kedaluwarsa  |
  |                         | bagian terakhir lunas: tiket dapur|
```

Pay-first tetap berlaku karena dapur baru menerima tiket setelah bagian terakhir lunas. Yang berubah hanya uang datang bertahap.

| Sudut pandang | Masalah | Pencegahan |
|---|---|---|
| Tamu | Teman tidak membayar, makanan tertahan | Daftar status bagian, pemesan bisa melunasi |
| Teman | Link terasa mencurigakan | Nama gerai, nominal, tanpa data lain |
| Staf | Pesanan menggantung, dana masuk | Layar penyelesaian Android |
| Pemilik gerai | Stok tertahan | Batas 15 menit sebelum ada dana masuk |
| Kesalahan manusia | Bagi ulang setelah ada yang bayar | Ditolak server setelah pembayaran pertama |
| Kesalahan manusia | Bayar dua kali | Satu pembayaran aktif per bagian |

## 9. Ancaman utama

| Ancaman | Dampak | Mitigasi |
|---|---|---|
| Key platform dipakai tenant | Dana salah akun | Gagal tertutup di produksi |
| Token tautan bocor | Orang lain melihat nominal | Tanpa isi pesanan, kedaluwarsa, hash di database |
| IDOR saat membagi | Mengacak pesanan orang | Pemilik sesi dicek dalam transaksi |
| Permintaan split bersamaan | Split ganda | Kunci baris pesanan |
| Webhook terlambat setelah batal | Dana tanpa pesanan | `payment.refund_required`, sudah ada |
| Amplifikasi tautan | Beban QRIS | Rate limit per IP dan per token |
| Warna CMS berbahaya | Injeksi gaya | Sudah divalidasi |

## 10. Jaringan dan Midtrans

- `HEAD /health/ready` dan `HEAD /api/v1/health` menjawab 404, sedangkan `GET` 200. Monitor yang memakai HEAD akan melapor gangguan palsu. [V]
- Webhook tenant lewat zona `/api/` umum, tidak punya blok khusus seperti webhook lama. [V]
- CSP dan Permissions-Policy belum ada. [V]
- Zona publik 30 permintaan per menit per IP terlalu sempit untuk banyak tamu di Wi-Fi yang sama. [V]
- Respons `/` memuat `s-maxage=31536000`, sementara Cloudflare melaporkan DYNAMIC. Aman sekarang, tapi berisiko bila aturan cache berubah. [V]
- Halaman order `no-store`. [V]

## 11. SEO

`noindex` untuk WPA sudah benar karena tautan meja privat. Hanya `/legal` yang boleh terindeks, dan itu butuh naskah lengkap dulu. SEO produk Cassy menunggu landing page yang sedang ditunda.

## 12. Yang sudah dikerjakan

- [x] Demo API v0.12.1, migrasi 000056
- [x] Web tayang dari main client `1dc89b9`
- [x] Dokumen re-alignment dan keputusan AL1 sampai AL11
- [x] Retry koneksi migrate di main, belum dirilis
- [ ] WPA khusus tamu, client #27, menunggu merge
- [ ] DESIGN.md, client #28, menunggu merge
- [ ] UI tamu, client #29, menunggu merge
- [ ] ADR-010 dan PRD, backend #121, menunggu merge
- [ ] Split tautan
- [ ] Seed demo dan foto
- [ ] CSP dan rate limit
- [ ] Naskah legal
- [ ] Layar operasional Android
- [ ] E2E terhadap demo

## 13. Backlog dan pertukaran

| Item | Dampak bila ditunda |
|---|---|
| Gagal tertutup untuk key platform | Dana salah akun di produksi, wajib sebelum pilot |
| Naskah legal | Tidak bisa pilot dan menghambat review Midtrans |
| Kasir Android penyelesai split | Split tidak boleh aktif |
| Rate limit | Tamu kena 429 saat ramai |
| Foto dan seed | Demo terlihat kosong |
| Halaman struk | Tamu hanya punya email |
| Tema gelap | Bisa ditunda |
