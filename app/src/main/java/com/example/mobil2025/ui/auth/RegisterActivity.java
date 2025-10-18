package com.example.mobil2025.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobil2025.R;
import com.example.mobil2025.data.auth.FirebaseAuthManager;
import com.example.mobil2025.data.repo.UserRepository;
import com.example.mobil2025.util.Validators;
import com.google.firebase.auth.FirebaseUser;

/**
 * Registracija:
 * - Kreira Firebase Auth nalog
 * - Salje verifikacioni email (aktivacioni link)
 * - Upisuje profil u Firestore (enabled=false + activationDeadline u UserRepository)
 * Napomena: Login/odjava radi drugi tim.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etPasswordConfirm, etUsername;
    private Button btnRegister;
    private ProgressBar progress;

    // Avatari (ImageView dugmad)
    private ImageView ivFox, ivTurtle, ivLion, ivCat, ivPanda;
    private String selectedAvatarKey = "avatar_fox"; // podrazumevano

    private FirebaseAuthManager authManager;
    private UserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = new FirebaseAuthManager();
        userRepo = new UserRepository();

        // Polja forme
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        etUsername = findViewById(R.id.etUsername);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);

        // Avatari
        ivFox = findViewById(R.id.avatar_fox);
        ivTurtle = findViewById(R.id.avatar_turtle);
        ivLion = findViewById(R.id.avatar_lion);
        ivCat = findViewById(R.id.avatar_cat);
        ivPanda = findViewById(R.id.avatar_panda);

        // Klik listener za izbor avatara (setSelected menja okvir preko avatar_border.xml)
        View.OnClickListener avatarClick = v -> {
            clearSelections();
            v.setSelected(true);
            int id = v.getId();
            if (id == R.id.avatar_fox)         selectedAvatarKey = "avatar_fox";
            else if (id == R.id.avatar_turtle)  selectedAvatarKey = "avatar_turtle";
            else if (id == R.id.avatar_lion)    selectedAvatarKey = "avatar_lion";
            else if (id == R.id.avatar_cat)     selectedAvatarKey = "avatar_cat";
            else if (id == R.id.avatar_panda)   selectedAvatarKey = "avatar_panda";
        };

        ivFox.setOnClickListener(avatarClick);
        ivTurtle.setOnClickListener(avatarClick);
        ivLion.setOnClickListener(avatarClick);
        ivCat.setOnClickListener(avatarClick);
        ivPanda.setOnClickListener(avatarClick);

        // Default selekcija (vizuelno obeleži lisicu)
        ivFox.setSelected(true);

        btnRegister.setOnClickListener(v -> onRegister());
    }

    private void clearSelections() {
        ivFox.setSelected(false);
        ivTurtle.setSelected(false);
        ivLion.setSelected(false);
        ivCat.setSelected(false);
        ivPanda.setSelected(false);
    }

    private void onRegister() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString();
        String pass2 = etPasswordConfirm.getText().toString();
        String username = etUsername.getText().toString().trim();

        // Validacija
        if (!Validators.isEmailValid(email)) { etEmail.setError("Neispravan email"); return; }
        if (!Validators.isPasswordValid(pass)) { etPassword.setError("Min 6 karaktera"); return; }
        if (!Validators.doPasswordsMatch(pass, pass2)) { etPasswordConfirm.setError("Lozinke se ne poklapaju"); return; }
        if (!Validators.isUsernameValid(username)) { etUsername.setError("3-20, slova/brojevi ._-"); return; }
        if (selectedAvatarKey == null || selectedAvatarKey.isEmpty()) { toast("Izaberi avatar"); return; }

        setLoading(true);

        // 1) Kreiraj Auth nalog
        authManager.createUser(email, pass, result -> {
            FirebaseUser fu = result.getUser();
            if (fu == null) {
                setLoading(false);
                toast("Neočekovana greška");
                return;
            }

            // 2) Pošalji verifikacioni email sa aktivacionim linkom
            fu.sendEmailVerification()
                    .addOnSuccessListener(a -> {
                        // (opciono) toast("Poslali smo verifikacioni email.");
                    })
                    .addOnFailureListener(e -> {
                        toast("Nije poslata verifikacija: " + e.getMessage());
                    });

            // 3) Upisi profil u Firestore (repo: enabled=false + activationDeadline=24h)
            userRepo.createUserProfileWithUniqueUsername(
                    fu.getUid(),
                    email,
                    username,
                    selectedAvatarKey,
                    aVoid -> {
                        setLoading(false);
                        toast("Registracija uspešna! Proveri email i aktiviraj nalog (link važi 24h).");
                        finish(); // zatvori registraciju; login radi drugi tim
                    },
                    e -> {
                        setLoading(false);
                        toast(e.getMessage());
                        // Rollback Auth naloga ako Firestore upis nije uspeo
                        FirebaseUser cur = authManager.currentUser();
                        if (cur != null) cur.delete();
                    }
            );
        }, e -> {
            setLoading(false);
            toast(e.getMessage());
        });
    }

    private void setLoading(boolean l) {
        progress.setVisibility(l ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!l);
        etEmail.setEnabled(!l);
        etPassword.setEnabled(!l);
        etPasswordConfirm.setEnabled(!l);
        etUsername.setEnabled(!l);
        ivFox.setEnabled(!l);
        ivTurtle.setEnabled(!l);
        ivLion.setEnabled(!l);
        ivCat.setEnabled(!l);
        ivPanda.setEnabled(!l);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
