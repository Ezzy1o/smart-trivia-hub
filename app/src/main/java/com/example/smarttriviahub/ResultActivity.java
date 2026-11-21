package com.example.smarttriviahub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.databinding.DataBindingUtil;

import com.example.smarttriviahub.databinding.ActivityResultBinding;
import com.example.smarttriviahub.databinding.ItemBreakdownBinding;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    // Percentage thresholds for performance labels
    private static final int EXCELLENT_MIN_PERCENT = 80;
    private static final int GOOD_MIN_PERCENT = 50;

    private ActivityResultBinding binding;

    // Data from QuizActivity. Saved so the "New Quiz" button can pass them again.
    private String name;
    private int categoryId;
    private String categoryName;
    private String difficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        EdgeToEdge.enable(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        // All data comes from Intent, and Intent persists across rotation. Thus the entire screen
        // is rebuilt from extras on every onCreate: no other state needs to be saved.
        Intent intent = getIntent();
        int score = intent.getIntExtra(Constants.EXTRA_SCORE, 0);
        int total = intent.getIntExtra(Constants.EXTRA_TOTAL, 0);
        name = intent.getStringExtra(Constants.EXTRA_NAME);
        categoryId = intent.getIntExtra(Constants.EXTRA_CATEGORY_ID, 0);
        categoryName = intent.getStringExtra(Constants.EXTRA_CATEGORY_NAME);
        difficulty = intent.getStringExtra(Constants.EXTRA_DIFFICULTY);

        @SuppressWarnings("unchecked")
        ArrayList<Question> results = IntentCompat.getSerializableExtra(
                intent, Constants.EXTRA_RESULTS, ArrayList.class);
        if (results == null) {
            results = new ArrayList<>();   // Intent without extras: show empty screen, not crash
        }

        showSummary(score, total);
        showBreakdown(results);

        binding.btnTryAgain.setOnClickListener(v -> tryAgain());
        binding.btnRestart.setOnClickListener(v -> backToMain());
    }

    // ---------- Summary ----------

    private void showSummary(int score, int total) {
        // Multiply by 100 FIRST, then divide: 8 * 100 / 10 = 80.
        // Check total > 0 to avoid division by zero.
        int percentage = total > 0 ? score * 100 / total : 0;

        binding.tvGreeting.setText(getString(R.string.label_greeting, name != null ? name : ""));
        binding.tvPercentage.setText(getString(R.string.label_percentage, percentage));
        binding.tvCorrectCount.setText(getString(R.string.label_correct_count, score));
        binding.tvWrongCount.setText(getString(R.string.label_wrong_count, total - score));
        binding.tvPerformanceLabel.setText(performanceLabelFor(percentage));
    }

    private int performanceLabelFor(int percentage) {
        if (percentage >= EXCELLENT_MIN_PERCENT) {
            return R.string.perf_excellent;
        } else if (percentage >= GOOD_MIN_PERCENT) {
            return R.string.perf_good;
        }
        return R.string.perf_needs_revision;
    }

    // ---------- Breakdown ----------

    /** Add a single item_breakdown row to layout_breakdown for each question. */
    private void showBreakdown(ArrayList<Question> results) {
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < results.size(); i++) {
            Question question = results.get(i);

            ItemBreakdownBinding item = ItemBreakdownBinding.inflate(
                    inflater, binding.layoutBreakdown, false);

            item.tvItemNumber.setText(String.valueOf(i + 1));
            item.tvItemQuestion.setText(question.getQuestionText());
            item.tvItemYourAnswer.setText(
                    getString(R.string.label_your_answer, question.getUserAnswer()));
            item.tvItemCorrectAnswer.setText(
                    getString(R.string.label_correct_answer, question.getCorrectAnswer()));

            if (question.isCorrect()) {
                item.ivItemStatus.setImageResource(R.drawable.ic_correct);
            } else {
                item.ivItemStatus.setImageResource(R.drawable.ic_wrong);
                // Incorrect user answer displayed in red
                item.tvItemYourAnswer.setTextColor(ContextCompat.getColor(this, R.color.wrong));
            }

            binding.layoutBreakdown.addView(item.getRoot());
        }
    }

    // ---------- Buttons ----------

    /** New quiz with the same name, category, and difficulty (new questions fetched). */
    private void tryAgain() {
        if (isFinishing()) {
            return;   // already transitioning: ignore double taps
        }
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra(Constants.EXTRA_NAME, name);
        intent.putExtra(Constants.EXTRA_CATEGORY_ID, categoryId);
        intent.putExtra(Constants.EXTRA_CATEGORY_NAME, categoryName);
        intent.putExtra(Constants.EXTRA_DIFFICULTY, difficulty);
        startActivity(intent);
        finish();   // discard old result: Back from new quiz does not return here
    }

    /** Return to MainActivity (Main Menu) and clear all screens above it from back stack. */
    private void backToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
