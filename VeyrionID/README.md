# VeyrionID

Plugin Paper 1.21.1 untuk Kartu Identitas Veyrion.

## Build
    mvn package
Hasil: `target/VeyrionID-1.0.0.jar` → taruh di folder `plugins/`.
(Butuh Java 21. Dibuat untuk Paper 1.21.1; versi 1.21.x lain umumnya tetap jalan.)

## Perintah
| Perintah | Fungsi |
|---|---|
| `/ki daftar <nama> <umur> <DD/MM/YYYY> <ras>` | Daftar & terima kartu (pakai `_` untuk spasi) |
| `/ki lihat [pemain]` | Lihat kartu (pemain lain: admin) |
| `/ki tunjuk` | Tunjukkan kartu ke pemain terdekat |
| `/ki kartu` | Ambil ulang item kartu |
| `/ki hapus <pemain>` | Hapus data (admin) |
| `/ki reload` | Muat ulang config (admin) |

Alias: `/identitas`, `/ktp`. Izin: `veyrion.id.use` (default semua), `veyrion.id.admin` (OP).

## Interaksi
Klik kanan kartu → lihat kartu sendiri. Klik kanan pemain sambil memegang kartu → tunjukkan ke pemain itu.

## Config
`config.yml`: daftar ras, izin daftar ulang, radius `tunjuk`, dan `custom-model-data` untuk tekstur kartu (resource pack, mis. memakai logo Veyrion).
