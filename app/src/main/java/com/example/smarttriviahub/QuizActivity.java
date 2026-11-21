package com.example.smarttriviahub;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.BundleCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.databinding.DataBindingUtil;

import com.example.smarttriviahub.databinding.ActivityQuizBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";

    // Keys for onSaveInstanceState / onRestoreInstanceState
    private static final String STATE_QUESTIONS = "state_questions";
    private static final String STATE_INDEX = "state_index";
    private static final String STATE_SCORE = "state_score";
    private static final String STATE_ERROR = "state_error";

    // Used only if Intent has no extras (e.g. opened without going through MainActivity)
    private static final int DEFAULT_CATEGORY_ID = 18;
    private static final String DEFAULT_DIFFICULTY = "medium";

    private ActivityQuizBinding binding;
    private MaterialButton[] optionButtons;

    // Data from MainActivity (via Intent)
    private String name;
    private int categoryId;
    private String categoryName;
    private String difficulty;

    // Quiz state. ALL of these are saved in onSaveInstanceState.
    private ArrayList<Question> questions;     // null until questions are successfully fetched
    private int currentIndex = 0;              // current question index (0-9)
    private int score = 0;
    private QuizApi.ErrorType errorType;       // null if no error

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        EdgeToEdge.enable(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quiz);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        readExtras();

        // Four option buttons collected in an array so they can be processed in a loop.
        optionButtons = new MaterialButton[]{
                binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4};
        for (int i = 0; i < optionButtons.length; i++) {
            final int position = i;   // clicked button position
            optionButtons[i].setOnClickListener(v -> onOptionClicked(position));
        }
        binding.btnNext.setOnClickListener(v -> onNextClicked());
        binding.btnRetry.setOnClickListener(v -> startFetch());

        showLoading();
        if (savedInstanceState == null) {
            // First time activity is opened: fetch questions.
            // If savedInstanceState is NOT null, activity is being recreated (e.g. screen rotated)
            // and onRestoreInstanceState() will restore state without re-fetching.
            startFetch();
        }
    }

    /** Read data sent by MainActivity. */
    private void readExtras() {
        Intent intent = getIntent();
        name = intent.getStringExtra(Constants.EXTRA_NAME);
        categoryId = intent.getIntExtra(Constants.EXTRA_CATEGORY_ID, DEFAULT_CATEGORY_ID);
        categoryName = intent.getStringExtra(Constants.EXTRA_CATEGORY_NAME);
        String extraDifficulty = intent.getStringExtra(Constants.EXTRA_DIFFICULTY);
        difficulty = extraDifficulty != null ? extraDifficulty : DEFAULT_DIFFICULTY;
    }

    // ---------- Three view states: loading / error / quiz ----------

    private void showLoading() {
        binding.progressLoading.setVisibility(View.VISIBLE);
        binding.groupError.setVisibility(View.GONE);
        binding.groupQuiz.setVisibility(View.GONE);
    }

    private void showError(QuizApi.ErrorType type) {
        errorType = type;
        binding.tvErrorMessage.setText(type.getMessageResId());
        binding.progressLoading.setVisibility(View.GONE);
        binding.groupError.setVisibility(View.VISIBLE);
        binding.groupQuiz.setVisibility(View.GONE);
    }

    private void showQuiz() {
        errorType = null;
        binding.progressLoading.setVisibility(View.GONE);
        binding.groupError.setVisibility(View.GONE);
        binding.groupQuiz.setVisibility(View.VISIBLE);
        renderQuestion();
    }

    // ---------- Fetch questions ----------

    private void startFetch() {
        Log.d(TAG, "startFetch: category=" + categoryId + ", difficulty=" + difficulty);
        errorType = null;
        showLoading();

        QuizApi.fetchQuestions(this, categoryId, difficulty, new QuizApi.Callback() {
            @Override
            public void onSuccess(List<Question> result) {
                // If activity is already destroyed (e.g. rotated during loading), ignore result.
                if (isDestroyed()) {
                    return;
                }
                questions = new ArrayList<>(result);
                currentIndex = 0;
                score = 0;
                showQuiz();
            }

            @Override
            public void onError(QuizApi.ErrorType type) {
                if (isDestroyed()) {
                    return;
                }
                showError(type);
            }
        });
    }

    // ---------- Render current question ----------

    /**
     * Redraws the quiz screen based on current state (questions, currentIndex, score).
     * Called after fetch, after answering, after Next, and after rotation,
     * so the UI always matches the data.
     */
    private void renderQuestion() {
        Question question = questions.get(currentIndex);
        int total = questions.size();

        binding.tvQuestionCounter.setText(
                getString(R.string.label_question_counter, currentIndex + 1, total));
        binding.tvScore.setText(getString(R.string.label_score, score));
        binding.progressQuiz.setMax(total);
        binding.progressQuiz.setProgress(currentIndex + 1);
        binding.tvCategory.setText(categoryName);
        binding.tvQuestion.setText(question.getQuestionText());

        renderOptions(question);

        // Feedback icon: neutral until user answers
        if (!question.isAnswered()) {
            binding.ivFeedback.setImageResource(R.drawable.ic_neutral);
        } else if (question.isCorrect()) {
            binding.ivFeedback.setImageResource(R.drawable.ic_correct);
        } else {
            binding.ivFeedback.setImageResource(R.drawable.ic_wrong);
        }

        // Next button only enabled after answering; last question changes its text.
        binding.btnNext.setEnabled(question.isAnswered());
        boolean isLast = currentIndex == total - 1;
        binding.btnNext.setText(isLast ? R.string.btn_see_result : R.string.btn_next);
    }

    private void renderOptions(Question question) {
        List<String> options = question.getOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            MaterialButton button = optionButtons[i];
            String option = options.get(i);
            button.setText(option);
            button.setEnabled(!question.isAnswered());

            if (!question.isAnswered()) {
                // Unanswered: normal appearance
                styleOption(button, R.color.surface, R.color.text_primary, R.color.primary);
            } else if (option.equals(question.getCorrectAnswer())) {
                // Correct answer always green
                styleOption(button, R.color.correct, R.color.on_primary, R.color.correct);
            } else if (option.equals(question.getUserAnswer())) {
                // User's wrong choice turns red
                styleOption(button, R.color.wrong, R.color.on_primary, R.color.wrong);
            } else {
                // Other options dimmed
                styleOption(button, R.color.surface, R.color.text_secondary, R.color.progress_track);
            }
        }
    }

    /** Set background, text, and stroke colors for buttons. All colors from colors.xml. */
    private void styleOption(MaterialButton button, @ColorRes int background,
                             @ColorRes int text, @ColorRes int stroke) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, background)));
        button.setTextColor(ContextCompat.getColor(this, text));
        button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, stroke)));
    }

    // ---------- User answers / next ----------

    private void onOptionClicked(int position) {
        Question question = questions.get(currentIndex);
        if (question.isAnswered()) {
            return;   // already answered, ignore double taps
        }

        question.setUserAnswer(question.getOptions().get(position));
        if (question.isCorrect()) {
            score++;
        }

        renderQuestion();   // redraw: button colors, icon, score, Next button

        String message = question.isCorrect()
                ? getString(R.string.msg_correct)
                : getString(R.string.msg_wrong, question.getCorrectAnswer());
        Snackbar.make(binding.rootQuiz, message, Snackbar.LENGTH_SHORT).show();
    }

    private void onNextClicked() {
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            renderQuestion();
            binding.scrollQuiz.scrollTo(0, 0);   // landscape: scroll back to top for new question
        } else {
            finishQuiz();
        }
    }

    /** Last question answered: send results to ResultActivity. */
    private void finishQuiz() {
        if (isFinishing()) {
            return;   // button double-tapped: only send to Result once
        }
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(Constants.EXTRA_SCORE, score);
        intent.putExtra(Constants.EXTRA_TOTAL, questions.size());
        intent.putExtra(Constants.EXTRA_RESULTS, questions);   // ArrayList<Question> (Serializable)
        intent.putExtra(Constants.EXTRA_NAME, name);
        intent.putExtra(Constants.EXTRA_CATEGORY_ID, categoryId);
        intent.putExtra(Constants.EXTRA_CATEGORY_NAME, categoryName);
        intent.putExtra(Constants.EXTRA_DIFFICULTY, difficulty);
        startActivity(intent);
        finish();   // quiz ended: Back from Result does not return to last question
    }

    // ---------- Rotation: save and restore state ----------

    /**
     * Called before activity is destroyed (including screen rotation).
     * We save all quiz state in the Bundle.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_QUESTIONS, questions);   // including user answers
        outState.putInt(STATE_INDEX, currentIndex);
        outState.putInt(STATE_SCORE, score);
        outState.putSerializable(STATE_ERROR, errorType);
    }

    /**
     * Called after onStart() when activity is recreated with a saved Bundle.
     * Restore state and redraw the same screen, WITHOUT fetching API again.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        questions = (ArrayList<Question>) BundleCompat.getSerializable(
                savedInstanceState, STATE_QUESTIONS, ArrayList.class);
        currentIndex = savedInstanceState.getInt(STATE_INDEX);
        score = savedInstanceState.getInt(STATE_SCORE);
        errorType = BundleCompat.getSerializable(
                savedInstanceState, STATE_ERROR, QuizApi.ErrorType.class);

        if (questions != null) {
            showQuiz();
        } else if (errorType != null) {
            showError(errorType);
        } else {
            // Rotated while still loading: old request lost with old activity,
            // so we need to fetch again.
            startFetch();
        }
    }

    // ---------- Lifecycle ----------

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
