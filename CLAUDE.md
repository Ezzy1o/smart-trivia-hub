# CLAUDE.md: Smart Learning & Trivia Assessment Hub

Konteks projek untuk Claude Code dan ahli kumpulan. Letak fail ini di **root repo**.

## Latar belakang
- Subjek: **SPG 0562 Android Programming**, German-Malaysian Institute (GMI), Mini Project, Academic Period Jul 2026.
- Dibuat secara **duo**, kod dikongsi melalui GitHub (`Ezzy1o/smart-trivia-hub`).
- Bahasa: **Java** (bukan Kotlin). Template asal: Empty Views Activity. Build script: Kotlin DSL (`build.gradle.kts`).
- Nama package: `com.example.smarttriviahub`

## Keperluan wajib (dari brief & rubric)
App mesti ada **3 Activity** dengan aliran linear:

1. **MainActivity: Registration & Entrance**
   - Kumpul butiran pelajar (nama, student ID, program).
   - Validation untuk field kosong/tak sah (guna `setError()`).
   - Pergi ke QuizActivity guna **explicit Intent + Bundle**.
2. **QuizActivity: Web-Connected Trivia Quiz Hub**
   - Fetch **10 soalan** guna **Volley** (`RequestQueue`, GET, parse `JSONObject`/`JSONArray`).
   - API dicadang: Open Trivia DB `https://opentdb.com/api.php?amount=10&type=multiple`.
   - Butang pilihan jawapan, **ImageView** tukar ikut betul/salah, **Toast/Snackbar** setiap jawapan, score dikemas kini.
3. **ResultActivity: Performance Analytics & Breakdown**
   - Kira peratus, breakdown betul vs salah dalam **ScrollView**, butang restart.

Untuk **semua** activity:
- **Data Binding** (`ActivityXxxBinding`). **Dilarang guna `findViewById()`.** Aktifkan `buildFeatures { dataBinding = true }` dan balut layout dengan `<layout>`.
- Override **kesemua 7** lifecycle callback (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onRestart`, `onDestroy`), setiap satu dengan `Log.d(TAG, ...)`.
- **ConstraintLayout**, widget **center** dalam portrait **dan** landscape.
- **Tiada hardcoded string atau warna.** Semua dalam `strings.xml` dan `colors.xml`.
- `<uses-permission android:name="android.permission.INTERNET"/>` dalam manifest.

## Perangkap yang dah dikenal pasti
- **Rotation dalam QuizActivity**: soalan, index dan score mesti kekal (`onSaveInstanceState` atau ViewModel). Jangan fetch API semula bila skrin diputar.
- **HTML entities** dari Open Trivia DB (`&quot;`, `&#039;`): decode guna `Html.fromHtml()`.
- **Rate limit API**: 1 request setiap 5 saat. Semak `response_code` (0 = OK, 5 = rate limit). Tunjuk mesej ralat + butang Cuba Semula.
- **Shuffle jawapan**, kalau tidak jawapan betul sentiasa di posisi sama.
- Nilai yang dihantar melalui Intent: guna konstan key (contoh `EXTRA_NAME`), jangan string bertaburan.

## Konsep yang dicadangkan (belum dimuktamadkan)
- Tema: **IT Revision Edition**. Sasaran: pelajar diploma yang nak uji kefahaman sebelum exam. Masalah: revision pasif → app bagi active recall dengan feedback serta-merta.
- Tambahan: pilih **kategori** (Computers, Mathematics, Science & Nature, General Knowledge) dan **difficulty** di MainActivity, dihantar sebagai parameter URL API.
- Bonus (hanya jika teras siap 100%): timer setiap soalan.

## Pembahagian kerja (cadangan)
- **Orang A:** setup project, MainActivity, ResultActivity, `strings.xml`/`colors.xml`/tema.
- **Orang B:** QuizActivity, Volley + parsing JSON, model `Question`, rotation, error state.
- **Bersama:** flowchart, pseudocode, report, latihan demo.

## Aliran kerja Git
- `main` sentiasa boleh build. Jangan push terus ke `main`.
- Setiap orang kerja di branch sendiri (`feature/main-activity`, `feature/quiz-volley`, ...).
- Gabung melalui **Pull Request**; ahli yang seorang lagi review dan mesti faham kod sebelum approve.

## Arahan untuk Claude Code
- Penilaian termasuk **demo individu (30%)** di mana setiap ahli mesti boleh pertahankan kod Data Binding, Volley, lifecycle dan MVC. Jadi:
  - **Terangkan setiap perubahan** secara ringkas dan kenapa ia ditulis begitu.
  - Utamakan kod yang mudah dibaca dan difahami berbanding yang terlalu "pandai".
- Ikut semua keperluan wajib di atas; rubric menolak markah untuk satu hardcoded string atau satu lifecycle callback yang tertinggal.
- Jangan tambah dependency selain yang diperlukan (Volley, Material Components) tanpa bertanya.
