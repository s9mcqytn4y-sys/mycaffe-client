# Rilis Pertama Aplikasi Tenant Cassy (Android)

Status: disetujui arah, 2026-11-10. Rujukan: ADR-010 di repo backend dan `docs/WpaReview2026-11.md`. Semua operasional tenant berada di aplikasi ini. Web hanya melayani tamu QR.

## ADR: Android menjadi satu-satunya konsol tenant

**Keputusan.** Rilis pertama mencakup setup dan operasi inti saja. Laporan, stok, invoice B2B, stamp, kanal ojol, MikroTik, dan antrean TV ditunda walaupun API-nya sudah ada.

**Alasan bisnis.** Tenant baru bisa ditagih setelah satu kafe bisa berjalan tanpa bantuan kita. Paket Pro Rp 299.000 per bulan menjadi sasaran pertama. Model biaya, CAC, dan runway belum bisa dihitung karena belum ada data pilot.

**Alasan teknis.** Aplikasi sekarang sekitar 3.700 baris dengan POS, pairing, Midtrans, dan printer. Compose, Ktor, Koin, dan CameraX dipertahankan.

**Konsekuensi.** Sampai rilis ini ada, tenant demo disiapkan lewat seed dan API, dan split tamu tetap mati.

## PRD

**Tujuan.** Pemilik kafe bisa menyiapkan gerai dan melayani tamu QR dari satu tablet Android.

**Ukuran selesai.** Satu kafe demo berjalan dari login sampai pesanan QR selesai, tanpa memanggil API manual.

**Pengguna.** Pemilik (setup) dan kasir atau dapur (harian).

| Layar | Kebutuhan | API |
|---|---|---|
| Login dan pairing | Slug, email, PIN, refresh token aman | `/auth/staff/*`, `/mobile/bootstrap` |
| Pengaturan server | Alamat API dari konfigurasi, tidak hardcode | `BuildConfig` |
| Kredensial Midtrans | Sandbox dan produksi, status terverifikasi | `/store/payment-credentials` |
| Brand gerai | Nama, warna, banner untuk WPA | `/store/cms`, `/media/uploads` |
| Menu | Kategori, produk, varian, addon, sub-addon, harga, foto, habis | `/catalog/*` |
| Meja dan QR | Tambah, ubah, arsip, rotasi, cetak poster QR | `/pos/tables*` |
| KDS | Tiket per stasiun, ubah status | `/kds/tickets*`, `/pos/ws-ticket`, `/ws` |
| Kasir | Sudah ada, sambungkan ke API sebenarnya | `/pos/*` |
| Shift | Buka dan tutup, syarat pesanan QR | `/pos/shifts` |
| Penyelesaian split | Daftar bagian, terima tunai untuk sisa, tandai selesai | Rute split staf, belum dibangun |

**Di luar cakupan rilis ini:** laporan, stok, invoice B2B, stamp, ojol, MikroTik, antrean TV, iOS.

**Kasus tepi wajib:** token kedaluwarsa saat layar menyala, koneksi putus saat ubah status, dua perangkat mengubah tiket yang sama (409 `KDS_STALE_STATUS`), rotasi QR saat tamu sedang memesan, PIN salah sampai terkunci, kredensial sandbox dipakai di produksi.

## DESIGN

Mengikuti `DESIGN.md` di root monorepo: token Espresso & Crema, target sentuh 48px untuk kasir, status selalu berupa teks. Layout tablet landscape memakai rail navigasi kiri. Layar setup memakai satu tugas per layar. Dapur memakai font besar yang terbaca dari jarak 2 meter.

## Urutan PR

Setiap PR harus lolos `./gradlew test bundleDebug`, dan layar dengan API punya test repository.

1. Fondasi: `BuildConfig` per lingkungan, klien Ktor tunggal, sesi dan refresh satu-per-satu, hapus data contoh di `PosLandscapeScreen.kt` dan `PaymentChannel.kt`.
2. Navigasi konsol dan pengaturan server.
3. Login, pairing, bootstrap.
4. Menu, termasuk unggah foto.
5. Meja, rotasi QR, dan poster QR.
6. Kredensial Midtrans dan brand gerai.
7. Shift dan penyelesaian kasir.
8. KDS dengan WebSocket tiket.
9. Penyelesaian split, lalu flag split tamu boleh dinyalakan.
10. Uji end to end terhadap demo, rilis internal Google Play.

**Sebelum rilis:** daftarkan paket `com.azuredevid.cassy` di verifikasi developer Android. Versi alpha `security-crypto` dan `biometric` dievaluasi di PR 1.

## Pengambilan keputusan bagi agent

Jangan menambah layar di luar tabel PRD tanpa persetujuan Director. Jangan menyimpan token atau server key di log. Jangan menghitung harga, pajak, atau total di klien.
