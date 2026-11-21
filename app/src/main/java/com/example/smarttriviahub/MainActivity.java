package com.example.smarttriviahub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.databinding.DataBindingUtil;

import com.example.smarttriviahub.databinding.ActivityMainBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Name: letters (including accented letters like é) and spaces only.
    private static final String NAME_PATTERN = "[\\p{L} ]+";

    // Student ID: 8 to 12 letters or numbers. Change here if the actual format differs,
    // and update error_id_invalid text in strings.xml.
    private static final String STUDENT_ID_PATTERN = "[A-Za-z0-9]{8,12}";

    // First entry of @array/programs is the hint "Select program", not an actual program.
    private static final int PROGRAM_HINT_POSITION = 0;

    // Spinner category position -> Open Trivia DB category ID.
    // Order MUST match @array/categories: Computers, Mathematics, Science & Nature, General Knowledge,
    // Books, Film, Music, Musicals & Theatres, Television, Video Games, Board Games, Comics,
    // Japanese Anime & Manga, Cartoon & Animations.
    private static final int[] CATEGORY_IDS = {18, 19, 17, 9, 10, 11, 12, 13, 14, 15, 16, 29, 31, 32};

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        EdgeToEdge.enable(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        // Form values (text, Spinner selection, RadioButton) do NOT need manual saving
        // during rotation: Android saves them automatically because every widget has an android:id.

        clearErrorOnTyping(binding.tilName, binding.etName);
        clearErrorOnTyping(binding.tilStudentId, binding.etStudentId);
        clearProgramErrorOnSelect();

        binding.btnStart.setOnClickListener(v -> onStartClicked());
    }

    /** Start button pressed: validate form first, only proceed if everything is valid. */
    private void onStartClicked() {
        // Run all validations (do not stop at the first error) so all invalid fields
        // are displayed simultaneously.
        boolean nameOk = validateName();
        boolean idOk = validateStudentId();
        boolean programOk = validateProgram();
        boolean difficultyOk = validateDifficulty();

        if (nameOk && idOk && programOk && difficultyOk) {
            startQuiz();
            return;
        }

        // Error found: focus on the first invalid field (in form order).
        if (!nameOk) {
            binding.etName.requestFocus();
        } else if (!idOk) {
            binding.etStudentId.requestFocus();
        } else if (!programOk) {
            binding.spinnerProgram.requestFocus();
        } else {
            binding.rgDifficulty.requestFocus();
        }
    }

    // ---------- Validation ----------

    private boolean validateName() {
        String name = readText(binding.etName);
        if (name.isEmpty()) {
            binding.tilName.setError(getString(R.string.error_name_empty));
            return false;
        }
        if (!name.matches(NAME_PATTERN)) {
            binding.tilName.setError(getString(R.string.error_name_invalid));
            return false;
        }
        binding.tilName.setError(null);   // null = clear error message
        return true;
    }

    private boolean validateStudentId() {
        String studentId = readText(binding.etStudentId);
        if (studentId.isEmpty()) {
            binding.tilStudentId.setError(getString(R.string.error_id_empty));
            return false;
        }
        if (!studentId.matches(STUDENT_ID_PATTERN)) {
            binding.tilStudentId.setError(getString(R.string.error_id_invalid));
            return false;
        }
        binding.tilStudentId.setError(null);
        return true;
    }

    private boolean validateProgram() {
        // Spinner has no setError() of its own, so the error is set on the TextView
        // currently displayed by the Spinner (the selected item).
        View selectedView = binding.spinnerProgram.getSelectedView();
        boolean isHint = binding.spinnerProgram.getSelectedItemPosition() == PROGRAM_HINT_POSITION;

        if (selectedView instanceof TextView) {
            ((TextView) selectedView).setError(isHint ? getString(R.string.error_program_invalid) : null);
        }
        return !isHint;
    }

    private boolean validateDifficulty() {
        // RadioButton is also a TextView, so setError() can be called. In practice rb_medium
        // is selected by default, so this check is extra protection.
        boolean selected = getSelectedDifficulty() != null;
        binding.rbHard.setError(selected ? null : getString(R.string.error_difficulty_invalid));
        return selected;
    }

    /** Read input field text; null-safe and trimmed. */
    private String readText(TextInputEditText editText) {
        Editable text = editText.getText();
        return text == null ? "" : text.toString().trim();
    }

    /** "difficulty" value for API URL, or null if no RadioButton is selected. */
    private String getSelectedDifficulty() {
        int checkedId = binding.rgDifficulty.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_easy) {
            return "easy";
        } else if (checkedId == R.id.rb_medium) {
            return "medium";
        } else if (checkedId == R.id.rb_hard) {
            return "hard";
        }
        return null;
    }

    // ---------- Send to QuizActivity ----------

    private void startQuiz() {
        int categoryPosition = binding.spinnerCategory.getSelectedItemPosition();

        // Bundle = container of key-value pairs. Keys always from Constants.
        Bundle extras = new Bundle();
        extras.putString(Constants.EXTRA_NAME, readText(binding.etName));
        extras.putString(Constants.EXTRA_STUDENT_ID, readText(binding.etStudentId));
        extras.putString(Constants.EXTRA_PROGRAM, binding.spinnerProgram.getSelectedItem().toString());
        extras.putInt(Constants.EXTRA_CATEGORY_ID, CATEGORY_IDS[categoryPosition]);
        extras.putString(Constants.EXTRA_CATEGORY_NAME, binding.spinnerCategory.getSelectedItem().toString());
        extras.putString(Constants.EXTRA_DIFFICULTY, getSelectedDifficulty());

        // Explicit Intent: name target class (QuizActivity.class) clearly.
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtras(extras);
        // SINGLE_TOP: if Start button is double-tapped quickly, the second tap does not
        // open a second QuizActivity (which would fetch again and hit rate limit).
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    // ---------- Clear error when user corrects input ----------

    /** Clear error message as soon as the user starts typing in that field. */
    private void clearErrorOnTyping(TextInputLayout layout, TextInputEditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /** Clear error on program Spinner as soon as the user selects another item. */
    private void clearProgramErrorOnSelect() {
        binding.spinnerProgram.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TextView) {
                    ((TextView) view).setError(null);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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
