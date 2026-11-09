# Roadmap dan PRD Pilot Satu

Status: draf untuk persetujuan Director, 10 November 2026.
Menggantikan bagian urutan di `apps/android/docs/ReleaseOnePlan.md`. Bagian blocker di `docs/WpaReview2026-11.md` sudah basi: fail-closed (#122), bagi tagihan backend (#123), dan rate limit nginx sudah selesai.

## Ringkasan

WPA tinggal satu sprint pengerasan untuk layak pilot. Android masih kerangka tanpa login nyata, jadi Android menjadi jalur kritis. Target berikutnya adalah satu kafe nyata memakai Cassy selama 2 minggu, gratis, dengan QR order, QRIS ke akun Midtrans kafe itu sendiri, dan KDS di tablet.

Keputusan Director:

| Topik | Keputusan |
|---|---|
| Urutan Android | Operasional dulu (R1A), setup kemudian (R1B) |
| Dapur di pilot 1 | KDS layar saja, tanpa printer |
| Pilot 1 | Gratis, 1 kafe, 2 minggu, perjanjian tertulis |
| Bagi tagihan | Flag mati sampai kasir Android bisa melunasi sisa |
| WPA | Hanya QR order tamu; semua operasional di Android |

## Kondisi hari ini (terverifikasi di demo)

- API `sha-0024b31`, schema 000057, web `sha-96d39b8`, nginx sinkron.
- `central-cafe`: key sandbox BYOK aktif, 5 produk berfoto di R2, nama toko Cassy Demo Store.
- QRIS end-to-end lolos: charge, simulator Midtrans, webhook per tenant, order lunas, tiket KDS terbentuk, halaman tamu berubah sendiri menjadi lunas.
- Web demo tidak lagi punya halaman operasional. Dua pesanan uji menunggu di KDS tanpa penerima sampai R1A selesai.

## Roadmap

Berbasis gerbang, tanpa tanggal karena belum ada data velocity.

| Fase | Isi | Gerbang keluar |
|---|---|---|
| 1. WPA siap pilot | W1 sampai W7 | E2E lulus terhadap demo, naskah privasi final |
| 2. Android R1A | A1 sampai A4 | Di tenant demo: login, buka shift, pesanan QR sampai selesai di KDS |
| 3. Android R1B | A5 sampai A8 | Kafe baru siap tanpa panggilan API manual |
| 4. Kesiapan pilot | S1 sampai S8, tugas Director | Semua S ditutup, satu transaksi uji di lingkungan pilot |
| 5. Pilot 1 | 1 kafe, 2 minggu | Target metrik tercapai |
| 6. Setelah pilot | W8 sampai W13, A9, pilot 3 sampai 5 kafe, penagihan | Bagi tagihan menyala per tenant |

Tidak masuk roadmap ini: laporan, stok, invoice B2B, stamp, ojol, MikroTik, antrean TV, iOS, printer, landing page.

## PRD: WPA QR Order

Tujuan: tamu memesan dan membayar dari meja tanpa bantuan staf.

| ID | Prio | Kebutuhan | Kriteria terima |
|---|---|---|---|
| W1 | P0 | Tombol simpan QR sebagai PNG dan petunjuk unggah dari galeri e-wallet. Tamu memesan dan membayar di HP yang sama, jadi QR di layar tidak bisa dipindai sendiri | Unduhan tertangkap Playwright; uji manual minimal 3 e-wallet sebelum dibangun |
| W2 | P0 | Pesanan aktif tahan reload: simpan id pesanan dan kunci percobaan, kosongkan keranjang setelah pesanan dibuat. Saat ini reload membuat kunci baru dan bisa menggandakan pesanan | E2E: reload menampilkan QR yang sama, `POST /guest/orders` satu kali |
| W3 | P0 | Pesan per kode error: sesi habis, QR dirotasi, produk habis, harga berubah. Saat ini semua 409 dianggap harga berubah | Satu E2E per kondisi, keranjang tetap utuh |
| W4 | P0 | Status batal dan refund tidak tampil sebagai "diterima" | Unit test semua nilai status |
| W5 | P0 | Tujuan data dan tautan privasi di dekat field nama dan email | Ada dalam dua bahasa |
| W6 | P0 | Uji pemindaian QR dengan logo tengah di aplikasi nyata; hapus logo kalau ada yang gagal | Minimal 3 aplikasi berhasil memindai |
| W7 | P0 | Gerbang kualitas: E2E terhadap demo, Playwright WebKit di CI, keyboard penuh, zoom 200%, axe | CI hijau, 0 pelanggaran serious |
| W8 | P1 | Pencarian lokal, hasil kosong menawarkan hapus kata kunci | E2E |
| W9 | P1 | Satu daftar dengan heading; strip kategori melompat dan mengikuti gulir | E2E posisi gulir |
| W10 | P1 | Garis status: menunggu, diterima, disiapkan, siap | `aria-live` terverifikasi |
| W11 | P1 | Halaman struk dan daftar pesanan saya | E2E |
| W12 | P1 | Foto detail di sheet produk, pengganti foto dengan rasio tetap | CLS di bawah 0,1 |
| W13 | P2 | UI bagi tagihan dan halaman bagian teman, di belakang flag | Aktif hanya setelah A9 |

### Perbaikan UI/UX

Temuan dari demo. Mengikuti `DESIGN.md` dan token yang ada, tidak membuat arah visual baru.

| Masalah | Perbaikan |
|---|---|
| Scrollbar bawaan tampil di strip kategori pada desktop | Sembunyikan scrollbar, pertahankan gulir dan fokus keyboard |
| Label "Reguler" tampil walau produk hanya punya satu varian | Tampilkan varian hanya bila produk punya lebih dari satu |
| Deskripsi menu berbahasa Inggris pada UI Indonesia | Isi seed demo dalam bahasa Indonesia |
| Foto Iced Latte dominan interior, foto V60 berupa ketel | Ganti foto; pedoman foto: minuman atau makanan mengisi minimal setengah bingkai |
| Kartu QR dan status bayar | Nominal dan batas waktu tetap terlihat tanpa gulir di layar 360 px |
| Angka durasi gerak berbeda antara `DESIGN.md` dan review | Tetapkan satu nilai di `DESIGN.md` |

Zustand, GSAP, dan Sonner tetap tidak diperlukan.

### Aturan brand

| Elemen | Aturan |
|---|---|
| Nama, logo, warna di layar tamu | Milik tenant dan dominan |
| Cassy | Hanya "Ditenagai Cassy" di footer, ukuran teks kecil, tanpa logo |
| Warna tenant | Hanya bila kontras AA terpenuhi; bila gagal, kembali ke token Espresso & Crema |
| Nama MyCaffe | Tidak boleh terlihat pengguna, termasuk data demo, email, dan judul halaman |
| Bahasa | Indonesia baku tapi ringan, Inggris sebagai pilihan, tanpa emoji |
| Klaim | Tanpa jumlah pengguna, testimoni, atau sertifikasi yang belum ada |

## PRD: Android Rilis Satu

Tujuan: staf melayani pesanan QR dari tablet, lalu pemilik menyiapkan gerai sendiri.

| ID | Fase | Kebutuhan | Kriteria terima |
|---|---|---|---|
| A1 | R1A | Fondasi: `BuildConfig` per lingkungan, satu klien HTTP, refresh token satu per satu, hapus data contoh, pajak lokal, dan teks simulasi | `./gradlew test bundleDebug` lolos, grep IP dan `0.10` kosong |
| A2 | R1A | Login, pairing, bootstrap, sesi tersimpan, gagal tertutup | Test: token kedaluwarsa, PIN terkunci |
| A3 | R1A | Shift buka dan tutup, navigasi konsol | Tanpa shift, pesanan QR ditolak dengan pesan jelas |
| A4 | R1A | KDS WebSocket, terbaca dari 2 meter, tombol habis cepat, banner koneksi | Status usang memuat ulang tiket |
| A5 | R1B | Kredensial Midtrans, penanda sandbox atau produksi | Key sandbox ditolak di produksi |
| A6 | R1B | Meja, rotasi QR, poster QR | Tamu lama melihat pesan QR dirotasi |
| A7 | R1B | Profil gerai (termasuk nama toko, butuh API baru), brand, menu lengkap dengan foto | Perubahan tampil di WPA |
| A8 | R1B | Kasir tersambung `/pos/*`, total dari server | Tidak ada hitung pajak di klien |
| A9 | Fase 6 | Penyelesaian bagian bagi tagihan, rute staf dibangun dulu | Baru setelah ini flag boleh menyala |

UX lintas layar: status operasional selalu terlihat di layar, bukan `Toast`. Target sentuh 48 px. Satu tugas per layar untuk setup.

Sebelum track produksi: cek syarat `targetSdk` Google Play dan evaluasi dua dependensi alpha.

## Persyaratan keamanan pilot

| ID | Kebutuhan | Bukti selesai |
|---|---|---|
| S1 | `APP_ENV=production` di lingkungan pilot, sehingga tenant tanpa BYOK ditolak (#122) | Uji charge tanpa kredensial gagal |
| S2 | Backup sebelum deploy aktif; `DEPLOY_SKIP_BACKUP` tidak boleh menyala | Log deploy dan uji restore |
| S3 | `CREDENTIAL_ENCRYPTION_KEY` tersimpan di vault, prosedur rotasi tertulis | Runbook, uji memakai `key_version` |
| S4 | Rotasi token WhatsApp bridge yang pernah tercetak di sesi kerja | Token lama ditolak |
| S5 | Android: token di Keystore, tanpa cleartext, tanpa rahasia di `BuildConfig` | Pemeriksaan APK |
| S6 | Tautan bagi tagihan membawa token di URL: `Referrer-Policy: no-referrer`, path disamarkan di log nginx, tanpa skrip pihak ketiga. Wajib sebelum flag menyala | Uji header dan contoh log |
| S7 | Data tamu: masa simpan nama dan email ditetapkan, naskah privasi final | Naskah disetujui, job pembersihan berjalan |
| S8 | Advisory `postcss` ditutup, audit dependensi hijau | Keluaran audit |

Yang sudah berlaku: RLS `mycaffe_app`, token hanya disimpan sebagai hash, cookie tamu Secure dan HttpOnly, CSRF header, verifikasi tanda tangan webhook.

Risiko diterima sementara: limiter dalam proses hanya cocok untuk satu instance API, dan CSP masih `unsafe-inline` untuk skrip Next.js.

## Metrik pilot

Diambil dari data server, tanpa analitik klien. Semua target adalah asumsi awal.

| Metrik | Target |
|---|---|
| Sesi QR yang menjadi pesanan | Di atas 50% |
| Pesanan yang lunas | Di atas 85% |
| QRIS kedaluwarsa | Di bawah 10% |
| Pesanan ganda dalam satu sesi | 0 |
| Pesanan QR selesai tanpa bantuan staf | Di atas 80% |

Setiap selisih antara webhook dan pesanan diperiksa Director selama pilot.

## Tugas Director

- Koreksi akun Midtrans dan naskah legal serta DPA oleh penasihat hukum.
- Backup kunci enkripsi ke vault, rotasi token WhatsApp.
- Memilih kafe pilot dan menandatangani perjanjian pilot.
- Menyetujui rilis backend bertag (18 commit setelah v0.12.1).

## Risiko utama

| Risiko | Mitigasi |
|---|---|
| Android jalur kritis dan dikerjakan satu orang | R1A dipotong sampai loop KDS berjalan, setup menyusul |
| Unggah QR dari galeri tidak didukung semua e-wallet | Uji sebelum W1 dibangun; cadangan: bayar di kasir atau pindai dari HP teman |
| Tablet KDS mati atau WebSocket putus saat jam ramai | Banner koneksi, muat ulang tiket, prosedur manual tertulis |
