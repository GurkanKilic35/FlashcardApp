package com.example.flashcardapp;

import android.content.Context;
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
import android.widget.EditText;
import android.widget.ProgressBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.content.SharedPreferences;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private TextView textViewProfileEmail;
    private EditText editTextDisplayName;
    private Button buttonLogout, buttonSaveProfile;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference userProfileRef;

    private SwitchMaterial switchShuffleCards;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "FlashcardAppPrefs";
    private static final String KEY_SHUFFLE_CARDS = "shuffle_cards_on_start";

    private String dbUrl = "https://flashcardapp-2fa06-default-rtdb.europe-west1.firebasedatabase.app/";

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewProfileEmail = view.findViewById(R.id.textViewProfileEmail);
        editTextDisplayName = view.findViewById(R.id.editTextProfileDisplayName);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        buttonSaveProfile = view.findViewById(R.id.buttonSaveProfile);
        progressBar = view.findViewById(R.id.progressBarProfile);
        switchShuffleCards = view.findViewById(R.id.switchShuffleCards);
        mAuth = FirebaseAuth.getInstance();

        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        boolean shuffleEnabled = sharedPreferences.getBoolean(KEY_SHUFFLE_CARDS, true);
        switchShuffleCards.setChecked(shuffleEnabled);

        switchShuffleCards.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_SHUFFLE_CARDS, isChecked);
            editor.apply();
        });

        loadUserProfile();
        buttonLogout.setOnClickListener(v -> {
            logoutUser();
        });

        buttonSaveProfile.setOnClickListener(v -> {
            saveUserProfile();
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            userProfileRef = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId).child("profile");

            // E-postayı Auth'dan al
            textViewProfileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "E-posta bulunamadı");

            // Görünen Adı yükle
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                editTextDisplayName.setText(currentUser.getDisplayName());
            } else {
                userProfileRef.child("displayName").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue() != null) {
                            editTextDisplayName.setText(snapshot.getValue(String.class));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading display name from DB: ", error.toException());
                    }
                });
            }

        } else {
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

        if (newDisplayName.equals(currentUser.getDisplayName())) {
            Toast.makeText(getContext(), "Değişiklik yapılmadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSaveProfile.setEnabled(false);

        // Firebase Auth Profilini Güncelle
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newDisplayName)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(taskAuth -> {
                    if (taskAuth.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth profili güncellendi.");
                        // Realtime Database'i Güncelle
                        if (userProfileRef != null) {
                            userProfileRef.child("displayName").setValue(newDisplayName)
                                    .addOnCompleteListener(taskDb -> {
                                        progressBar.setVisibility(View.GONE);
                                        buttonSaveProfile.setEnabled(true);
                                        if (taskDb.isSuccessful()) {
                                            Log.d(TAG, "Realtime DB profili güncellendi.");
                                            Toast.makeText(getContext(), "Profil başarıyla güncellendi!", Toast.LENGTH_SHORT).show();
                                        } else {
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

    private void sendUserToLoginActivity() {
        if (getActivity() != null) {
            Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            getActivity().finish();
        }
    }
}