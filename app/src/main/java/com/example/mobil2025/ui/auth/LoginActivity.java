package com.example.mobil2025.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobil2025.MainActivity;
import com.example.mobil2025.R;
import com.example.mobil2025.data.auth.FirebaseAuthManager;
import com.example.mobil2025.ui.profile.ProfileActivity;
import com.example.mobil2025.util.AuthErrorUtils;
import com.example.mobil2025.util.Validators;
import com.google.firebase.auth.FirebaseAuth;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private ProgressBar progress;

    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new FirebaseAuthManager();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        progress = findViewById(R.id.progress);

        btnLogin.setOnClickListener(v -> loginUser());
        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if(email.isEmpty()) { etEmail.setError("Unesi email"); return; }

        if(password.isEmpty()) { etPassword.setError("Unesi lozinku"); return; }

        if (!Validators.isLoginInputValid(email, password)) {
            if (!Validators.isEmailValid(email)) {
                etEmail.setError("Neispravan format email-a");
            }
            if (!Validators.isPasswordValid(password)) {
                etPassword.setError("Lozinka mora imati najmanje 6 karaktera");
            }
            return; // prekini prije Firebase poziva
        }

        // sad se moze pokusati prijava na Firebase
        setLoading(true);

        authManager.signIn(email, password, result -> {
            setLoading(false);
            Toast.makeText(this, "Prijava uspešna", Toast.LENGTH_SHORT).show();
            // Prelazak na glavni ekran
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        }, e -> {
            setLoading(false);

            // ne otkrivamo da li nalog postoji - bolje za bezbijednost u slucaju napada - to je privacyMode = true kao drugi parametar
            AuthErrorUtils.UiHint hint = AuthErrorUtils.fromException(e, true);
            switch (hint.target) {
                case EMAIL:    etEmail.setError(hint.message); break;
                case PASSWORD: etPassword.setError(hint.message); break;
                case NONE: break;
            }

            Toast.makeText(this, hint.message, Toast.LENGTH_LONG).show();
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    @Override
    protected void onStart(){
        super.onStart();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if(auth.getCurrentUser() != null) {
            // Vec ulogovan - preskoci login
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent); // pokreni MainActivity iz ove LoginActivity
        }
    }




}
