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
import androidx.databinding.DataBindingUtil;

import com.example.smarttriviahub.databinding.ActivityMainBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Nama: huruf (termasuk huruf bertanda seperti é) dan ruang sahaja.
    private static final String NAME_PATTERN = "[\\p{L} ]+";

    // Student ID: 8 hingga 12 huruf atau nombor. UBAH di sini jika format sebenar berbeza,
    // dan kemas kini teks error_id_invalid dalam strings.xml.
    private static final String STUDENT_ID_PATTERN = "[A-Za-z0-9]{8,12}";

    // Entri pertama @array/programs ialah petunjuk "Pilih program", bukan program sebenar.
    private static final int PROGRAM_HINT_POSITION = 0;

    // Kedudukan dalam Spinner kategori -> ID kategori Open Trivia DB.
    // Susunan MESTI sama dengan @array/categories: Computers, Mathematics,
    // Science & Nature, General Knowledge.
    private static final int[] CATEGORY_IDS = {18, 19, 17, 9};

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

        // Nilai form (teks, pilihan Spinner, RadioButton) TIDAK perlu disimpan secara manual
        // semasa rotation: Android menyimpannya sendiri kerana setiap widget ada android:id.

        clearErrorOnTyping(binding.tilName, binding.etName);
        clearErrorOnTyping(binding.tilStudentId, binding.etStudentId);
        clearProgramErrorOnSelect();

        binding.btnStart.setOnClickListener(v -> onStartClicked());
    }

    /** Ditekan butang Mula: semak form dahulu, hanya teruskan jika semuanya sah. */
    private void onStartClicked() {
        // Semua semakan dijalankan (bukan berhenti pada ralat pertama) supaya semua medan
        // yang salah dipaparkan serentak.
        boolean nameOk = validateName();
        boolean idOk = validateStudentId();
        boolean programOk = validateProgram();
        boolean difficultyOk = validateDifficulty();

        if (nameOk && idOk && programOk && difficultyOk) {
            startQuiz();
            return;
        }

        // Ada ralat: letak fokus pada medan pertama yang salah (ikut susunan dalam borang).
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
        binding.tilName.setError(null);   // null = padam mesej ralat
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
        // Spinner tiada setError() sendiri, jadi ralat dipasang pada TextView
        // yang sedang dipaparkan oleh Spinner (item yang dipilih).
        View selectedView = binding.spinnerProgram.getSelectedView();
        boolean isHint = binding.spinnerProgram.getSelectedItemPosition() == PROGRAM_HINT_POSITION;

        if (selectedView instanceof TextView) {
            ((TextView) selectedView).setError(isHint ? getString(R.string.error_program_invalid) : null);
        }
        return !isHint;
    }

    private boolean validateDifficulty() {
        // RadioButton ialah TextView juga, jadi boleh setError(). Dalam amalan rb_medium
        // dipilih secara lalai, jadi semakan ini hanya perlindungan tambahan.
        boolean selected = getSelectedDifficulty() != null;
        binding.rbHard.setError(selected ? null : getString(R.string.error_difficulty_invalid));
        return selected;
    }

    /** Baca teks medan input; null-safe dan buang ruang di hujung. */
    private String readText(TextInputEditText editText) {
        Editable text = editText.getText();
        return text == null ? "" : text.toString().trim();
    }

    /** Nilai "difficulty" untuk URL API, atau null jika tiada RadioButton dipilih. */
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

    // ---------- Hantar ke QuizActivity ----------

    private void startQuiz() {
        int categoryPosition = binding.spinnerCategory.getSelectedItemPosition();

        // Bundle = bungkusan pasangan key-value. Key sentiasa dari Constants.
        Bundle extras = new Bundle();
        extras.putString(Constants.EXTRA_NAME, readText(binding.etName));
        extras.putString(Constants.EXTRA_STUDENT_ID, readText(binding.etStudentId));
        extras.putString(Constants.EXTRA_PROGRAM, binding.spinnerProgram.getSelectedItem().toString());
        extras.putInt(Constants.EXTRA_CATEGORY_ID, CATEGORY_IDS[categoryPosition]);
        extras.putString(Constants.EXTRA_CATEGORY_NAME, binding.spinnerCategory.getSelectedItem().toString());
        extras.putString(Constants.EXTRA_DIFFICULTY, getSelectedDifficulty());

        // Explicit Intent: kita namakan kelas sasaran (QuizActivity.class) dengan jelas.
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtras(extras);
        // SINGLE_TOP: jika butang Mula ditekan dua kali dengan pantas, tekanan kedua tidak
        // membuka QuizActivity kedua (yang akan fetch kali kedua dan terkena rate limit).
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    // ---------- Padam ralat bila pengguna membetulkan ----------

    /** Padam mesej ralat sebaik sahaja pengguna mula menaip semula dalam medan itu. */
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

    /** Padam ralat pada Spinner program sebaik sahaja pengguna memilih item lain. */
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
