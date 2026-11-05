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

## Konsep (DIMUKTAMADKAN)
- Tema: **IT Revision Edition**. Sasaran: pelajar diploma yang nak uji kefahaman sebelum exam. Masalah: revision pasif → app bagi active recall dengan feedback serta-merta.
- MainActivity ada pilihan **kategori** (Spinner) dan **difficulty** (RadioGroup: easy/medium/hard), dihantar dalam Bundle dan dimasukkan ke URL API (`&category=<id>&difficulty=<level>`).
- Kategori (ID Open Trivia DB): Computers (18, default), Mathematics (19), Science & Nature (17), General Knowledge (9).
- Sesetengah gabungan kategori + difficulty mungkin tiada 10 soalan; API pulangkan `response_code` 1. Mesti tunjuk mesej yang jelas dan cadangkan user tukar difficulty.
- Bonus (hanya jika teras siap 100%): timer setiap soalan.

## Pembahagian kerja (DIPUTUSKAN)
- **Ahli UI** (pemilik repo, Ezzy1o):
  - Setup rangka project (gradle, manifest, 3 activity kosong).
  - Semua fail `res/`: layout (`activity_main`, `activity_quiz`, `activity_result`, `item_breakdown`, versi `layout-land` jika perlu), `strings.xml`, `colors.xml`, `themes.xml`, drawable/ikon.
  - Pastikan widget center dalam portrait dan landscape, dan ScrollView digunakan dengan betul.
- **Ahli Logik:**
  - Semua kod Java: `MainActivity` (validation, Spinner, RadioGroup, Intent + Bundle), `QuizActivity` (Volley, JSON, semakan jawapan, feedback, rotation, error state), `ResultActivity` (peratus, breakdown, restart), model `Question`, kelas `Constants`.
  - Lifecycle logging di ketiga-tiga activity.
- **Report:** Ahli UI tulis Introduction, Screenshot & Penerangan, bahagian XML. Ahli Logik tulis Pseudocode, Flowchart, bahagian Java. Conclusion dan References ditulis bersama.
- **Review silang wajib:** Ahli UI review setiap PR Java sampai faham (demo individu akan tanya pasal Data Binding, Volley, lifecycle). Ahli Logik review setiap PR layout.

## Kontrak antara UI dan Logik
Ahli Logik guna nama-nama ini dalam Java (`binding.<id dalam camelCase>`). **Jangan tukar atau buang ID tanpa beritahu ahli yang seorang lagi**, kerana kod Java akan gagal build. Menambah ID baru dibenarkan.

### activity_main.xml
| ID | Jenis | Kegunaan |
|---|---|---|
| `tv_app_title` | TextView | Tajuk app |
| `tv_subtitle` | TextView | Penerangan ringkas |
| `et_name` | TextInputEditText | Nama (validation di Java) |
| `et_student_id` | TextInputEditText | Student ID (validation di Java) |
| `spinner_program` | Spinner | Program; entri dari `@array/programs` |
| `spinner_category` | Spinner | Kategori; entri dari `@array/categories` (susunan: Computers, Mathematics, Science & Nature, General Knowledge) |
| `rg_difficulty` | RadioGroup | Mengandungi `rb_easy`, `rb_medium`, `rb_hard` |
| `btn_start` | MaterialButton | Mula kuiz |

Bungkus kandungan dalam ScrollView supaya form boleh di-scroll dalam landscape.

### activity_quiz.xml
| ID | Jenis | Kegunaan |
|---|---|---|
| `progress_loading` | ProgressBar (bulat) | Tunjuk semasa fetch API |
| `group_error` | ConstraintLayout `Group` | Mengandungi `tv_error_message` dan `btn_retry` |
| `tv_error_message` | TextView | Mesej ralat (teks diisi oleh Java) |
| `btn_retry` | MaterialButton | Cuba semula fetch |
| `group_quiz` | ConstraintLayout `Group` | Semua widget kuiz di bawah, untuk show/hide sekali gus |
| `tv_question_counter` | TextView | "Soalan 3/10" |
| `progress_quiz` | ProgressBar (mendatar) | Kemajuan 0–10 |
| `tv_score` | TextView | Score semasa |
| `tv_category` | TextView | Nama kategori |
| `tv_question` | TextView | Teks soalan (dalam CardView) |
| `btn_option_1` … `btn_option_4` | MaterialButton | 4 pilihan jawapan |
| `iv_feedback` | ImageView | Ikon neutral / betul / salah |
| `btn_next` | MaterialButton | Soalan seterusnya / Lihat Keputusan |

Root perlu ada ID `root_quiz` (CoordinatorLayout atau ConstraintLayout) sebagai anchor Snackbar.

### activity_result.xml
| ID | Jenis | Kegunaan |
|---|---|---|
| `tv_greeting` | TextView | "Tahniah, <nama>!" |
| `tv_percentage` | TextView | Peratus besar |
| `tv_performance_label` | TextView | Cemerlang / Baik / Perlu Ulangkaji |
| `tv_correct_count` | TextView | Bilangan betul |
| `tv_wrong_count` | TextView | Bilangan salah |
| `scroll_breakdown` | ScrollView | Bekas breakdown |
| `layout_breakdown` | LinearLayout (vertical) | Java tambah satu `item_breakdown` untuk setiap soalan |
| `btn_try_again` | MaterialButton | Kuiz baru, tetapan sama |
| `btn_restart` | MaterialButton | Kembali ke MainActivity |

### item_breakdown.xml (satu baris breakdown)
`tv_item_number`, `tv_item_question`, `tv_item_your_answer`, `tv_item_correct_answer`, `iv_item_status`.

### Resource dikongsi
- **Drawable:** `ic_correct`, `ic_wrong`, `ic_neutral` (Vector Asset).
- **Warna:** `primary`, `primary_dark`, `accent`, `background`, `surface`, `text_primary`, `text_secondary`, `correct`, `wrong`, `on_primary`.
- **String untuk kegunaan Java** (Ahli UI cipta, Ahli Logik guna; teks boleh diubah, nama jangan):
  - Validation: `error_name_empty`, `error_name_invalid`, `error_id_empty`, `error_id_invalid`
  - Feedback: `msg_correct`, `msg_wrong` (dengan `%1$s` = jawapan betul)
  - Ralat API: `error_no_internet`, `error_rate_limit`, `error_not_enough_questions`, `error_generic`
  - Label: `label_question_counter` (`%1$d`/`%2$d`), `label_score` (`%1$d`), `label_percentage` (`%1$d%%`), `label_greeting` (`%1$s`), `btn_see_result`
  - Prestasi: `perf_excellent`, `perf_good`, `perf_needs_revision`
- Ahli Logik boleh tambah string baru jika perlu, di bahagian bawah `strings.xml` bawah komen `<!-- Java messages -->`, untuk elak konflik merge.

### Intent extras (dalam `Constants.java`, dimiliki Ahli Logik)
`EXTRA_NAME`, `EXTRA_STUDENT_ID`, `EXTRA_PROGRAM`, `EXTRA_CATEGORY_ID`, `EXTRA_CATEGORY_NAME`, `EXTRA_DIFFICULTY`, `EXTRA_SCORE`, `EXTRA_TOTAL`, `EXTRA_RESULTS` (senarai soalan + jawapan user).

## Aliran kerja Git
- `main` sentiasa boleh build. Jangan push terus ke `main`.
- Branch ikut peranan: `ui/<skrin>` (contoh `ui/main-layout`) dan `logic/<skrin>` (contoh `logic/quiz-volley`).
- Gabung melalui **Pull Request**; ahli yang seorang lagi review dan mesti faham kod sebelum approve.
- `git pull` dari `main` sebelum mula kerja setiap hari.

## Urutan kerja
1. Ahli UI: rangka project (`setup/skeleton`) → merge ke `main`.
2. Serentak:
   - Ahli UI: layout ikut kontrak di atas, bermula dengan `activity_quiz` (paling banyak widget).
   - Ahli Logik: `Constants`, model `Question`, Volley + parsing JSON (boleh diuji melalui Logcat sebelum UI siap).
3. Gabungkan skrin demi skrin: Main → Quiz → Result.
4. Uji rotation dan landscape untuk setiap skrin.
5. Ambil screenshot sepanjang jalan; flowchart dilukis bersama awal-awal.

## Arahan untuk Claude Code
- Penilaian termasuk **demo individu (30%)** di mana setiap ahli mesti boleh pertahankan kod Data Binding, Volley, lifecycle dan MVC. Jadi:
  - **Terangkan setiap perubahan** secara ringkas dan kenapa ia ditulis begitu.
  - Utamakan kod yang mudah dibaca dan difahami berbanding yang terlalu "pandai".
- Ikut semua keperluan wajib di atas; rubric menolak markah untuk satu hardcoded string atau satu lifecycle callback yang tertinggal.
- Jangan tambah dependency selain yang diperlukan (Volley, Material Components) tanpa bertanya.
