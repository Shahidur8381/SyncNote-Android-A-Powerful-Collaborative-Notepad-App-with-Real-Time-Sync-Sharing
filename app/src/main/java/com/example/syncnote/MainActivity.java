package com.example.syncnote;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.syncnote.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // 1.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force light mode - no dark mode allowed
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler().postDelayed(() -> {
            SessionManager sessionManager = SessionManager.getInstance(this);
            
            Intent intent;
            if (sessionManager.isLoggedIn()) {
                // User is logged in, go to Home
                intent = new Intent(MainActivity.this, HomeActivity.class);
            } else {
                // User is not logged in, go to Login
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }
            
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}




