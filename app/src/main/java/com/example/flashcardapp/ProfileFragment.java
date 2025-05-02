package com.example.flashcardapp; // Kendi paket adınız

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText; // Yeni
import android.widget.ProgressBar; // Yeni
import androidx.annotation.NonNull; // Yeni
import androidx.annotation.Nullable; // Yeni
import com.google.android.gms.tasks.OnCompleteListener; // Yeni
import com.google.android.gms.tasks.Task; // Yeni
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest; // Yeni
import com.google.firebase.database.DataSnapshot; // Yeni
import com.google.firebase.database.DatabaseError; // Yeni
import com.google.firebase.database.DatabaseReference; // Yeni
import com.google.firebase.database.FirebaseDatabase; // Yeni
import com.google.firebase.database.ValueEventListener; // Yeni

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private TextView textViewProfileEmail; // Bu zaten vardı (ID'si değişmiş olabilir)
    private EditText editTextDisplayName; // Yeni
    private Button buttonLogout, buttonSaveProfile; // Save butonu yeni
    private ProgressBar progressBar; // Yeni
    private FirebaseAuth mAuth;
    private DatabaseReference userProfileRef; // Yeni: Kullanıcının profiline referans

    private String dbUrl = "https://flashcardapp-2fa06-default-rtdb.europe-west1.firebasedatabase.app/";

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewProfileEmail = view.findViewById(R.id.textViewProfileEmail); // ID'yi kontrol et
        editTextDisplayName = view.findViewById(R.id.editTextProfileDisplayName);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        buttonSaveProfile = view.findViewById(R.id.buttonSaveProfile);
        progressBar = view.findViewById(R.id.progressBarProfile);
        mAuth = FirebaseAuth.getInstance();

        loadUserProfile(); // Kullanıcı bilgilerini yükle

        buttonLogout.setOnClickListener(v -> {
            logoutUser(); // Çıkış metodu
        });

        buttonSaveProfile.setOnClickListener(v -> {
            saveUserProfile(); // Profil kaydetme metodu
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            userProfileRef = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId).child("profile");

            // E-postayı Auth'dan al
            textViewProfileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "E-posta bulunamadı");

            // Görünen Adı yükle (Önce Auth'dan, yoksa DB'den)
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                editTextDisplayName.setText(currentUser.getDisplayName());
                Log.d(TAG, "Display Name loaded from Auth Profile.");
            } else {
                Log.d(TAG, "Display Name not found in Auth Profile, checking Realtime DB...");
                // Auth'da yoksa Realtime DB'den çekmeyi dene
                userProfileRef.child("displayName").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue() != null) {
                            editTextDisplayName.setText(snapshot.getValue(String.class));
                            Log.d(TAG, "Display Name loaded from Realtime DB.");
                        } else {
                            Log.d(TAG, "Display Name not found in Realtime DB either.");
                            // İsteğe bağlı: Kullanıcı adı yoksa e-postanın @ işaretinden öncesini kullan
                            // String email = currentUser.getEmail();
                            // if (email != null && email.contains("@")) {
                            //    editTextDisplayName.setText(email.substring(0, email.indexOf('@')));
                            // }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading display name from DB: ", error.toException());
                    }
                });
            }

        } else {
            // Kullanıcı giriş yapmamışsa (normalde buraya gelmez)
            sendUserToLoginActivity();
        }
    }

    private void saveUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String newDisplayName = editTextDisplayName.getText().toString().trim();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Önce giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(newDisplayName)) {
            editTextDisplayName.setError("Görünen ad boş olamaz.");
            editTextDisplayName.requestFocus();
            return;
        }

        // Mevcut görünen ad ile aynıysa işlem yapma (isteğe bağlı)
        if (newDisplayName.equals(currentUser.getDisplayName())) {
            Toast.makeText(getContext(), "Değişiklik yapılmadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSaveProfile.setEnabled(false);

        // 1. Firebase Auth Profilini Güncelle
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newDisplayName)
                // .setPhotoUri(...) // Profil resmi için
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(taskAuth -> {
                    if (taskAuth.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth profili güncellendi.");
                        // 2. Realtime Database'i Güncelle (Auth başarılı olursa)
                        if (userProfileRef != null) {
                            userProfileRef.child("displayName").setValue(newDisplayName)
                                    .addOnCompleteListener(taskDb -> {
                                        progressBar.setVisibility(View.GONE);
                                        buttonSaveProfile.setEnabled(true);
                                        if (taskDb.isSuccessful()) {
                                            Log.d(TAG, "Realtime DB profili güncellendi.");
                                            Toast.makeText(getContext(), "Profil başarıyla güncellendi!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            // DB güncelleme hatası (ama Auth başarılıydı)
                                            Log.e(TAG, "Realtime DB güncelleme hatası: ", taskDb.getException());
                                            Toast.makeText(getContext(), "Profil güncellendi (veritabanı kısmı hariç).", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // DB referansı yoksa (hata durumu)
                            progressBar.setVisibility(View.GONE);
                            buttonSaveProfile.setEnabled(true);
                            Toast.makeText(getContext(), "Profil güncellendi (Auth kısmı).", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Auth profil güncelleme hatası
                        progressBar.setVisibility(View.GONE);
                        buttonSaveProfile.setEnabled(true);
                        Log.e(TAG, "Firebase Auth profil güncelleme hatası: ", taskAuth.getException());
                        Toast.makeText(getContext(), "Profil güncellenemedi: " + taskAuth.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void logoutUser() {
        mAuth.signOut(); // Firebase oturumunu kapat
        Toast.makeText(getActivity(), "Çıkış yapıldı", Toast.LENGTH_SHORT).show();
        sendUserToLoginActivity(); // Kullanıcıyı Login ekranına yönlendir
    }

    // Kullanıcıyı LoginActivity'ye gönderen metot (Önceki adımlardakiyle aynı)
    private void sendUserToLoginActivity() {
        if (getActivity() != null) {
            Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            getActivity().finish();
        }
    }
}