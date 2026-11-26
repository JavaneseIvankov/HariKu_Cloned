# Handoff — Revyl Proof of Changes setup (HariKu_Cloned)

Status per 2026-11-23. Setup dijalankan dari sesi pi lain (cwd `/home/arundaya`), bukan dari repo ini.
Berkas ini satu-satunya sumber status; jangan mengulang langkah yang sudah bertanda ✓.

## Ringkasan status

| Langkah | Status |
|:---|:---|
| Revyl CLI | ✓ v0.2.43 di `~/.revyl/bin/revyl` (tambahkan ke PATH tiap shell) |
| Auth | ✓ org `Runsworkspace`, akun `runsworkspace@gmail.com` |
| Deteksi app | ✓ android / gradle, path `.` |
| `.revyl/config.yaml` | ✓ dibuat + published (`revyl config push`) |
| App Revyl | ✓ `hariku_cloned Android` = `511af424-aaa9-4e24-bdbd-1d350c69b4e9` |
| Project Revyl | ✓ `15a73e3a-44de-4eff-802e-c529262da3c8` |
| GitHub App | ✓ connected ke `JavaneseIvankov/HariKu_Cloned`, access granted |
| Project status | ✓ `enabled` (authority: manual) |
| Proof of Changes | ✓ ON (`pr_review.proof_of_changes.enabled: true`) |
| Build pertama | ✓ `remote-f5e68a8c` (job `f5e68a8c-df09-449d-8c2d-66f38e4124a1`), build `a700c9b0-fc97-42a6-bd5e-3affafb345f0` |
| Device session | ✓ android ready, session `347e1a2c-4071-42f3-bb2b-603ee9168a5f` |
| Sign-in proof | ✗ **belum** — app berhenti di layar `Masuk` |

## ID dan URL

- Viewer sesi device: https://app.revyl.ai/sessions/347e1a2c-4071-42f3-bb2b-603ee9168a5f
- Build logs: https://app.revyl.ai/apps/511af424-aaa9-4e24-bdbd-1d350c69b4e9/builds/f5e68a8c-df09-449d-8c2d-66f38e4124a1
- Branch Revyl yang dipakai: `feat/interactive-chat-assistant` (auto-detect dari git)
- Screenshot verifikasi: `/tmp/revyl-app.png` (layar Masuk HariKu, bukan crash → secret Firebase terbukti masuk)

## Perintah dasar (selalu export dulu)

```sh
export PATH="$HOME/.revyl/bin:$PATH" REVYL_AGENT=pi
cd /home/arundaya/AndroidStudioProjects/HariKu_Cloned
```

`REVYL_SETUP_REF` hanya untuk atribusi setup, tidak wajib untuk operasi harian.

## Build secrets (nilai TIDAK ada di repo)

| Secret | Dipakai untuk |
|:---|:---|
| `GOOGLE_SERVICES_JSON` | ditulis ke `app/google-services.json` oleh `setup_commands` |
| `GEMINI_API_KEY` | ditulis ke `local.properties` sebagai `gemini.api.key` |

Keduanya file gitignored, jadi build cloud tanpa secret ini akan sukses tapi app gagal saat runtime (FirebaseAuth tanpa `google_app_id`, chatbot tanpa key).

Perbarui nilai (jangan pernah echo nilainya):
```sh
revyl build secret set GOOGLE_SERVICES_JSON --stdin < app/google-services.json
awk -F= '/^gemini.api.key=/{print substr($0, index($0,"=")+1)}' local.properties \
  | revyl build secret set GEMINI_API_KEY --stdin
```

## Langkah berikutnya (hanya ini yang tersisa)

Bukti signed-in. Repo tidak punya bypass/debug-login maupun test account (yang ada hanya string dummy di `AuthValidationTest.kt`). Dua pilihan, keduanya butuh keputusan pemilik repo:

1. **Test account Firebase** — simpan kredensial sebagai secret, lalu tambah instruksi sign-in ke `pr_review.proof_of_changes.system_prompt` dengan placeholder:
   ```sh
   revyl global var set "REVYL_TEST_EMAIL=<email>"
   revyl global var set "REVYL_TEST_PASSWORD=<password>" --secret
   ```
2. **Handler bypass preview-only** — mengubah kode auth (`feature/auth/`), jadi tampilkan diff dan tunggu persetujuan eksplisit sebelum menulis. Setelah itu build ulang, karena build lama tidak memuat handler.

Verifikasi setelah salah satu dipasang:
```sh
revyl device stop --session-id 347e1a2c-4071-42f3-bb2b-603ee9168a5f
revyl device start --app-id 511af424-aaa9-4e24-bdbd-1d350c69b4e9 --no-open --json
revyl device validation --session-id <session-baru> "The app shows a signed-in screen, not a sign-in screen" --json
```

Kalau sign-in tidak dibutuhkan, cukup tutup sesi dan biarkan Proof of Changes menguji layar publik:
```sh
revyl device stop --session-id 347e1a2c-4071-42f3-bb2b-603ee9168a5f
```

## Hemat kuota build

Cloud build memakai kuota. Alternatif, tanpa mengubah config:

| Cara | Perintah |
|:---|:---|
| Build di mesin sendiri | `revyl build --build-profile development --platform android --local` |
| Upload APK yang sudah ada | `revyl build upload --file app/build/outputs/apk/debug/app-debug.apk --app 511af424-aaa9-4e24-bdbd-1d350c69b4e9` |
| Sesi langsung dari artifact | `revyl device start --app-url <url-apk> --no-open --json` |

Untuk PR automation tanpa cloud build: build di CI lalu daftarkan dengan
`revyl build upload --repo JavaneseIvankov/HariKu_Cloned --commit <sha> --pr <n>`, dan ubah `pr_review.build.kind` dari `revyl` ke mode CI (jalankan `revyl config validate` untuk memastikan nilai yang sah).

## Jebakan yang sudah ditemui

- `revyl config validate` gagal total (pesan "Project configuration operation could not be completed") selama GitHub App belum connected. Itu bukan config salah.
- `revyl build cancel` hanya mengirim request; job lama tetap tampil sesaat.
- Output revyl yang mengandung ANSI/TUI bisa mengacaukan pipe; redirect ke file lalu `cat -v`.
- `app/google-services.json` dan `local.properties` gitignored — jangan pernah commit; pakai build secret.
- Config lama: `build.profiles.development.android`, bukan `preview`. Nama profil bebas, tapi `pr_review.build.profile` harus cocok.

## Berkas yang berubah

- `/.revyl/` (baru, untracked) — `config.yaml` (commit ini), sisanya state lokal yang di-ignore `.revyl/.gitignore`
- Tidak ada perubahan kode aplikasi, tidak ada commit, tidak ada push.
