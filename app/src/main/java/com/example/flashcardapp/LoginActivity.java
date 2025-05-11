package com.example.flashcardapp;

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
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText editTextEmail, editTextPassword;
    TextInputLayout tilEmail, tilPassword;
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
        tilEmail = findViewById(R.id.tilLoginEmail);
        tilPassword = findViewById(R.id.tilLoginPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewGoToRegister = findViewById(R.id.textViewGoToRegister);
        progressBar = findViewById(R.id.progressBarLogin);
        mAuth = FirebaseAuth.getInstance();

        textViewGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        buttonLogin.setOnClickListener(v -> {
            loginUser();
        });
    }

    private void loginUser() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Doğrulamalar ve Hata Gösterme
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("E-posta gerekli.");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Şifre gerekli.");
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Giriş başarılı!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen giriş hatası";
                        if (errorMessage.contains("password") || errorMessage.contains("Password")) {
                            tilPassword.setError("Hatalı şifre.");
                            editTextPassword.requestFocus();
                        } else if (errorMessage.contains("The supplied auth credential is incorrect, malformed or has expired") || errorMessage.contains("USER_NOT_FOUND")) {
                            Toast.makeText(LoginActivity.this, "Giriş başarısız. E-posta veya şifre yanlış." , Toast.LENGTH_LONG).show();

                        } else if (errorMessage.contains("The email address is badly formatted")) {
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