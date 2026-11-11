# PRD v3: WPA Lifecycle Pesanan Meja

Pemilik: Abdul Aziz (Director). Status: draf untuk disetujui. Tanggal: 11 November 2026.
Lanjutan dari `docs/PrdWpaTenantReady.md` (v2). Fokus: lifecycle pesanan meja dari pindai sampai pesanan sampai ke tamu, ditambah keputusan sesi grill 11 November.

## Ringkasan

Sebagian besar PRD v2 sudah live di demo: keandalan host (Fase A), QRIS/GoPay/ShopeePay (Fase B), menu kartu berfoto dan status reaktif (Fase C dan D), dan menu demo lengkap (C11). Rilis ini menutup sisanya dan menambah apa yang dibutuhkan tenant sungguhan: mode layanan per gerai, bunyi saat pesanan siap, penanganan error dan maintenance yang lengkap, keranjang yang eksplisit soal varian dan add-on, serta naskah tamu yang sesuai metode bayar baru. Dua keputusan Android ikut direvisi karena berdampak ke keamanan kas.

## Status PRD v2

| Fase | Status | Bukti |
|---|---|---|
| A Keandalan | Selesai dari sisi kode dan host demo | Monitor uptime tiap 5 menit, `.wslconfig`, backup per jam dan sebelum deploy, uji restore PASS |
| B Midtrans | B1 sampai B5 live; B6, B7 terbuka | backend #128, web #40 |
| C UI v2 | C1, C2, C5 sampai C9, C11 live; C3, C4 sebagian | web #42, #43; menu demo 28 produk |
| D Notifikasi | D1 sampai D4 live | web #43 |
| E Email, favicon | Terbuka | |
| F Keamanan, utang teknis | Terbuka | |

## Keputusan sesi 11 November

| # | Keputusan | Konsekuensi |
|---|---|---|
| Q1 | Tenant Express hanya dinonaktifkan di demo (soft: `is_active` false, `deleted_at` terisi) | Seed test tetap dua tenant untuk uji isolasi RLS |
| Q2 | PIN tetap identitas kasir; biometrik hanya membuka ulang sesi kasir yang sama | User story Android 2 direvisi (lihat bawah) |
| Q3 | Mode layanan per gerai, `qr_service_mode` = `pickup` (bawaan) atau `table` | Teks status, toast, dan bel menyesuaikan; KDS butuh aksi "sudah diantar" |
| Q4 | `/` (arahan pindai QR) dan `/legal` (naskah tamu) dipertahankan | UU PDP dan review Midtrans butuh halaman kebijakan yang bisa diakses |
| Q5 | Pilot di VPS lokal Jakarta, stack sama dengan demo | Pemilihan penyedia jadi tugas Director |
| Q6 | Email tetap Gmail SMTP | App password yang tertempel di chat wajib dirotasi |
| Q7 | Bunyi bel disintesis Web Audio, tanpa file suara | Tanpa lisensi aset; varian nada lewat parameter |

## Cerita pengguna

### Tamu (WPA)

| ID | Sebagai tamu, saya ingin | Kriteria terima |
|---|---|---|
| T1 | Tahu persis apa yang saya pesan sebelum bayar | Keranjang menampilkan varian dan setiap add-on serta sub-add-on per baris, dengan harga dari quote |
| T2 | Tidak memesan dua kali tanpa sengaja | Konfigurasi sama digabung jumlahnya; konfigurasi berbeda jadi baris terpisah yang terlihat beda; bila ada pesanan berjalan, halaman menu menawarkan "Lanjutkan pembayaran" sebelum checkout baru |
| T3 | Tahu kapan harus mengambil pesanan, atau bahwa pesanan sedang diantar | Teks langkah, toast, getar, dan bel mengikuti mode gerai |
| T4 | Mendapat pesan yang jelas saat sesuatu gagal | Setiap kode HTTP dari API punya pesan dan aksi berikutnya (lihat matriks) |
| T5 | Menengok pesanan lagi tanpa disepam | Tautan pelacakan di struk email; satu email per permintaan; tanpa notifikasi pemasaran |
| T6 | Tahu data apa yang disimpan | Naskah privasi menyebut cookie sesi meja dan penyimpanan lokal; tanpa banner cookie karena tidak ada pelacakan |

### Tenant (Android, tim mobile)

| ID | Cerita | Revisi |
|---|---|---|
| US1 | Pairing tablet lewat QR bertanda tangan saat boot pertama | QR pairing tidak lagi dibuat di "Web Backoffice" (ADR-010: web hanya untuk tamu). Sumber QR: tablet atau ponsel owner yang sudah login, atau perintah operator. Kunci state saat QR terbaca dan batas waktu QR tetap |
| US2 | Serah terima shift dan buka kunci cepat | Pergantian kasir selalu lewat PIN 6 digit milik kasir itu. Sidik jari hanya membuka ulang sesi kasir yang sama setelah kunci otomatis 300 detik, karena BiometricPrompt tidak tahu sidik jari milik siapa. Tiga gagal PIN memberi backoff dan sisa percobaan |
| US3 | KDS menandai pesanan "sudah diantar" | Dibutuhkan untuk mode `table`; tanpa aksi ini tamu tidak pernah melihat langkah terakhir |
| US4 | Owner mengubah mode layanan gerai | Pengaturan `qr_service_mode` di layar pengaturan gerai |

## Lifecycle pesanan

| Status backend | Mode `pickup` (tamu melihat) | Mode `table` (tamu melihat) |
|---|---|---|
| `pending_payment` | Menunggu pembayaran (QR atau tombol aplikasi) | Sama |
| `queued` | Pesanan diterima | Pesanan diterima |
| `preparing` | Sedang disiapkan | Sedang disiapkan |
| `ready` | Siap diambil di bar; toast, getar, bel dua nada | Sedang diantar ke meja Anda; tanpa bel |
| `served`, `completed` | Selesai | Sudah diantar; toast "Selamat menikmati"; bel satu nada lembut |
| `cancelled` | Dibatalkan; arahan ke staf bila uang sudah masuk | Sama |

Bel hanya berbunyi setelah tamu pernah mengetuk halaman (aturan autoplay), tidak saat tab tersembunyi, dan bisa dibisukan; pilihan bisu disimpan di perangkat.

## Matriks error dan halaman

| Kondisi | Respons | Yang dilihat tamu |
|---|---|---|
| Rute tidak dikenal, termasuk tebakan seperti `/kds`, `/queue`, `/admin` | 404 dari Next | "Halaman tidak ditemukan", ajakan pindai ulang QR; tanpa membedakan rute "terlarang" dan tidak ada, agar tidak membocorkan struktur |
| Token QR tidak dikenal atau meja dinonaktifkan | API 404 | "Meja tidak ditemukan, minta bantuan staf" |
| Sesi tamu habis | API 401 `GUEST_SESSION_EXPIRED` | Muat ulang sesi otomatis, keranjang utuh |
| Harga berubah, produk habis | API 409 | Pesan spesifik, quote dihitung ulang |
| Metode bayar dimatikan gerai | API 400 `INVALID_PAYMENT` | "Metode ini sedang tidak tersedia", tawarkan metode lain |
| Terlalu banyak percobaan | API 429 | "Terlalu banyak percobaan, tunggu sebentar" |
| Server error | API 5xx | Pesan umum dan tombol coba lagi; tanpa detail teknis |
| Maintenance terjadwal | nginx 503 + `Retry-After` dengan halaman statis | "Sedang ada pemeliharaan singkat; pesanan yang sudah dibayar tetap aman"; halaman tidak memanggil API |
| Crash render | `error.tsx` | Pesan umum dan tombol coba lagi |
| Akses langsung ke `/api/*` dari browser | JSON error apa adanya | Tidak ditampilkan sebagai halaman |
| `/ws` tanpa tiket | 401 | Tidak terlihat tamu; WPA jatuh ke polling |

## Backlog rilis ini

| ID | Kebutuhan | Repo | Kriteria terima |
|---|---|---|---|
| N1 | Mode layanan per gerai: migrasi additive `qr_service_mode`, ikut di resolve QR publik, WPA mengikuti mode | backend, web | Test integrasi nilai bawaan; E2E kedua mode |
| N2 | Bel Web Audio dua varian dan tombol bisu | web | Unit test jadwal nada; E2E bel tidak dipicu saat tab tersembunyi |
| N3 | Halaman maintenance nginx 503 dan saklar `maintenance.on` di host | backend (deploy) | `curl` 503 dengan `Retry-After` saat saklar aktif; webhook Midtrans tetap diteruskan |
| N4 | Keranjang eksplisit: varian dan semua add-on per baris, baris berbeda untuk konfigurasi berbeda | web | E2E dua konfigurasi produk sama |
| N5 | Footer selalu di dasar layar pada halaman pendek | web | Screenshot 390 x 844 layar bayar |
| N6 | Naskah tamu v1.1: QRIS, GoPay, ShopeePay, mode layanan, penyimpanan lokal; bahasa jelas | backend (migrasi data) | Versi baru aktif, versi lama nonaktif, hash cocok isi |
| N7 | Rapikan nama grup add-on demo (contoh "Plant Milk Preparation (Sub-Addon)") | data demo | Tanpa label teknis di layar tamu |
| N8 | Tautan pelacakan di struk email (E1), template berbrand tenant | backend | Uji kirim ke kotak masuk |
| N9 | Favicon dan ikon PWA dicek terhadap aset vault (E2) | web | Lighthouse tanpa peringatan ikon |
| N10 | Penyetelan animasi: satu durasi umpan balik 150 ms, hasil bayar 300 ms, semua di bawah `motion-safe` | web | Uji reduced motion |
| N11 | Review keamanan terarah (F1) dan ledger utang teknis (F2) | kedua repo | Dokumen dan perbaikan temuan tinggi |
| N12 | Checklist aktivasi Midtrans per tenant (B6) | docs | Dibagikan saat onboarding |
| N13 | Desain UI bagi tagihan (W13), tetap di belakang flag sampai Android bisa melunasi sisa bagian | web | Mockup dan E2E di belakang flag |

## Urutan kerja

1. N1 backend, lalu N1 web bersama N2 dan N10 (satu area: layar status).
2. N4 dan N5 (keranjang dan tata letak).
3. N3 halaman maintenance.
4. N6 dan N7 (naskah dan data), N8 dan N9 (email dan ikon).
5. N11 review keamanan dan ledger.
6. N12 dan N13.

## Infrastruktur

- Demo tetap di laptop dengan mitigasi yang sudah terpasang. Bersihkan container `buildx_buildkit_builder` yang tidak dipakai, dan pertimbangkan melonggarkan interval health check demo agar beban VM turun saat laptop sibuk.
- Pilot: VPS lokal Jakarta (Q5), stack Docker Compose dan cloudflared yang sama, backup R2 per jam.

## Tugas Director

| Tugas | Memblokir |
|---|---|
| Rotasi kredensial yang tertempel di chat: Midtrans production server key, app password Gmail, token R2, DSN Sentry | Go-live |
| Pilih penyedia VPS Jakarta dan setujui biaya | Pilot uang sungguhan |
| Simpan passphrase backup di vault | Pemulihan |
| Review naskah tamu v1.1 dengan penasihat hukum, termasuk lokasi data | Go-live |
| Luruskan akun Midtrans `G357841497` (situs Kredivo, form koreksi) | Aktivasi metode |
| Uji W1 dan W6 di minimal 3 e-wallet | W1, W6 |
| Task WSL saat log on dan setelan tidur baterai | Demo stabil |

## Bukan cakupan

Web push, halaman toko publik, pustaka animasi atau toast, file suara, banner cookie, metode bayar selain tiga yang dipilih.

## Risiko

| Risiko | Mitigasi |
|---|---|
| Mode `table` dipakai sebelum KDS punya aksi "sudah diantar" | Mode `table` hanya bisa dipilih setelah US3 rilis |
| Bel tidak berbunyi di iPhone mode senyap | Toast dan judul tab tetap menjadi sinyal utama |
| Laptop demo sibuk membuat demo lambat atau 502 | Tutup aplikasi berat saat demo; pilot di VPS |
| Biometrik di Android tetap dipakai sebagai identitas | Revisi US2 jadi syarat review PR Android |
| Data tamu di luar kendali tenant setelah Gmail dipakai | Struk hanya berisi ringkasan pesanan; email lengkap terhapus 90 hari |
