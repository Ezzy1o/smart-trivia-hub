package com.example.smarttriviahub;

import android.text.Html;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model (M in MVC): a single trivia question along with user answers.
 * Implements Serializable so the list of questions can be passed via Intent / Bundle
 * and saved during screen rotation (onSaveInstanceState).
 */
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String questionText;
    private final String correctAnswer;
    private final List<String> options;   // 4 choices, already shuffled
    private String userAnswer;            // null until user answers

    /**
     * @param rawQuestion      raw question text from API (may contain &quot; &#039; etc.)
     * @param rawCorrect       raw correct answer
     * @param rawIncorrectList 3 raw incorrect answers
     */
    public Question(String rawQuestion, String rawCorrect, List<String> rawIncorrectList) {
        this.questionText = decode(rawQuestion);
        this.correctAnswer = decode(rawCorrect);

        // Combine 3 incorrect answers + 1 correct answer, all decoded beforehand.
        this.options = new ArrayList<>();
        for (String raw : rawIncorrectList) {
            options.add(decode(raw));
        }
        options.add(correctAnswer);

        // API always puts the correct answer in the same place. Shuffle so position is random.
        Collections.shuffle(options);
    }

    /** Convert HTML entities (&quot; &amp; &#039;) to normal characters. */
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
