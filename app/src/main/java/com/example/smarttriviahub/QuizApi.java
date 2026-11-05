package com.example.smarttriviahub;

import android.content.Context;

import androidx.annotation.StringRes;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Mengambil 10 soalan dari Open Trivia DB menggunakan Volley.
 * Kelas ini TIDAK menyentuh UI: ia hanya pulangkan hasil (senarai soalan atau jenis ralat)
 * melalui Callback. Activity yang menentukan apa yang dipaparkan.
 */
public class QuizApi {

    /** Jenis ralat. Setiap jenis tahu id string mesej pengguna dalam strings.xml. */
    public enum ErrorType {
        NO_INTERNET(R.string.error_no_internet),
        RATE_LIMIT(R.string.error_rate_limit),
        NOT_ENOUGH_QUESTIONS(R.string.error_not_enough_questions),
        GENERIC(R.string.error_generic);

        private final int messageResId;

        ErrorType(@StringRes int messageResId) {
            this.messageResId = messageResId;
        }

        /** Guna: getString(type.getMessageResId()). */
        @StringRes
        public int getMessageResId() {
            return messageResId;
        }
    }

    /** Activity melaksanakan ini untuk menerima hasil. Dipanggil pada main thread. */
    public interface Callback {
        void onSuccess(List<Question> questions);

        void onError(ErrorType errorType);
    }

    // Nilai response_code dalam JSON Open Trivia DB
    private static final int CODE_OK = 0;
    private static final int CODE_NOT_ENOUGH_QUESTIONS = 1;
    private static final int CODE_RATE_LIMIT = 5;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private static final int TIMEOUT_MS = 10000;

    // Soalan "multiple choice" ada 1 jawapan betul + 3 jawapan salah = 4 pilihan
    private static final int INCORRECT_ANSWER_COUNT = 3;

    // Satu RequestQueue dikongsi seluruh app (setiap queue membuka beberapa thread).
    private static RequestQueue requestQueue;

    private QuizApi() {
    }

    private static RequestQueue getRequestQueue(Context context) {
        if (requestQueue == null) {
            // ApplicationContext, bukan Activity: queue hidup lebih lama daripada activity.
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    public static void fetchQuestions(Context context, int categoryId, String difficulty,
                                      Callback callback) {
        String url = Constants.API_BASE_URL
                + "?amount=" + Constants.QUESTION_COUNT
                + "&type=multiple"
                + "&category=" + categoryId
                + "&difficulty=" + difficulty;

        // GET, tiada body (null). Listener pertama = berjaya, kedua = gagal.
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> handleResponse(response, callback),
                error -> callback.onError(errorFromVolley(error)));

        // Tunggu 10 saat (lalai 2.5 saat terlalu singkat untuk API ini), tanpa cuba semula
        // automatik supaya tidak menambah beban bila terkena rate limit.
        request.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS, 0, 1f));

        getRequestQueue(context).add(request);
    }

    /** Respons HTTP 200 diterima. Semak response_code dahulu, kemudian parse soalan. */
    private static void handleResponse(JSONObject response, Callback callback) {
        List<Question> questions;
        try {
            int code = response.getInt("response_code");
            if (code != CODE_OK) {
                callback.onError(errorFromResponseCode(code));
                return;
            }
            questions = parseQuestions(response.getJSONArray("results"));
        } catch (JSONException e) {
            // JSON tidak mengikut bentuk yang dijangka
            callback.onError(ErrorType.GENERIC);
            return;
        }
        if (questions.isEmpty()) {
            // Kod 0 tetapi tiada soalan: kuiz tidak boleh dimulakan
            callback.onError(ErrorType.GENERIC);
            return;
        }
        callback.onSuccess(questions);
    }

    /** JSONArray "results" -> List<Question>. */
    private static List<Question> parseQuestions(JSONArray results) throws JSONException {
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);

            JSONArray incorrectJson = item.getJSONArray("incorrect_answers");
            if (incorrectJson.length() != INCORRECT_ANSWER_COUNT) {
                // QuizActivity mengandaikan tepat 4 pilihan (4 butang); lebih selamat tolak
                // data yang pelik daripada ranap semasa memaparkan butang.
                throw new JSONException("Dijangka " + INCORRECT_ANSWER_COUNT + " jawapan salah");
            }
            List<String> incorrect = new ArrayList<>();
            for (int j = 0; j < incorrectJson.length(); j++) {
                incorrect.add(incorrectJson.getString(j));
            }

            questions.add(new Question(
                    item.getString("question"),
                    item.getString("correct_answer"),
                    incorrect));
        }
        return questions;
    }

    private static ErrorType errorFromResponseCode(int code) {
        switch (code) {
            case CODE_NOT_ENOUGH_QUESTIONS:
                return ErrorType.NOT_ENOUGH_QUESTIONS;
            case CODE_RATE_LIMIT:
                return ErrorType.RATE_LIMIT;
            default:
                return ErrorType.GENERIC;
        }
    }

    /** Permintaan gagal sebelum JSON sempat dibaca (rangkaian atau HTTP error). */
    private static ErrorType errorFromVolley(VolleyError error) {
        // Open Trivia DB pulangkan rate limit sebagai HTTP 429, bukan JSON dengan kod 5.
        if (error.networkResponse != null
                && error.networkResponse.statusCode == HTTP_TOO_MANY_REQUESTS) {
            return ErrorType.RATE_LIMIT;
        }
        if (error instanceof NoConnectionError || error instanceof TimeoutError) {
            return ErrorType.NO_INTERNET;
        }
        return ErrorType.GENERIC;
    }
}
