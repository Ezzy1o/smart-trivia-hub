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
import androidx.databinding.DataBindingUtil;

import com.example.smarttriviahub.databinding.ActivityQuizBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";

    // Key untuk onSaveInstanceState / onRestoreInstanceState
    private static final String STATE_QUESTIONS = "state_questions";
    private static final String STATE_INDEX = "state_index";
    private static final String STATE_SCORE = "state_score";
    private static final String STATE_ERROR = "state_error";

    // Digunakan hanya jika Intent tiada extras (contoh: dibuka tanpa melalui MainActivity)
    private static final int DEFAULT_CATEGORY_ID = 18;
    private static final String DEFAULT_DIFFICULTY = "medium";

    private ActivityQuizBinding binding;
    private MaterialButton[] optionButtons;

    // Data dari MainActivity (melalui Intent)
    private String name;
    private int categoryId;
    private String categoryName;
    private String difficulty;

    // Keadaan kuiz. SEMUA ini disimpan dalam onSaveInstanceState.
    private ArrayList<Question> questions;     // null selagi soalan belum berjaya diambil
    private int currentIndex = 0;              // indeks soalan semasa (0-9)
    private int score = 0;
    private QuizApi.ErrorType errorType;       // null jika tiada ralat

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

        readExtras();

        // Empat butang pilihan dikumpul dalam satu array supaya boleh diproses dengan gelung.
        optionButtons = new MaterialButton[]{
                binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4};
        for (int i = 0; i < optionButtons.length; i++) {
            final int position = i;   // kedudukan butang yang ditekan
            optionButtons[i].setOnClickListener(v -> onOptionClicked(position));
        }
        binding.btnNext.setOnClickListener(v -> onNextClicked());
        binding.btnRetry.setOnClickListener(v -> startFetch());

        showLoading();
        if (savedInstanceState == null) {
            // Kali pertama activity dibuka: ambil soalan.
            // Jika savedInstanceState TIDAK null, activity sedang dicipta semula (contoh: skrin
            // diputar) dan onRestoreInstanceState() akan memulihkan keadaan tanpa fetch semula.
            startFetch();
        }
    }

    /** Baca data yang dihantar oleh MainActivity. */
    private void readExtras() {
        Intent intent = getIntent();
        name = intent.getStringExtra(Constants.EXTRA_NAME);
        categoryId = intent.getIntExtra(Constants.EXTRA_CATEGORY_ID, DEFAULT_CATEGORY_ID);
        categoryName = intent.getStringExtra(Constants.EXTRA_CATEGORY_NAME);
        String extraDifficulty = intent.getStringExtra(Constants.EXTRA_DIFFICULTY);
        difficulty = extraDifficulty != null ? extraDifficulty : DEFAULT_DIFFICULTY;
    }

    // ---------- Tiga keadaan paparan: loading / ralat / kuiz ----------

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

    // ---------- Ambil soalan ----------

    private void startFetch() {
        Log.d(TAG, "startFetch: kategori=" + categoryId + ", tahap=" + difficulty);
        errorType = null;
        showLoading();

        QuizApi.fetchQuestions(this, categoryId, difficulty, new QuizApi.Callback() {
            @Override
            public void onSuccess(List<Question> result) {
                // Jika activity sudah dimusnahkan (contoh: diputar semasa loading), abaikan hasil.
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

    // ---------- Paparkan soalan semasa ----------

    /**
     * Melukis semula skrin kuiz berdasarkan keadaan semasa (questions, currentIndex, score).
     * Dipanggil selepas fetch, selepas menjawab, selepas Seterusnya dan selepas rotation,
     * jadi paparan sentiasa sepadan dengan data.
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

        // Ikon maklum balas: neutral sehingga pengguna menjawab
        if (!question.isAnswered()) {
            binding.ivFeedback.setImageResource(R.drawable.ic_neutral);
        } else if (question.isCorrect()) {
            binding.ivFeedback.setImageResource(R.drawable.ic_correct);
        } else {
            binding.ivFeedback.setImageResource(R.drawable.ic_wrong);
        }

        // Butang Seterusnya hanya aktif selepas menjawab; soalan terakhir tukar teksnya.
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
                // Belum dijawab: rupa biasa
                styleOption(button, R.color.surface, R.color.text_primary, R.color.primary);
            } else if (option.equals(question.getCorrectAnswer())) {
                // Jawapan betul sentiasa hijau
                styleOption(button, R.color.correct, R.color.on_primary, R.color.correct);
            } else if (option.equals(question.getUserAnswer())) {
                // Pilihan pengguna yang salah jadi merah
                styleOption(button, R.color.wrong, R.color.on_primary, R.color.wrong);
            } else {
                // Pilihan lain dipudarkan
                styleOption(button, R.color.surface, R.color.text_secondary, R.color.progress_track);
            }
        }
    }

    /** Tetapkan warna latar, teks dan garisan butang. Semua warna dari colors.xml. */
    private void styleOption(MaterialButton button, @ColorRes int background,
                             @ColorRes int text, @ColorRes int stroke) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, background)));
        button.setTextColor(ContextCompat.getColor(this, text));
        button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, stroke)));
    }

    // ---------- Pengguna menjawab / seterusnya ----------

    private void onOptionClicked(int position) {
        Question question = questions.get(currentIndex);
        if (question.isAnswered()) {
            return;   // sudah dijawab, abaikan tekanan berganda
        }

        question.setUserAnswer(question.getOptions().get(position));
        if (question.isCorrect()) {
            score++;
        }

        renderQuestion();   // lukis semula: warna butang, ikon, skor, butang Seterusnya

        String message = question.isCorrect()
                ? getString(R.string.msg_correct)
                : getString(R.string.msg_wrong, question.getCorrectAnswer());
        Snackbar.make(binding.rootQuiz, message, Snackbar.LENGTH_SHORT).show();
    }

    private void onNextClicked() {
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            renderQuestion();
            binding.scrollQuiz.scrollTo(0, 0);   // landscape: kembali ke atas untuk soalan baharu
        } else {
            finishQuiz();
        }
    }

    /** Soalan terakhir dijawab: hantar keputusan ke ResultActivity. */
    private void finishQuiz() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(Constants.EXTRA_SCORE, score);
        intent.putExtra(Constants.EXTRA_TOTAL, questions.size());
        intent.putExtra(Constants.EXTRA_RESULTS, questions);   // ArrayList<Question> (Serializable)
        intent.putExtra(Constants.EXTRA_NAME, name);
        intent.putExtra(Constants.EXTRA_CATEGORY_ID, categoryId);
        intent.putExtra(Constants.EXTRA_CATEGORY_NAME, categoryName);
        intent.putExtra(Constants.EXTRA_DIFFICULTY, difficulty);
        startActivity(intent);
        finish();   // kuiz sudah tamat: butang Back dari Result tidak kembali ke soalan terakhir
    }

    // ---------- Rotation: simpan dan pulihkan keadaan ----------

    /**
     * Dipanggil sebelum activity dimusnahkan (termasuk semasa skrin diputar).
     * Kita simpan semua keadaan kuiz dalam Bundle.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_QUESTIONS, questions);   // termasuk jawapan pengguna
        outState.putInt(STATE_INDEX, currentIndex);
        outState.putInt(STATE_SCORE, score);
        outState.putSerializable(STATE_ERROR, errorType);
    }

    /**
     * Dipanggil selepas onStart() apabila activity dicipta semula dengan Bundle yang disimpan.
     * Pulihkan keadaan dan lukis semula skrin yang sama, TANPA fetch API.
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
            // Diputar semasa masih loading: permintaan lama hilang bersama activity lama,
            // jadi kita perlu ambil semula.
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
