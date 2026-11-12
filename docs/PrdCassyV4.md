# PRD v4: Re-branding Cassy dan AzureDevID, refine UI, Android Rilis Satu, landing

Status: disetujui Director, 2026-11-12. Pengganti arah visual di PRD v3. Rujukan: `docs/PrdWpaV3.md`, `apps/android/docs/ReleaseOnePlan.md`, `docs/TechDebt.md`.

## Ringkasan

Cassy pindah dari "POS kafe" menjadi platform pesan dan kasir untuk semua usaha makanan dan minuman di Indonesia. Identitas Espresso & Crema yang terlalu kopi diganti palet pandan dan kunyit. Pekerjaan berjalan tiga fase: fondasi brand dan WPA, Android Rilis Satu di atas token baru, lalu ilustrasi 3D dan landing.

Konteks operasional: tinjauan hukum naskah tamu dan tenant selesai; rotasi kredensial ditutup oleh Director; belum ada VPS, semua backend tetap di lingkungan lokal (WSL).

## Keputusan

| # | Topik | Keputusan |
|---|---|---|
| Q1 | Pasar | Semua usaha F&B: restoran, kafe, bakery, food court, cloud kitchen, kios. Ritel belum, karena fitur barcode dan varian SKU belum ada |
| Q2 | Arsitektur brand | Tiga lapis. Gerai memimpin aplikasi tamu. Cassy brand produk (landing, POS, KDS, onboarding, "Ditenagai Cassy"). AzureDevID entitas hukum (legal, invoice, footer email, profil perusahaan) |
| Q3 | Kepribadian | Hangat dan praktis: rekan kerja yang tenang dan cekatan di dapur yang sibuk. Presisi lewat detail, bukan kemewahan |
| Q4 | Warna | Pandan (utama), kunyit (aksen), putih tulang hangat, tinta hijau kehitaman. Warna status terpisah dan lolos WCAG AA. Tanpa cokelat |
| Q5 | Huruf | Plus Jakarta Sans untuk UI produk. Display face terpisah untuk brand dan landing (kandidat Bricolage Grotesque), dipilih lewat uji wordmark |
| Q6 | Ikon | Diperbarui 2026-11-13: pustaka standar per stack, tanpa paket ikon buatan sendiri. Web memakai `lucide-react`, Android memakai Material Symbols (`androidx.compose.material.icons`) |
| Q7 | Ilustrasi | Satu sumber 3D isometrik di Blender: GLB untuk landing, render PNG/WebP untuk paket ilustrasi, jiplakan SVG untuk ukuran kecil |
| Q8 | AzureDevID | Penyegaran. Logo AD dan biru dipertahankan, ditambah versi mono, aturan ruang kosong, kop surat, dan lockup "oleh AzureDevID" |
| Q9 | Fallback tamu | Gerai tanpa warna, atau warnanya gagal kontras 4.5:1, memakai tinta netral, bukan pandan dan bukan espresso |
| Q10 | Urutan | Brand dan WPA, lalu Android Rilis Satu, lalu ilustrasi dan landing |
| Q11 | Arsitektur Android | Satu modul, lapisan ketat per fitur, Koin, satu klien Ktor, DataStore dengan Tink. Tanpa Paging 3 dan tanpa Retrofit di Rilis Satu |
| Q12 | Hosting landing | `apps/landing` di Cloudflare Pages, `cassy.abbdullaziz.online`. Tombol demo menuju `demo.abbdullaziz.online` |

## Persona brand

**Siapa Cassy.** Rekan kerja yang paling bisa diandalkan saat jam makan siang: tenang, cekatan, tidak banyak bicara, selalu benar soal angka.

**Untuk siapa.** Pemilik usaha F&B yang mengurus banyak hal sendiri, kasir yang melayani antrean, juru masak yang membaca tiket dari jarak dua meter, dan tamu yang ingin pesan tanpa menunggu pelayan.

**Suara.** Bahasa Indonesia sehari-hari yang sopan. Kalimat pendek, kata kerja di depan, angka jelas.

| Lakukan | Hindari |
|---|---|
| "Pesanan sudah masuk dapur." | "Pesanan Anda berhasil diproses dengan sukses!" |
| "Bayar sebelum 14.30." | "Segera selesaikan pembayaran Anda sekarang juga" |
| "Koneksi terputus. Kami coba lagi begitu tersambung." | "Ups! Terjadi kesalahan" |
| Menyebut apa yang terjadi dan langkah berikutnya | Klaim yang tidak bisa dibuktikan, superlatif, emoji |

## Fase 1: fondasi brand dan WPA

| ID | Keluaran | Lokasi | Kriteria terima |
|---|---|---|---|
| B1 | Pedoman brand Cassy: persona, suara, palet, huruf, ikon, ilustrasi, logo, contoh benar dan salah | Vault `BrandAssetsLogo\Cassy\Guidelines\` | Satu dokumen bisa dipakai kontraktor tanpa bertanya |
| B2 | Palet terukur: hex final, rasio kontras setiap pasangan teks dan latar, mode gelap untuk POS dan KDS | B1 dan `packages/design-tokens` | Semua pasangan teks normal lolos 4.5:1, teks besar dan ikon 3:1 |
| B3 | Logo Cassy: wordmark, monogram, ikon aplikasi, versi mono | Vault `BrandAssetsLogo\Cassy\Logo\` | SVG sumber, PNG 16 sampai 1024 px, terbaca di 16 px |
| B4 | Penyegaran AzureDevID: versi mono, ruang kosong, palet, kop surat, lockup | Vault `BrandAssetsLogo\AzureDevId\` | Tidak bertabrakan dengan pandan |
| B5 | Token desain tunggal untuk web, Android, iOS | `packages/design-tokens` | Web, Compose, dan iOS dihasilkan atau diverifikasi dari satu berkas |
| B6 | Dibatalkan 2026-11-13 (lihat Q6). Paket ikon vault dipindah ke Recycle Bin | - | - |
| B7 | Kritik UI WPA, Android, iOS | `docs/DesignCritique2026-11.md` | Setiap temuan punya bukti, prioritas, dan perbaikan |
| W1 | WPA pindah ke token baru dan fallback tinta netral | `apps/web` | E2E dan axe hijau, tanpa sisa warna espresso |
| W2 | Refine copy WPA | `apps/web/src/lib/messages.ts` | Mengikuti tabel suara di atas |
| W3 | Naskah legal versi final: tamu 1.0.0 tanpa label draf, judul ToS tenant memakai Cassy | Migrasi backend baru | Versi lama nonaktif, hash cocok isi, tenant yang sudah setuju tidak kehilangan catatan persetujuannya |

## Fase 2: Android Rilis Satu

Rencana dan urutan PR ada di `apps/android/docs/ReleaseOnePlan.md`, diperbarui untuk token baru, US1 sampai US4 dari PRD v3, dan aturan arsitektur Q11.

## Fase 3: ilustrasi dan landing

| ID | Keluaran | Kriteria terima |
|---|---|---|
| I1 | Delapan adegan isometrik Blender: depan gerai dan QR meja, tamu memesan di ponsel, dapur dan KDS, kasir dan struk, ambil di bar, antar ke meja, pemeliharaan, menu kosong | Satu sudut kamera, satu pencahayaan, palet B2; GLB di bawah 1,5 MB per adegan |
| L1 | Landing `apps/landing`: React, React Three Fiber, GSAP 3, zustand. Cerita kamera melintasi adegan I1, ditambah bagian standar SaaS: masalah, cara kerja, fitur, harga, FAQ, ajakan coba demo | LCP di bawah 2,5 detik di ponsel kelas menengah, versi tanpa 3D untuk `prefers-reduced-motion` dan perangkat lemah, lolos axe |
| L2 | Deploy Cloudflare Pages ke `cassy.abbdullaziz.online` | Pratinjau per PR, produksi dari `main` |

## Risiko

| Risiko | Mitigasi |
|---|---|
| Display face atau logo baru tidak terbaca di ukuran kecil | Uji wordmark di 16, 32, dan 48 px sebelum dikunci; fallback Plus Jakarta Bold |
| Hijau pandan tertukar dengan hijau status "lunas" | Status memakai hijau daun yang lebih terang plus teks; tidak pernah warna saja |
| Adegan 3D berat di ponsel murah | Anggaran ukuran GLB, kompresi Draco atau Meshopt, fallback gambar statis |
| Ganti tema WPA mengubah tampilan gerai demo | Gerai dengan warna sendiri tetap memakai warnanya; hanya fallback yang berubah |
