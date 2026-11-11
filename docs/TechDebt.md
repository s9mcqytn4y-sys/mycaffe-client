# Ledger utang teknis (PRD v3 N11, F2)

Satu baris per utang: apa, kenapa belum dibayar, dan kapan harus dibayar. Hapus baris saat lunas, catat PR-nya di commit. Review keamanan F1 ada di `mycaffe-backend/docs/Engineering/SecurityReviewF1.md`.

Prioritas: **T** tinggi (bayar sebelum pilot), **S** sedang (bayar dalam rilis berikutnya), **R** rendah (bayar saat menyentuh area itu).

## Dari PRD v2 F2

| ID | Utang | Prioritas | Pemicu bayar |
|---|---|---|---|
| D1 | Image Playwright di CI (`mcr.microsoft.com/playwright:v1.65.0-noble`) diselaraskan manual dengan `@playwright/test` di lockfile. Saat ini cocok (1.65.0) | R | Langkah CI yang membandingkan keduanya, atau saat menaikkan Playwright |
| D2 | Isi legal dimuat di browser (`LegalView` client component) karena web tidak memanggil `BACKEND_INTERNAL_URL` saat render server | R | Saat SEO halaman legal penting atau saat menambah halaman server lain |
| D3 | Pesan cadangan `apiFetch` ("Tidak bisa terhubung ke server", "Permintaan gagal") hanya bahasa Indonesia. Sebagian besar pemanggil sudah memetakan ke `t.*` | R | Saat ada pemanggil baru yang menampilkan `err.message` |
| D4 | Judul ToS tenant di data masih "Syarat dan Ketentuan Layanan SaaS MyCaffe by AzureDevID" | S | Versi baru dokumen tenant setelah tinjauan hukum; jangan ubah versi yang sudah disetujui tenant |

## Ditemukan selama PRD v3

| ID | Utang | Prioritas | Pemicu bayar |
|---|---|---|---|
| S1 | Token bearer di path (struk, pelacakan, bagi tagihan) tercatat utuh di access log nginx | S | Sebelum log dikirim ke layanan pihak ketiga: samarkan dengan `map` di `log_format` |
| S2 | Repo client belum punya pemindai rahasia di CI (backend sudah Gitleaks) | R | Saat ada konfigurasi atau kunci baru di repo client |
| S3 | Rate limit tamu (sesi, pesanan, pembayaran) disimpan di memori proses, benar hanya untuk satu instance API (ADR-002) | S | Sebelum API dijalankan lebih dari satu instance |
| O1 | `/opt/mycaffe` di host demo adalah salinan `deploy/` dan `scripts/`, bukan checkout git; perubahan konfigurasi deploy perlu disalin manual | S | Sebelum pilot VPS Jakarta: checkout git dengan tag rilis |
| O2 | Port forwarding localhost Windows ke WSL putus; skrip lokal memakai IP WSL | R | Saat mengganti mesin atau setelah `wsl --shutdown` terjadwal |
| O3 | Override `next>postcss` di `pnpm-workspace.yaml` karena Next memasang postcss 8.4.31 | R | Hapus saat Next memakai postcss yang sudah ditambal |
| P1 | Mode antar meja hanya mencapai "Sudah diantar" bila KDS Android menandai `served` (US3) | T | Sebelum gerai antar meja memakai mode `table` |
| P2 | Ikon Cassy PNG dibuat dari SVG vault di repo web, belum disimpan balik ke vault | R | Saat Director merapikan aset vault |
| P3 | Halaman struk menampilkan nama varian tunggal seperti "Reguler" karena struk menyimpan snapshot varian. Disengaja, sama dengan struk kertas | R | Bila tenant meminta; butuh flag `has_variants` di snapshot pesanan |
| P4 | Demo memakai nama grup add-on yang dirapikan lewat `db/seeds/demo_group_names.sql`, sedangkan `test_fixtures.sql` masih memakai nama lama | R | Saat fixture uji ditata ulang |
