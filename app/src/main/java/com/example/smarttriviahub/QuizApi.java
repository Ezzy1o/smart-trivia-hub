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
 * Fetches 10 questions from Open Trivia DB using Volley.
 * This class does NOT touch the UI: it only returns results (list of questions or error type)
 * via Callback. The Activity determines what is displayed.
 */
public class QuizApi {

    /** Error types. Each type knows its user message string resource ID in strings.xml. */
    public enum ErrorType {
        NO_INTERNET(R.string.error_no_internet),
        RATE_LIMIT(R.string.error_rate_limit),
        NOT_ENOUGH_QUESTIONS(R.string.error_not_enough_questions),
        GENERIC(R.string.error_generic);

        private final int messageResId;

        ErrorType(@StringRes int messageResId) {
            this.messageResId = messageResId;
        }

        /** Usage: getString(type.getMessageResId()). */
        @StringRes
        public int getMessageResId() {
            return messageResId;
        }
    }

    /** Activities implement this to receive results. Called on the main thread. */
    public interface Callback {
        void onSuccess(List<Question> questions);

        void onError(ErrorType errorType);
    }

    // response_code values in Open Trivia DB JSON
    private static final int CODE_OK = 0;
    private static final int CODE_NOT_ENOUGH_QUESTIONS = 1;
    private static final int CODE_RATE_LIMIT = 5;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private static final int TIMEOUT_MS = 10000;

    // Multiple choice questions have 1 correct answer + 3 incorrect answers = 4 choices
    private static final int INCORRECT_ANSWER_COUNT = 3;

    // A single RequestQueue shared across the app (each queue opens multiple threads).
    private static RequestQueue requestQueue;

    private QuizApi() {
    }

    private static RequestQueue getRequestQueue(Context context) {
        if (requestQueue == null) {
            // ApplicationContext, not Activity: queue lives longer than activity.
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

        // GET, no body (null). First listener = success, second = error.
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> handleResponse(response, callback),
                error -> callback.onError(errorFromVolley(error)));

        // Wait 10 seconds (default 2.5s is too short for this API), without automatic retry
        // so as not to add load when hitting rate limit.
        request.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS, 0, 1f));

        getRequestQueue(context).add(request);
    }

    /** HTTP 200 response received. Check response_code first, then parse questions. */
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
            // JSON does not match expected structure
            callback.onError(ErrorType.GENERIC);
            return;
        }
        if (questions.isEmpty()) {
            // Code 0 but no questions: quiz cannot be started
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
                // QuizActivity assumes exactly 4 choices (4 buttons); safer to reject
                // malformed data than crash when displaying buttons.
                throw new JSONException("Expected " + INCORRECT_ANSWER_COUNT + " incorrect answers");
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

    /** Request failed before JSON could be read (network or HTTP error). */
    private static ErrorType errorFromVolley(VolleyError error) {
        // Open Trivia DB returns rate limit as HTTP 429, not JSON with code 5.
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
