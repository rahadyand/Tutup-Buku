# Status tugas hardening dan handoff akademik

## Fase saat ini
Fase5 — checkpoint perbaikan dan pengemasan0.5.1-rc1. Instruksi sumber: lampiran74a07b81-a8d2-4171-9752-818e7d9bed05/pasted-text-1.txt, dibaca seluruhnya. Pengguna mengizinkan pekerjaan mandiri lintas fase1–8, commit/push normal ke main, tanpa menunggu penerimaan antar-fase.

## Bukti awal
- HEAD lokal6172645b874575efc113678938f376fcaa82d1ce, pesan M5 sesuai laporan.
- Working tree awal bersih; origin/main diverifikasi sama6172645 dengan git ls-remote.
- AGENTS.md dibaca. Java21/JavaFX21, schema3, source tetap berlapis dan semua tes memakai database/aset terisolasi.

## Selesai / perubahan
- PRD/dokumen/source/tes/script ditinjau, termasuk SQL/CAS/migrasi/guard/ownership. Matriks audit sedang ditulis.
- Defek terkonfirmasi: kembali ke akademik membawa attendance lama, dapat menimpa hasil edit aktual saat simpan metadata. WorkspaceView sekarang refresh akademik setelah guard navigasi.
- ReleaseHardeningFxCheck membuktikan kegagalan baseline lalu lulus sesudah fix; simpan nama/SKS mempertahankan attendancefalse/confirmedtrue.
- IndependentCalculationTest23tes lulus: literal batas PRD,40kasus ledger integer dan32kasus target pencarian maju. Tambahan regresi bobot/order snapshot dan antrean draft+DB ditulis.
- Versi kandidat baru0.5.1-rc1 pada pom/script/title; kandidat M5 tidak dihapus. Rumus/skema/dependency aplikasi tidak diubah.

## Pemeriksaan aktual
- git status awal kosong; branchmain, remoteGitHub yang diminta cocok.
- -Dtest=ReleaseHardeningFxCheck verify: awal1failure pada stale attendance, kemudian -Dtest=ReleaseHardeningFxCheck,IndependentCalculationTest -DreuseForks=false verify:24tes lulus.
- Regresi final -Pwindows-package verify:163tes lulus. Suite GUI+benchmark:9tes lulus (8GUI+1benchmark). Skala proses125%:1tes lulus. Screenshot reader/grades/academic dibuka dan ditinjau; benchmark median3,486ms,max5,418ms,cold21,610ms. Rincian pada HARDENING_VERIFICATION.

## Blokir
- Belum ada. Komputer kedua, jaringan fisik terputus, Windows125% nyata dan usability manusia akan tetap NOT PERFORMED tanpa bukti nyata.

## Sisa langkah
1. Matriks P0_AUDIT dan kit/Handoff Markdown sudah ditulis; selesaikan peninjauan tautan/keterbacaan dan hasil paket.
2. Semua regresi akhir di atas selesai; tidak perlu mengulang tanpa perubahan/failure baru. Buat checkpoint perbaikan terverifikasi.
3. Kemas rc0.5.1 baru sesudah checkpoint, audit/ekstrak/uji launcher nyata dengan data belajar+akademik. Jangan memakai bukti biner lama untuk biner berubah.
4. Kit penerimaan manusia + morning checklist.
5. Draft laporan, penjelasan OOP, outline/notes presentasi, demo/fallback, tanya-jawab sidang dalam Markdown editable; fakta hilang memakai placeholder.
6. Regresi akhir sesuai perubahan, dokumentasi, audit diff/staging, checkpoint/push normal, cocokkan remote.

## Instruksi melanjutkan
Baca berkas ini setelah kompaksi, lalu git status/log dan hasil tes/artefak aktual. Jangan mengulang fase yang sudah terbukti selesai. Pertahankan kandidat M5 dan data nyata. Markdown adalah format handoff wajib; format tambahan opsional. Jangan mulai fitur P1/P2 atau menulis hasil partisipan fiktif. Selesaikan seluruh pekerjaan independen sebelum menandai goal complete.
