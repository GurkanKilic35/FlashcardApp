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

public class RegisterActivity extends AppCompatActivity {

    EditText editTextEmail, editTextPassword;
    TextInputLayout tilEmail, tilPassword;
    Button buttonRegister;
    TextView textViewGoToLogin;
    ProgressBar progressBar;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextEmail = findViewById(R.id.editTextRegisterEmail);
        editTextPassword = findViewById(R.id.editTextRegisterPassword);
        tilEmail = findViewById(R.id.tilRegisterEmail);
        tilPassword = findViewById(R.id.tilRegisterPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewGoToLogin = findViewById(R.id.textViewGoToLogin);
        progressBar = findViewById(R.id.progressBarRegister);
        mAuth = FirebaseAuth.getInstance();

        textViewGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        buttonRegister.setOnClickListener(v -> {
            registerUser();
        });
    }

    private void registerUser() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Doğrulamalar ve Hata Gösterme
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("E-posta gerekli.");
            tilEmail.requestFocus();
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Şifre gerekli.");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("Şifre en az 6 karakter olmalı.");
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE); // İşlem başlarken ProgressBar'ı göster

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
                        if (errorMessage.contains("email address is already in use")) {
                            tilEmail.setError("Bu e-posta adresi zaten kullanılıyor.");
                            editTextEmail.requestFocus();
                        } else if (errorMessage.contains("The email address is badly formatted.")) {
                            tilEmail.setError("Geçersiz e-posta formatı.");
                            editTextEmail.requestFocus();
                        }
                        else {
                            Toast.makeText(RegisterActivity.this, "Kayıt başarısız: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}