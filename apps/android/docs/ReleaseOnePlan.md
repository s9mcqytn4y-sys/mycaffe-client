# Rilis Pertama Aplikasi Tenant Cassy (Android)

Status: disetujui arah, 2026-11-10. Diperbarui 2026-11-12 untuk PRD v4 (`docs/PrdCassyV4.md` di root monorepo): token baru, US1 sampai US4, dan aturan arsitektur. Rujukan: ADR-010 di repo backend dan `docs/WpaReview2026-11.md`. Semua operasional tenant berada di aplikasi ini. Web hanya melayani tamu QR.

## ADR: Android menjadi satu-satunya konsol tenant

**Keputusan.** Rilis pertama mencakup setup dan operasi inti saja. Laporan, stok, invoice B2B, stamp, kanal ojol, MikroTik, dan antrean TV ditunda walaupun API-nya sudah ada.

**Alasan bisnis.** Tenant baru bisa ditagih setelah satu kafe bisa berjalan tanpa bantuan kita. Paket Pro Rp 299.000 per bulan menjadi sasaran pertama. Model biaya, CAC, dan runway belum bisa dihitung karena belum ada data pilot.

**Alasan teknis.** Aplikasi sekarang sekitar 3.700 baris dengan POS, pairing, Midtrans, dan printer. Compose, Ktor, Koin, dan CameraX dipertahankan.

**Konsekuensi.** Sampai rilis ini ada, tenant demo disiapkan lewat seed dan API, dan split tamu tetap mati.

## PRD

**Tujuan.** Pemilik usaha F&B (restoran, kafe, bakery, food court, kios) bisa menyiapkan gerai dan melayani tamu QR dari satu tablet Android.

**Ukuran selesai.** Satu gerai demo berjalan dari login sampai pesanan QR selesai, tanpa memanggil API manual.

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
| Mode layanan (US4) | Owner memilih ambil sendiri atau antar ke meja | `PATCH /store/settings` `qr_service_mode` |
| KDS "sudah diantar" (US3) | Aksi menandai pesanan `served` untuk gerai antar ke meja | `/kds/tickets*` |

**Cerita dari PRD v3 yang wajib ikut.**
- US1: pairing lewat QR bertanda tangan dari tablet atau ponsel owner yang sudah login, bukan dari web backoffice. State terkunci saat QR terbaca, QR punya batas waktu.
- US2: pergantian kasir selalu lewat PIN 6 digit milik kasir. Sidik jari hanya membuka ulang sesi kasir yang sama setelah kunci otomatis 300 detik. Tiga PIN salah memberi backoff dan sisa percobaan.
- US3 dan US4: lihat tabel di atas.

**Di luar cakupan rilis ini:** laporan, stok, invoice B2B, stamp, ojol, MikroTik, antrean TV, iOS.

**Kasus tepi wajib:** token kedaluwarsa saat layar menyala, koneksi putus saat ubah status, dua perangkat mengubah tiket yang sama (409 `KDS_STALE_STATUS`), rotasi QR saat tamu sedang memesan, PIN salah sampai terkunci, kredensial sandbox dipakai di produksi.

## DESIGN

Mengikuti PRD v4: token pandan dan kunyit dari `packages/design-tokens`, Plus Jakarta Sans, ikon Material Symbols dari `androidx.compose.material.icons` (PRD v4 Q6 diperbarui 2026-11-13), target sentuh 48px untuk kasir, status selalu berupa teks. Layout tablet landscape memakai rail navigasi kiri. Layar setup memakai satu tugas per layar. Dapur memakai font besar yang terbaca dari jarak 2 meter.

## Arsitektur (PRD v4 Q11, prinsip SOLID)

- Satu modul `:app`, paket per fitur (`feature/menu`, `feature/kds`, ...). Di tiap fitur: `ui` (Compose tanpa state sendiri) ke `ViewModel` (state satu arah) ke antarmuka `domain` ke implementasi `data`.
- Koin benar-benar dijalankan dengan modul per lapisan; layar dan ViewModel hanya bergantung pada antarmuka (DIP).
- Satu `HttpClient` Ktor dari Koin, dengan auth dan refresh token satu-per-satu di satu tempat (SRP). Retrofit tidak ditambahkan.
- Metode bayar dan printer di balik antarmuka, sehingga metode baru tidak mengubah layar kasir (OCP).
- Preferences DataStore untuk pengaturan (alamat server, printer, bahasa). Token disimpan di DataStore terenkripsi Tink dengan kunci Android Keystore, menggantikan `security-crypto` alpha.
- Paging 3 tidak dipakai di rilis ini: daftar kecil dan KDS real-time. Ditambahkan saat riwayat pesanan atau laporan masuk.
- Paket `designsystem/` (token, tipografi, ikon) disiapkan agar bisa pindah ke modul sendiri saat ada aplikasi kedua atau build melambat.

## Urutan PR

Setiap PR harus lolos `./gradlew test bundleDebug`, dan layar dengan API punya test repository.

1. Fondasi: `BuildConfig` per lingkungan, Koin dijalankan, klien Ktor tunggal, sesi dan refresh satu-per-satu, DataStore dan Tink, `designsystem/` dengan token baru, hapus data contoh di `PosLandscapeScreen.kt` dan `PaymentChannel.kt`.
2. Navigasi konsol dan pengaturan server.
3. Login, pairing, bootstrap.
4. Menu, termasuk unggah foto.
5. Meja, rotasi QR, dan poster QR.
6. Kredensial Midtrans, brand gerai, dan mode layanan (US4).
7. Shift dan penyelesaian kasir.
8. KDS dengan WebSocket tiket dan aksi sudah diantar (US3).
9. Penyelesaian split, lalu flag split tamu boleh dinyalakan.
10. Uji end to end terhadap demo, rilis internal Google Play.

**Sebelum rilis:** daftarkan paket `com.azuredevid.cassy` di verifikasi developer Android. `security-crypto` diganti Tink di PR 1; versi alpha `biometric` dievaluasi di PR 1.

## Pengambilan keputusan bagi agent

Jangan menambah layar di luar tabel PRD tanpa persetujuan Director. Jangan menyimpan token atau server key di log. Jangan menghitung harga, pajak, atau total di klien.
