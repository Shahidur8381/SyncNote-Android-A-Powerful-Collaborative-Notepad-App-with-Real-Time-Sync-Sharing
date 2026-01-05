package com.example.syncnote;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.syncnote.firebase.FirebaseManager;
import com.example.syncnote.models.UserModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText usernameInput, securityAnswerInput, newPasswordInput, confirmPasswordInput;
    private TextView securityQuestionLabel, errorLabel, successLabel;
    private LinearLayout step1Layout, step2Layout, step3Layout;
    private MaterialButton nextButton;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;

    private int currentStep = 1;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        firebaseManager = FirebaseManager.getInstance();
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        usernameInput = findViewById(R.id.usernameInput);
        securityAnswerInput = findViewById(R.id.securityAnswerInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        securityQuestionLabel = findViewById(R.id.securityQuestionLabel);
        errorLabel = findViewById(R.id.errorLabel);
        successLabel = findViewById(R.id.successLabel);
        step1Layout = findViewById(R.id.step1Layout);
        step2Layout = findViewById(R.id.step2Layout);
        step3Layout = findViewById(R.id.step3Layout);
        nextButton = findViewById(R.id.nextButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        nextButton.setOnClickListener(v -> handleNextStep());
    }

    private void handleNextStep() {
        hideMessages();

        switch (currentStep) {
            case 1:
                verifyUsername();
                break;
            case 2:
                verifySecurityAnswer();
                break;
            case 3:
                resetPassword();
                break;
        }
    }

    private void verifyUsername() {
        username = getText(usernameInput);

        if (username.isEmpty()) {
            showError("Please enter your username");
            return;
        }

        showLoading(true);

        firebaseManager.getUserByUsername(username, new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(UserModel user) {
                runOnUiThread(() -> {
                    showLoading(false);

                    if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isEmpty()) {
                        showError("No security question set for this account");
                        return;
                    }

                    // Show security question
                    securityQuestionLabel.setText(user.getSecurityQuestion());
                    usernameInput.setEnabled(false);
                    step2Layout.setVisibility(View.VISIBLE);
                    nextButton.setText("Verify Answer");
                    currentStep = 2;
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Username not found");
                });
            }
        });
    }

    private void verifySecurityAnswer() {
        String answer = getText(securityAnswerInput);

        if (answer.isEmpty()) {
            showError("Please enter your security answer");
            return;
        }

        showLoading(true);

        firebaseManager.verifySecurityAnswer(username, answer, result -> {
            runOnUiThread(() -> {
                showLoading(false);

                if (result) {
                    // Show new password fields
                    securityAnswerInput.setEnabled(false);
                    step3Layout.setVisibility(View.VISIBLE);
                    nextButton.setText("Reset Password");
                    currentStep = 3;
                } else {
                    showError("Incorrect security answer");
                    securityAnswerInput.setText("");
                }
            });
        });
    }

    private void resetPassword() {
        String newPassword = getText(newPasswordInput);
        String confirmPassword = getText(confirmPasswordInput);

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        showLoading(true);

        firebaseManager.updatePassword(username, newPassword, result -> {
            runOnUiThread(() -> {
                showLoading(false);

                if (result) {
                    showSuccess("Password reset successful! Redirecting to login...");
                    nextButton.setEnabled(false);

                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
                } else {
                    showError("Failed to reset password. Please try again.");
                }
            });
        });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void hideMessages() {
        errorLabel.setVisibility(View.GONE);
        successLabel.setVisibility(View.GONE);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisibility(View.VISIBLE);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        nextButton.setEnabled(!show);
    }
}
