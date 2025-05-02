package com.example.flashcardapp; // Kendi paket adınız

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText editTextEmail, editTextPassword;
    TextInputLayout tilEmail, tilPassword; // Yeni
    Button buttonLogin;
    TextView textViewGoToRegister;
    ProgressBar progressBar;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextLoginEmail);
        editTextPassword = findViewById(R.id.editTextLoginPassword);
        tilEmail = findViewById(R.id.tilLoginEmail); // Yeni
        tilPassword = findViewById(R.id.tilLoginPassword); // Yeni
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewGoToRegister = findViewById(R.id.textViewGoToRegister);
        progressBar = findViewById(R.id.progressBarLogin);
        mAuth = FirebaseAuth.getInstance();

        textViewGoToRegister.setOnClickListener(v -> { // Listener aynı
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        buttonLogin.setOnClickListener(v -> { // Listener aynı
            loginUser();
        });
    }

    private void loginUser() {
        // Hataları temizle
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Doğrulamalar ve Hata Gösterme
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("E-posta gerekli."); // Yeni
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Şifre gerekli."); // Yeni
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> { // Lambda
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Giriş başarılı!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Hataları daha anlamlı göster
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen giriş hatası";
                        if (errorMessage.contains("password") || errorMessage.contains("Password")) {
                            tilPassword.setError("Hatalı şifre.");
                            editTextPassword.requestFocus();
                        } else if (errorMessage.contains("user record") || errorMessage.contains("USER_NOT_FOUND")) {
                            tilEmail.setError("Bu e-posta ile kayıtlı kullanıcı bulunamadı.");
                            editTextEmail.requestFocus();
                        } else if (errorMessage.contains("invalid email")) {
                            tilEmail.setError("Geçersiz e-posta formatı.");
                            editTextEmail.requestFocus();
                        }
                        else {
                            Toast.makeText(LoginActivity.this, "Giriş başarısız: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}