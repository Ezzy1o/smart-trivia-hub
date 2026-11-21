package com.example.smarttriviahub;

/**
 * All constant values shared between activities and API classes.
 * Intent keys are placed here so there are no scattered strings: if a key is typed incorrectly,
 * the compiler immediately flags an error (compared to regular strings which only fail at runtime).
 */
public final class Constants {

    // Private constructor: this class is just a constant container, no need to instantiate objects.
    private Constants() {
    }

    // Keys for Bundle / Intent extras
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
