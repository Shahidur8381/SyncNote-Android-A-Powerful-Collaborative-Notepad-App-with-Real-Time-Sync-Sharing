package com.example.syncnote;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.syncnote.firebase.FirebaseManager;
import com.example.syncnote.models.UserModel;
import com.example.syncnote.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameInput, passwordInput;
    private TextView errorLabel;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseManager = FirebaseManager.getInstance();
        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        errorLabel = findViewById(R.id.errorLabel);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        findViewById(R.id.registerLink).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        findViewById(R.id.forgotPasswordLink).setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void attemptLogin() {
        String username = getText(usernameInput);
        String password = getText(passwordInput);

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        showLoading(true);
        errorLabel.setVisibility(View.GONE);

        firebaseManager.authenticateUser(username, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(UserModel user) {
                runOnUiThread(() -> {
                    showLoading(false);
                    sessionManager.login(user.getId(), user.getUsername(), user.getEmail());
                    Toast.makeText(LoginActivity.this,
                            "Welcome back, " + user.getUsername() + "!", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(error);
                    passwordInput.setText("");
                });
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
        loginButton.setEnabled(!show);
    }
}
