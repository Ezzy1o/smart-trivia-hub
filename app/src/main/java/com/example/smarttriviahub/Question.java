package com.example.smarttriviahub;

import android.text.Html;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model (M dalam MVC): satu soalan trivia beserta jawapan pengguna.
 * Implements Serializable supaya senarai soalan boleh dihantar melalui Intent / Bundle
 * dan disimpan semasa skrin diputar (onSaveInstanceState).
 */
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String questionText;
    private final String correctAnswer;
    private final List<String> options;   // 4 pilihan, sudah di-shuffle
    private String userAnswer;            // null selagi pengguna belum menjawab

    /**
     * @param rawQuestion      teks soalan mentah dari API (mungkin ada &quot; &#039; dan sebagainya)
     * @param rawCorrect       jawapan betul mentah
     * @param rawIncorrectList 3 jawapan salah mentah
     */
    public Question(String rawQuestion, String rawCorrect, List<String> rawIncorrectList) {
        this.questionText = decode(rawQuestion);
        this.correctAnswer = decode(rawCorrect);

        // Gabung 3 jawapan salah + 1 jawapan betul, semuanya didecode dahulu.
        this.options = new ArrayList<>();
        for (String raw : rawIncorrectList) {
            options.add(decode(raw));
        }
        options.add(correctAnswer);

        // API sentiasa letak jawapan betul di tempat yang sama. Shuffle supaya kedudukannya rawak.
        Collections.shuffle(options);
    }

    /** Tukar HTML entities (&quot; &amp; &#039;) kepada aksara biasa. */
    private static String decode(String raw) {
        return Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString();
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public boolean isAnswered() {
        return userAnswer != null;
    }

    public boolean isCorrect() {
        return correctAnswer.equals(userAnswer);
    }
}
