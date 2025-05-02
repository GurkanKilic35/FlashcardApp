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

public class RegisterActivity extends AppCompatActivity {

    // EditText referansları kalabilir (metni almak için)
    EditText editTextEmail, editTextPassword;
    // TextInputLayout referansları (hata göstermek için)
    TextInputLayout tilEmail, tilPassword;
    Button buttonRegister;
    TextView textViewGoToLogin;
    ProgressBar progressBar;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ID'leri bulma
        editTextEmail = findViewById(R.id.editTextRegisterEmail);
        editTextPassword = findViewById(R.id.editTextRegisterPassword);
        tilEmail = findViewById(R.id.tilRegisterEmail); // Yeni
        tilPassword = findViewById(R.id.tilRegisterPassword); // Yeni
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewGoToLogin = findViewById(R.id.textViewGoToLogin);
        progressBar = findViewById(R.id.progressBarRegister);
        mAuth = FirebaseAuth.getInstance();

        textViewGoToLogin.setOnClickListener(v -> { // Listener aynı
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        buttonRegister.setOnClickListener(v -> { // Listener aynı
            registerUser();
        });
    }

    private void registerUser() {
        // Hataları temizle (her denemede)
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Doğrulamalar ve Hata Gösterme (TextInputLayout ile)
        if (TextUtils.isEmpty(email)) {
            // editTextEmail.setError("E-posta gerekli."); // Eski yöntem
            tilEmail.setError("E-posta gerekli."); // Yeni yöntem
            tilEmail.requestFocus(); // Odaklanma TextInputLayout'a değil içindeki EditText'e olmalı
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            // editTextPassword.setError("Şifre gerekli."); // Eski
            tilPassword.setError("Şifre gerekli."); // Yeni
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            // editTextPassword.setError("Şifre en az 6 karakter olmalı."); // Eski
            tilPassword.setError("Şifre en az 6 karakter olmalı."); // Yeni
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE); // İşlem başlarken ProgressBar'ı göster

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> { // Lambda kullanımı daha kısa
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Hata mesajını daha spesifik göstermeye çalışabiliriz
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
                        // E-posta formatı veya zaten kullanımda hatası gibi durumları kontrol edebiliriz
                        if (errorMessage.contains("email address is already in use")) {
                            tilEmail.setError("Bu e-posta adresi zaten kullanılıyor.");
                            editTextEmail.requestFocus();
                        } else if (errorMessage.contains("invalid email address")) {
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