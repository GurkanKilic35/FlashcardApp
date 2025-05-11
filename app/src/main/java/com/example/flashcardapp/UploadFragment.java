package com.example.flashcardapp;

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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class UploadFragment extends Fragment {

    private static final String TAG = "UploadFragment";

    private EditText editTextListName, editTextListDescription;
    private TextInputLayout tilListName;
    private Button buttonSaveList;
    private SwitchMaterial switchMakePublic;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference userListsRef;
    private DatabaseReference publicListsRef;
    private String currentUserId;
    private boolean isAdmin = false;

    private String dbUrl = "https://flashcardapp-2fa06-default-rtdb.europe-west1.firebasedatabase.app/";


    public UploadFragment() {}

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTextListName = view.findViewById(R.id.editTextListNameUpload);
        editTextListDescription = view.findViewById(R.id.editTextListDescriptionUpload);
        tilListName = view.findViewById(R.id.tilListNameUpload);
        switchMakePublic = view.findViewById(R.id.switchMakePublic);
        buttonSaveList = view.findViewById(R.id.buttonSaveList);
        progressBar = view.findViewById(R.id.progressBarUpload);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        publicListsRef = FirebaseDatabase.getInstance(dbUrl).getReference("public_lists");

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            userListsRef = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(currentUserId).child("my_lists");
            checkAdminStatus(currentUserId);
        } else {
            Log.w(TAG, "Kullanıcı giriş yapmamış, kaydetme devre dışı.");
            buttonSaveList.setEnabled(false);
            switchMakePublic.setVisibility(View.GONE);
        }

        buttonSaveList.setOnClickListener(v -> {
            if (currentUserId != null) {
                saveNewList();
            } else {
                Toast.makeText(getContext(), "Liste kaydedilemedi (Kullanıcı bulunamadı).", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAdminStatus(String userId) {
        DatabaseReference adminRef = FirebaseDatabase.getInstance(dbUrl).getReference("admins").child(userId);
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isAdmin = snapshot.exists();
                Log.d(TAG, "Admin check completed. isAdmin: " + isAdmin);
                // Admin ise switch'i görünür yap
                if (isAdmin) {
                    switchMakePublic.setVisibility(View.VISIBLE);
                } else {
                    switchMakePublic.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isAdmin = false;
                switchMakePublic.setVisibility(View.GONE);
            }
        });
    }

    private void saveNewList() {
        tilListName.setError(null);
        String listName = editTextListName.getText().toString().trim();
        String listDescription = editTextListDescription.getText().toString().trim();

        if (TextUtils.isEmpty(listName)) {
            tilListName.setError("Liste adı gerekli.");
            editTextListName.requestFocus();
            return;
        }

        // Hedef veritabanı referansını belirle
        DatabaseReference targetRef;
        boolean isPublic = isAdmin && switchMakePublic.isChecked(); // Admin ve seçiliyse public

        if (isPublic) {
            targetRef = publicListsRef; // Hedef: /public_lists
        } else {
            // Admin değilse veya admin ama public istemiyorsa kişisel listeye kaydet
            if (userListsRef == null) { // Güvenlik kontrolü
                Toast.makeText(getContext(), "Kullanıcı referansı bulunamadı.", Toast.LENGTH_SHORT).show();
                return;
            }
            targetRef = userListsRef; // Hedef: /users/$uid/my_lists
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSaveList.setEnabled(false);
        switchMakePublic.setEnabled(false);

        String newListId = targetRef.push().getKey();
        if (newListId == null) {
            Toast.makeText(getContext(), "Liste kaydedilirken hata oluştu (ID sorunu).", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            buttonSaveList.setEnabled(true);
            switchMakePublic.setEnabled(true);
            return;
        }

        // CardList objesini oluştur ve creator UID'sini ekle
        CardList newCardList = new CardList(listName, listDescription);
        newCardList.setCreatedByUid(currentUserId);

        // Veritabanına kaydet
        targetRef.child(newListId).setValue(newCardList)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    buttonSaveList.setEnabled(true);
                    switchMakePublic.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Liste başarıyla kaydedildi!", Toast.LENGTH_SHORT).show();
                        editTextListName.setText("");
                        editTextListDescription.setText("");
                        switchMakePublic.setChecked(false);
                        tilListName.setError(null);
                        editTextListName.requestFocus();
                        Log.d(TAG, "Liste kaydedildi. Path: " + targetRef.child(newListId).toString());

                    } else {
                        Toast.makeText(getContext(), "Liste kaydedilirken hata oluştu: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Firebase setValue hatası (" + targetRef.toString() + "): ", task.getException());
                    }
                });
    }
}