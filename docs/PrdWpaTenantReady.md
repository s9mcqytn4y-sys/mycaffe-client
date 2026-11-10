# PRD: WPA Siap Dipakai Tenant

Pemilik: Abdul Aziz (Director). Status: draf untuk disetujui. Tanggal: 10 November 2026.
Lanjutan dari `docs/PrdWpaPilotReady.md`. Langkah 1 sampai 5 PRD itu sudah merge (backend #125, client #35 sampai #38). Web di demo belum di-deploy sejak `sha-c8b9af1`.

## Ringkasan

Kode pembayaran WPA sudah benar: webhook memverifikasi signature, membalas 503 untuk gagal sementara sehingga Midtrans mencoba ulang, aman terhadap notifikasi ganda dan tidak berurutan, dan rekonsiliasi lewat Get Status menutup sisanya. Yang belum siap adalah tiga hal di luar kode inti: host yang bisa mati, akun Midtrans yang masih ditahan review, dan tampilan yang masih terasa seperti daftar teks. Rilis ini mengerjakannya berurutan: keandalan, kesiapan Midtrans dan metode bayar, UI v2, notifikasi, email, lalu tugas Director.

## Insiden yang mendasari Fase A

Notifikasi `expire` untuk payment `537d920c` (pesanan uji Rp 23.100) gagal dengan HTTP 530 dan Midtrans berhenti mencoba setelah sekitar 2 jam.

| Waktu (UTC) | Kejadian |
|---|---|
| 00:35 | QRIS dibuat, kedaluwarsa 00:50 |
| 00:49:02 | Tunnel Cloudflare berhenti karena host (WSL di laptop) mati |
| 00:50:38 | Midtrans mengirim `expire`, Cloudflare membalas 530 |
| 02:53 | Midtrans mengirim email "dead notification" |
| 11:15 | Host menyala lagi; order tercatat `cancelled/expired`, payment `expired` |

Tidak ada uang hilang dan status akhir benar, tetapi pesanan menggantung sekitar 10 jam. Pada pesanan yang dibayar, dapur baru menerima tiket setelah host hidup.

## Keputusan yang dikunci

| # | Keputusan | Konsekuensi |
|---|---|---|
| Q1 | Host tetap laptop untuk demo dan uji internal | Mitigasi wajib di A1 sampai A3; VPS menjadi gerbang sebelum transaksi uang sungguhan di kafe pilot |
| Q2 | Metode bayar pilot: QRIS, GoPay, ShopeePay | Endpoint tamu menerima metode; tiap tenant mengaktifkan ketiganya di akun Midtrans sendiri |
| Q3 | Tidak ada halaman toko publik | Tiap tenant menyediakan URL sendiri untuk review Midtrans; dicatat sebagai syarat onboarding |
| Q4 | Notifikasi pesanan siap hanya di halaman | Toast, getar (Android), judul tab, animasi garis status; tanpa web push |
| Q5 | Kartu menu grid 2 kolom, foto 1:1 dominan | 3 kolom mulai 768 px; inisial untuk produk tanpa foto |
| Q6 | Demo memakai menu asli kafe dari Director | Butuh izin pemilik untuk nama dan foto; menunggu kiriman data |

## Fase A: keandalan

| ID | Kebutuhan | Kriteria terima |
|---|---|---|
| A1 | Laptop tidak tidur saat dicolok; WSL, Docker, dan stack demo menyala otomatis setelah Windows boot | Restart Windows, demo kembali melayani `/health/ready` tanpa tindakan manual |
| A2 | Monitor uptime eksternal ke `https://demo.abbdullaziz.online/health/ready`, alert ke email Director | Mematikan tunnel memicu email dalam 5 menit |
| A3 | Alert untuk payment gateway `pending` lebih dari 30 menit melewati `expires_at`; rekonsiliasi berjalan saat API menyala | Test integrasi; log menunjukkan rekonsiliasi pada start |
| A4 | Backup sebelum deploy aktif di demo (S2), uji restore sekali | Log deploy tanpa `DEPLOY_SKIP_BACKUP`; restore ke database sementara berhasil |
| A5 | Header keamanan demo disamakan dengan prod (T5): CSP, HSTS, Permissions-Policy | `curl -I` menunjukkan ketiganya |
| A6 | Gerbang VPS sebelum uang sungguhan: compose dan cloudflared yang sama di VPS Jakarta | Checklist go-live ditandatangani Director |

## Fase B: Midtrans dan metode bayar

| ID | Kebutuhan | Kriteria terima |
|---|---|---|
| B1 | `POST /public/guest/orders/:id/payments` menerima `method` (`qris`, `gopay`, `shopeepay`), divalidasi terhadap metode yang diaktifkan gerai | Metode yang tidak aktif ditolak 422; test integrasi per metode |
| B2 | Callback URL dibangun di server dari base URL WPA dan token meja, tidak pernah dari input klien | Test: field callback dari klien diabaikan |
| B3 | Deeplink hanya diteruskan bila berasal dari respons Midtrans dan skemanya ada di allowlist yang dicatat dari respons sandbox GoPay dan ShopeePay | Unit test skema; allowlist berisi skema yang benar-benar teramati |
| B4 | UI pilih metode di keranjang; GoPay dan ShopeePay membuka aplikasi, kembali ke WPA langsung memeriksa status | E2E dengan mock; uji manual sandbox per metode |
| B5 | Kasus tepi: kembali tanpa bayar, ganti metode (percobaan lama dibatalkan, settlement terlambat jadi `refund_required`), aplikasi tidak terpasang | E2E per kasus |
| B6 | Checklist aktivasi Midtrans per tenant: URL website, foto selfie, payment flow, status per metode | Dokumen onboarding tenant; metode baru aktif di Cassy setelah disetujui Midtrans |
| B7 | Luruskan akun AzureDevID `G357841497`: website di pengajuan Kredivo tercatat `geaniproduction.com/fotoflow`, form koreksi 9 November belum dikirim | Tugas Director; status akun aktif |

## Fase C: UI v2

Arah: fotografis, brand tenant dominan, Espresso & Crema sebagai cadangan. Dial dari `DESIGN.md` tetap: ENERGY 2, RHYTHM 1, MOTION 1, dengan pengecualian dua animasi hasil bayar di C7.

| ID | Kebutuhan | Kriteria terima |
|---|---|---|
| C1 | Header: logo tenant bila ada, nama gerai, meja, status buka, banner pengumuman; ringkas saat digulir | Screenshot 360 dan 1280 px |
| C2 | Kartu menu grid 2 kolom (3 kolom mulai 768 px): foto 1:1, nama maksimal 2 baris, harga tabular, tombol tambah di sudut foto; inisial untuk produk tanpa foto; deskripsi pindah ke sheet | E2E tambah dari kartu; CLS di bawah 0,1 |
| C3 | Sheet produk: foto 4:3, varian dan opsi sebagai pilihan bersegmen, jumlah, catatan | axe 0 serious |
| C4 | Keranjang: ikon tambah dan kurang, hapus baris, ringkasan total menonjol, pilihan metode bayar (B4) | E2E |
| C5 | Garis status reaktif: waktu per langkah, transisi saat status berubah, toast saat siap | E2E dengan perubahan status mock |
| C6 | Footer: satu baris ringkas "Ditenagai Cassy", tautan legal, ganti bahasa, tidak tertutup bilah keranjang | Screenshot |
| C7 | Animasi bayar sukses dan gagal dengan CSS `@keyframes`, mati bila `prefers-reduced-motion` | Uji dengan emulasi reduced motion |
| C8 | Set ikon tunggal (Phosphor regular, SVG inline lewat Iconify, tanpa dependensi runtime) untuk keranjang, tambah, kurang, centang, silang, jam, lonceng | Tidak ada ikon dari set lain |
| C9 | Token: hapus `azd-*` dan `--font-sans` Inter yang tidak dimuat dari layar tamu, `focus-ring` memakai warna brand dengan kontras AA, skala tipografi 12/14/16/20/24/32, radius 8 dan 16, satu durasi gerak 150 ms | `tokens:verify` hijau; grep tanpa `azd-` di `apps/web/src` |
| C10 | Tombol dan form: tinggi 44 px minimum, 56 px untuk aksi utama, state disabled, loading, dan error yang konsisten | axe dan review visual |
| C11 | Data demo dari menu asli kafe (Q6): bahasa Indonesia, foto yang mengisi minimal setengah bingkai, lewat API katalog; produk lama diarsipkan | Menunggu data Director |

Tidak ada pustaka komponen atau animasi baru. Zustand, GSAP, Motion, Magic UI, dan Sonner tetap tidak dipakai; toast dibuat dari komponen sendiri dengan `role="status"`.

## Fase D: notifikasi dan jaringan

| ID | Kebutuhan | Kriteria terima |
|---|---|---|
| D1 | Toast komponen sendiri untuk pesanan siap, pembayaran diterima, dan gagal | `aria-live` terverifikasi |
| D2 | `navigator.vibrate` saat siap (Android); judul tab berganti saat tab tidak aktif | Uji manual Android |
| D3 | Banner koneksi terputus dari `online`/`offline`; quote dan checkout mencoba ulang setelah online | E2E dengan emulasi offline |
| D4 | Kembali dari aplikasi e-wallet (`visibilitychange`, `pageshow`) memicu cek status langsung | E2E |

## Fase E: email dan favicon

| ID | Kebutuhan | Kriteria terima |
|---|---|---|
| E1 | Template struk email memakai nama dan warna tenant, Cassy hanya di footer, versi teks polos, tanpa nama MyCaffe | Uji kirim ke kotak masuk; cek tampilan di Gmail ponsel |
| E2 | Favicon dan ikon PWA dicek terhadap aset di vault `BrandAssetsLogo`; `theme_color` mengikuti token | Lighthouse PWA tanpa peringatan ikon |

## Fase F: keamanan dan utang teknis

| ID | Kebutuhan |
|---|---|
| F1 | Review keamanan terarah: callback dan deeplink (B2, B3), rate limit initiate payment per sesi tamu, tidak ada data pribadi di URL, flag cookie tamu |
| F2 | Ledger utang teknis di `docs/TechDebt.md`: tag image Playwright terikat versi lockfile, isi legal dimuat di browser karena web tanpa `BACKEND_INTERNAL_URL`, `apiFetch` fallback berbahasa Indonesia, judul ToS tenant masih MyCaffe |

## Tugas Director

| Tugas | Memblokir |
|---|---|
| Kirim menu asli kafe beserta izin pemakaian nama dan foto | C11 |
| Luruskan akun Midtrans `G357841497` dan kirim form koreksi | B7, go-live |
| Review naskah tamu `1.0.0-draft` bersama penasihat hukum | Go-live |
| Putuskan judul ToS tenant (masih MyCaffe) | Rilis legal |
| Tetapkan masa simpan nama dan email tamu (S7) | Naskah privasi final |
| Backup `CREDENTIAL_ENCRYPTION_KEY` ke vault, rotasi token WhatsApp bridge | Go-live |
| Uji W1 dan W6 di minimal 3 e-wallet | W1, W6 |
| Setujui VPS sebelum transaksi uang sungguhan (A6) | Go-live |

## Urutan PR

1. Infra demo: A1 sampai A5 (repo backend dan host).
2. Backend: B1 sampai B3, A3.
3. Web: C9 token dan C8 ikon (fondasi), lalu C1, C2, C6.
4. Web: C3, C4, B4, B5.
5. Web: C5, C7, D1 sampai D4.
6. Backend: E1. Web: E2.
7. F1 review keamanan dan F2 ledger.
8. C11 data demo setelah menu asli diterima.

## Bukan cakupan

Halaman toko publik (Q3), web push (Q4), metode bayar selain QRIS, GoPay, ShopeePay, pencarian menu (W8), riwayat pesanan (W11), bagi tagihan (W13), mode gelap tamu.

## Risiko

| Risiko | Mitigasi |
|---|---|
| Laptop mati saat ada transaksi | A1 sampai A3 sekarang, A6 sebelum uang sungguhan |
| Tenant tertahan di review Midtrans karena tidak punya website | Checklist B6 dibagikan saat onboarding; Director menanyakan ke Midtrans apakah profil media sosial diterima |
| Deeplink tidak kembali ke WPA di beberapa browser | D4 memeriksa status saat halaman aktif lagi; tombol "Saya sudah bayar" sebagai cadangan |
| Foto menu asli berkualitas rendah | Pedoman foto dibagikan ke tenant; inisial sebagai cadangan |

## Sumber

- Midtrans, best practices to handle notification: https://docs.midtrans.com/reference/best-practices-to-handle-notification
- Midtrans, HTTP(S) Notification / Webhooks: https://docs.midtrans.com/docs/https-notification-webhooks
- Midtrans, Transaction Status Cycle: https://docs.midtrans.com/docs/transaction-status-cycle
- Midtrans, Core API advanced features (custom expiry): https://docs.midtrans.com/docs/coreapi-advanced-features
