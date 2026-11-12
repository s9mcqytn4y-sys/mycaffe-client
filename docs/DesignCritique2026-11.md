# Kritik desain Cassy: WPA, Android, iOS (PRD v4 B7)

Tanggal: 2026-11-12. Diukur terhadap PRD v4 (`docs/PrdCassyV4.md`) dan pedoman brand di vault (`BrandAssetsLogo\Cassy\Guidelines`). Tingkat: **Kritis** (salah secara bisnis atau aturan inti), **Sedang** (mengganggu pengguna atau brand), **Ringan** (rapikan saat menyentuh area itu).

## Ringkasan

WPA tamu sudah matang secara fungsi dan aksesibilitas, dan sejak #52 dan #53 sudah memakai palet, fallback, dan logo baru. Masalah terbesar ada di Android POS: masih bertema Espresso & Crema, memakai kategori dan teks khusus kopi, dan **menghitung pajak di perangkat dengan bilangan pecahan**. iOS tidak punya UI; yang perlu hanya nama teknis.

## WPA tamu (`apps/web`)

### Kesan pertama

Nama gerai besar di kiri atas dan chip "Meja A1" langsung menjawab "saya di mana, meja berapa". Bar keranjang di bawah menjadi satu ajakan yang jelas. Tampilan netral tinta tidak lagi terasa kedai kopi.

### Kegunaan

| Temuan | Tingkat | Rekomendasi |
|---|---|---|
| Produk tanpa foto tampil sebagai kotak dengan inisial ("C", "CL"), mirip avatar orang, bukan makanan | Sedang | Ganti dengan ilustrasi kecil per kategori dari paket ilustrasi (fase 3) atau ikon `Dish` di tengah; jangan inisial |
| Kartu menu tidak menampilkan "pilih ukuran" sebelum dibuka, sehingga tombol "+" dan "Pilih" terlihat sama | Ringan | Beri keterangan kecil "Ada pilihan" di bawah harga untuk produk bervarian |
| Status "Habis" hanya teks kecil abu di bawah harga | Ringan | Badge dengan ikon dan teks, kartu sedikit diredupkan (sudah ada), sejajar dengan aturan status selalu teks |

### Hierarki visual

- **Yang pertama dilihat**: nama gerai, lalu bar keranjang. Benar untuk tujuan tamu.
- **Alur baca**: nama gerai, kategori, kartu menu dua kolom, keranjang. Lurus.
- **Penekanan**: harga tebal dan tabular, sudah benar. Tagline dan banner pengumuman CMS bersaing dengan menu bila keduanya diisi; batasi banner ke satu baris.

### Konsistensi

| Elemen | Masalah | Rekomendasi |
|---|---|---|
| `packages/design-tokens/i18n.json` | Tidak dipakai aplikasi mana pun (hanya dibaca verifier), dan isinya bertentangan dengan copy sebenarnya: "Sedang Disiapkan Barista/Dapur", "Didukung oleh AzureDevID", "Bayar Tunai di Kasir" | Hapus berkas dan pemeriksaannya, atau jadikan sumber copy betulan. Rekomendasi: hapus, karena copy WPA ada di `messages.ts` |
| Ikon | Enam ikon Phosphor di `Icon.tsx`; ikon F&B Cassy baru ada di vault | Tambahkan ikon F&B ke `Icon.tsx` saat layar yang memakainya dibangun |

### Aksesibilitas

- **Kontras**: lolos AA di tema terang dan gelap, diperiksa `verify-tokens.mjs` dan axe di CI.
- **Target sentuh**: 44 px ke atas di WPA.
- **Keterbacaan**: Plus Jakarta 16 px untuk isi, reflow 320 px tanpa gulir samping (diuji).

### Yang sudah baik

- Langkah status pesanan dengan teks, waktu, dan bel yang bisa dibisukan.
- Pesan galat spesifik per kode API, termasuk pemeliharaan.
- Gerai memimpin: warna gerai menggantikan tinta bila terbaca.

## Android POS (`apps/android`)

### Kesan pertama

Tata letak tiga panel (rail, katalog, ringkasan) tepat untuk tablet landscape. Tapi warnanya masih cokelat espresso dan kategori kopi, jadi tablet ini terlihat seperti POS kedai kopi, bukan POS untuk semua F&B.

### Kegunaan

| Temuan | Tingkat | Rekomendasi |
|---|---|---|
| Pajak dihitung di perangkat dengan pecahan: `val taxPb1 = (subtotal * 0.10).toLong()` (`ui/pos/PosLandscapeScreen.kt:121`). Melanggar dua aturan inti: harga dan pajak hanya dari server, uang selalu bilangan bulat | Kritis | Ambil subtotal, pajak, dan total dari quote server (seperti WPA). Masuk Android Rilis Satu PR 1 dan 7 |
| Kategori menu dikunci di `strings.xml`: Espresso, Manual Brew, Non-Coffee, Beans & Bottle | Kritis | Kategori dari `/catalog`, bukan string. Hapus `category_*` |
| Placeholder pencarian "Cari 38 menu resmi (Espresso, Latte, Croissant...)" | Sedang | "Cari menu" saja; jumlah dan contoh bergantung gerai |
| Tombol tunai cepat "50k", "100k", "200k" | Sedang | Format rupiah penuh ("Rp 50.000") dan nilai dari pecahan uang yang wajar untuk total, bukan konstanta |
| 27 teks UI ditulis langsung di Compose, tidak lewat `strings.xml` (contoh `MidtransConfigScreen.kt`, `PosQrisModal.kt`) | Sedang | Pindahkan ke resource; label "QRIS DINAMIS (MIDTRANS CORE API)" diganti bahasa pengguna: "QRIS" |
| Tombol hapus item memakai karakter "✕" sebagai teks (`PosCatalogGrid.kt:98`) | Ringan | Ikon dengan `contentDescription` "Hapus <nama item>" |

### Konsistensi

| Elemen | Masalah | Rekomendasi |
|---|---|---|
| Warna | `ui/theme/Theme.kt` menulis 26 warna Espresso & Crema dengan tangan | Hasilkan `CassyColors.kt` dari `tokens.json` (generator sudah ada untuk web), pakai tema gelap pandan untuk POS dan KDS |
| Huruf | Tidak ada font produk; jatuh ke Roboto bawaan | Plus Jakarta Sans lewat `res/font`, tabular figures untuk harga |
| Ikon | Belum memakai ikon Cassy | `ImageVector` dari paket ikon vault (CashDrawer, Qris, Shift, KitchenTicket) |

### Aksesibilitas

- **Kontras**: tema lama lolos AA, tapi akan diganti; tema baru sudah diukur.
- **Target sentuh**: pencarian `Modifier.size(<48.dp)` tidak menemukan apa pun; belum diukur di perangkat dengan Layout Inspector.
- **Pembaca layar**: tidak ada `contentDescription = null` yang tertinggal; tombol "✕" perlu label.

### Yang sudah baik

- Antarmuka repository sudah dipisah dari implementasi Ktor (`domain/repository`), dasar yang benar untuk SOLID.
- Teks i18n ID dan EN sudah punya kerangka `values` dan `values-en`.
- Tata letak landscape memakai proporsi lebar layar, bukan ukuran tetap.

## iOS (`apps/ios`)

Hanya jembatan perangkat keras Objective-C untuk printer (`MyCaffeHardwareBridge`); tidak ada layar. Tidak ada kritik UI. Nama kelas memakai "MyCaffe", yang menurut CLAUDE.md boleh bertahan sebagai pengenal teknis; ganti hanya bila jembatan ini dipublikasikan sebagai SDK.

## Prioritas

1. **Pajak dan total dari server di Android** (Kritis). Uang yang salah di struk merusak kepercayaan gerai dan melanggar aturan inti. Masuk Rilis Satu PR 1, quote dari API seperti WPA.
2. **Kategori dan copy Android lepas dari kopi** (Kritis untuk rebrand). Kategori dari `/catalog`, placeholder netral, teks ke `strings.xml`.
3. **Tema Android dari token** (Sedang). Generator `build-tokens.mjs` ditambah keluaran Compose, Plus Jakarta di `res/font`, tema gelap pandan untuk POS dan KDS.
4. **Placeholder produk tanpa foto di WPA** (Sedang). Ikon `Dish` sekarang, ilustrasi kategori di fase 3.
5. **Hapus `i18n.json` yang mati** (Ringan, tapi menyesatkan kontraktor).
