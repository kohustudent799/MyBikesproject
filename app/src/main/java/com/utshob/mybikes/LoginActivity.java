package com.utshob.mybikes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.utshob.mybikes.util.Prefs;
import com.utshob.mybikes.util.Ui;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;
    private TextInputEditText etEmail, etPassword;
    private CheckBox cbRemember;
    private MaterialButton btnLogin, btnGoogle;
    private ProgressBar progress;
    private View root;

    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    busy(false);
                    Ui.snack(root, "Google sign-in failed (code " + e.getStatusCode() + "). Check SHA-1 and google-services.json.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();
        root = findViewById(R.id.loginRoot);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRemember = findViewById(R.id.cbRemember);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        progress = findViewById(R.id.progress);
        TextView tvRegister = findViewById(R.id.tvRegister);

        cbRemember.setChecked(Prefs.rememberMe());
        etEmail.setText(Prefs.savedEmail());

        // default_web_client_id is generated from google-services.json (only when Google sign-in is
        // enabled in Firebase and a SHA-1 is added). Looking it up by name keeps the build working
        // even before that is done; the Google button then shows a helpful message instead.
        int webClientRes = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (webClientRes != 0) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(webClientRes))
                    .requestEmail()
                    .build();
            googleClient = GoogleSignIn.getClient(this, gso);
        }

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogle.setOnClickListener(v -> {
            if (googleClient == null) {
                Ui.snack(root, "Google Sign-In is not set up: enable Google in Firebase, add your SHA-1, and re-download google-services.json.");
                return;
            }
            busy(true);
            googleClient.signOut().addOnCompleteListener(t ->      // lets the user pick an account every time
                    googleLauncher.launch(googleClient.getSignInIntent()));
        });
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginWithEmail() {
        String email = Ui.str(etEmail);
        String pass = etPassword.getText() == null ? "" : etPassword.getText().toString();
        if (email.isEmpty()) { Ui.err(etEmail, "Enter your email"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { Ui.err(etEmail, "Enter a valid email"); return; }
        if (pass.isEmpty()) { Ui.err(etPassword, "Enter your password"); return; }
        busy(true);
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> goMain())
                .addOnFailureListener(e -> {
                    busy(false);
                    Ui.snack(root, "Login failed: " + e.getMessage());
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(cred)       // creates the Firebase user automatically for new people
                .addOnSuccessListener(r -> {
                    saveProfile();
                    goMain();
                })
                .addOnFailureListener(e -> {
                    busy(false);
                    Ui.snack(root, "Sign-in failed: " + e.getMessage());
                });
    }

    private void saveProfile() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) return;
        Map<String, Object> p = new HashMap<>();
        p.put("name", u.getDisplayName() == null ? "" : u.getDisplayName());
        p.put("email", u.getEmail() == null ? "" : u.getEmail());
        FirebaseDatabase.getInstance().getReference("users").child(u.getUid()).child("profile").updateChildren(p);
    }

    private void goMain() {
        Prefs.setRememberMe(cbRemember.isChecked());
        Prefs.setSavedEmail(cbRemember.isChecked() ? Ui.str(etEmail) : "");
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void busy(boolean b) {
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!b);
        btnGoogle.setEnabled(!b);
    }
}
