package com.utshob.mybikes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.utshob.mybikes.util.Prefs;
import com.utshob.mybikes.util.Ui;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private MaterialButton btnRegister;
    private ProgressBar progress;
    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        root = findViewById(R.id.registerRoot);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);
        TextView tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> register());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void register() {
        String name = Ui.str(etName);
        String email = Ui.str(etEmail);
        String pass = etPassword.getText() == null ? "" : etPassword.getText().toString();
        String confirm = etConfirm.getText() == null ? "" : etConfirm.getText().toString();

        if (name.isEmpty()) { Ui.err(etName, "Enter your name"); return; }
        if (email.isEmpty()) { Ui.err(etEmail, "Enter your email"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { Ui.err(etEmail, "Enter a valid email"); return; }
        if (pass.length() < 6) { Ui.err(etPassword, "Password must be at least 6 characters"); return; }
        if (!pass.equals(confirm)) { Ui.err(etConfirm, "Passwords do not match"); return; }

        busy(true);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    FirebaseUser u = r.getUser();
                    if (u == null) { busy(false); return; }
                    Map<String, Object> p = new HashMap<>();
                    p.put("name", name);
                    p.put("email", email);
                    FirebaseDatabase.getInstance().getReference("users").child(u.getUid()).child("profile").updateChildren(p);
                    u.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build())
                            .addOnCompleteListener(t -> {
                                Prefs.setRememberMe(true);
                                Prefs.setSavedEmail(email);
                                startActivity(new Intent(this, MainActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    busy(false);
                    Ui.snack(root, "Registration failed: " + e.getMessage());
                });
    }

    private void busy(boolean b) {
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!b);
    }
}
