package com.example.syncnote;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.syncnote.firebase.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText usernameInput, emailInput, passwordInput, confirmPasswordInput, securityAnswerInput;
    private AutoCompleteTextView securityQuestionDropdown;
    private TextView errorLabel;
    private MaterialButton registerButton;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseManager = FirebaseManager.getInstance();
        initViews();
        setupSecurityQuestions();
        setupClickListeners();
    }

    private void initViews() {
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        securityQuestionDropdown = findViewById(R.id.securityQuestionDropdown);
        securityAnswerInput = findViewById(R.id.securityAnswerInput);
        errorLabel = findViewById(R.id.errorLabel);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupSecurityQuestions() {
        String[] questions = getResources().getStringArray(R.array.security_questions);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, questions);
        securityQuestionDropdown.setAdapter(adapter);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> attemptRegister());

        findViewById(R.id.loginLink).setOnClickListener(v -> {
            finish();
        });
    }

    private void attemptRegister() {
        String username = getText(usernameInput);
        String email = getText(emailInput);
        String password = getText(passwordInput);
        String confirmPassword = getText(confirmPasswordInput);
        String securityQuestion = securityQuestionDropdown.getText().toString().trim();
        String securityAnswer = getText(securityAnswerInput);

        // Validation
        if (!validateInputs(username, email, password, confirmPassword, securityQuestion, securityAnswer)) {
            return;
        }

        showLoading(true);
        errorLabel.setVisibility(View.GONE);

        firebaseManager.registerUser(username, email, password, securityQuestion, securityAnswer,
                new FirebaseManager.RegisterCallback() {
                    @Override
                    public void onSuccess(String userId) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(RegisterActivity.this,
                                    "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            showError(error);
                        });
                    }
                });
    }

    private boolean validateInputs(String username, String email, String password,
                                   String confirmPassword, String securityQuestion, String securityAnswer) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() ||
                confirmPassword.isEmpty() || securityQuestion.isEmpty() || securityAnswer.isEmpty()) {
            showError("Please fill in all fields");
            return false;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return false;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return false;
        }

        if (securityAnswer.length() < 2) {
            showError("Security answer must be at least 2 characters");
            return false;
        }

        return true;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!show);
    }
}
