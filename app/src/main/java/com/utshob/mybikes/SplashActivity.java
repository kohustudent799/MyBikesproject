package com.utshob.mybikes;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utshob.mybikes.util.Prefs;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Intent next;
            if (user != null && Prefs.rememberMe()) {
                next = new Intent(this, MainActivity.class);       // stay logged in
            } else {
                if (user != null) FirebaseAuth.getInstance().signOut();  // "Remember me" was off
                next = new Intent(this, LoginActivity.class);
            }
            startActivity(next);
            finish();
        }, 1500);
    }
}
