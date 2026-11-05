package com.example.smarttriviahub;

/**
 * Semua nilai tetap yang dikongsi antara activity dan kelas API.
 * Key Intent diletakkan di sini supaya tiada string bertaburan: kalau key ditaip salah,
 * compiler terus beri ralat (berbanding string biasa yang hanya gagal masa app berjalan).
 */
public final class Constants {

    // Constructor private: kelas ini hanya bekas pemalar, tak perlu dibuat objek.
    private Constants() {
    }

    // Key untuk Bundle / Intent extras
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_STUDENT_ID = "extra_student_id";
    public static final String EXTRA_PROGRAM = "extra_program";
    public static final String EXTRA_CATEGORY_ID = "extra_category_id";
    public static final String EXTRA_CATEGORY_NAME = "extra_category_name";
    public static final String EXTRA_DIFFICULTY = "extra_difficulty";
    public static final String EXTRA_SCORE = "extra_score";
    public static final String EXTRA_TOTAL = "extra_total";
    public static final String EXTRA_RESULTS = "extra_results";

    // Open Trivia DB
    public static final String API_BASE_URL = "https://opentdb.com/api.php";
    public static final int QUESTION_COUNT = 10;
}
