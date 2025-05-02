package com.example.flashcardapp; // Kendi paket adınız

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

import androidx.annotation.NonNull; // Ekleyelim
import androidx.annotation.Nullable; // Ekleyelim
import com.google.android.material.switchmaterial.SwitchMaterial; // Yeni
import com.google.firebase.database.DataSnapshot; // Yeni
import com.google.firebase.database.DatabaseError; // Yeni
import com.google.firebase.database.ValueEventListener; // Yeni

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue; // Zaman damgası için


public class UploadFragment extends Fragment {

    private static final String TAG = "UploadFragment";

    private EditText editTextListName, editTextListDescription;
    private TextInputLayout tilListName; // Yeni
    // Açıklama zorunlu olmadığı için TextInputLayout referansı şart değil, ama isterseniz ekleyebilirsiniz.
    private Button buttonSaveList;
    private SwitchMaterial switchMakePublic; // Yeni
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference userListsRef; // Kişisel listelerin kaydedileceği yer
    private DatabaseReference publicListsRef; // Herkese açık listelerin kaydedileceği yer
    private String currentUserId; // Kullanıcı ID'sini saklamak için
    private boolean isAdmin = false; // Kullanıcının admin durumu

    private String dbUrl = "https://flashcardapp-2fa06-default-rtdb.europe-west1.firebasedatabase.app/";


    public UploadFragment() {
        // Required empty public constructor
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- View ID'lerini Bulma ---
        editTextListName = view.findViewById(R.id.editTextListNameUpload);
        editTextListDescription = view.findViewById(R.id.editTextListDescriptionUpload);
        tilListName = view.findViewById(R.id.tilListNameUpload);
        switchMakePublic = view.findViewById(R.id.switchMakePublic); // Yeni
        buttonSaveList = view.findViewById(R.id.buttonSaveList);
        progressBar = view.findViewById(R.id.progressBarUpload);

        // --- Firebase Referansları ve Auth ---
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        publicListsRef = FirebaseDatabase.getInstance(dbUrl).getReference("public_lists"); // Public ref

        if (currentUser != null) {
            currentUserId = currentUser.getUid(); // ID'yi değişkene al
            userListsRef = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(currentUserId).child("my_lists");
            checkAdminStatus(currentUserId); // Admin durumunu kontrol et
        } else {
            Log.w(TAG, "Kullanıcı giriş yapmamış, kaydetme devre dışı.");
            buttonSaveList.setEnabled(false);
            switchMakePublic.setVisibility(View.GONE); // Switch'i de gizle
            // Toast.makeText(getContext(), "Liste kaydetmek için giriş yapmalısınız.", Toast.LENGTH_LONG).show();
        }

        buttonSaveList.setOnClickListener(v -> {
            if (currentUserId != null) { // Kullanıcı ID'si varsa kaydetmeyi dene
                saveNewList();
            } else {
                Toast.makeText(getContext(), "Liste kaydedilemedi (Kullanıcı bulunamadı).", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Kullanıcının admin olup olmadığını kontrol eden metot
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
                Log.e(TAG, "Admin check failed: ", error.toException());
                isAdmin = false; // Hata durumunda admin değil varsay
                switchMakePublic.setVisibility(View.GONE); // Hata durumunda da gizle
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
            Log.d(TAG, "Saving list to PUBLIC path.");
        } else {
            // Admin değilse veya admin ama public istemiyorsa kişisel listeye kaydet
            if (userListsRef == null) { // Güvenlik kontrolü
                Toast.makeText(getContext(), "Kullanıcı referansı bulunamadı.", Toast.LENGTH_SHORT).show();
                return;
            }
            targetRef = userListsRef; // Hedef: /users/$uid/my_lists
            Log.d(TAG, "Saving list to PRIVATE path.");
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSaveList.setEnabled(false);
        switchMakePublic.setEnabled(false); // Kayıt sırasında değiştirilemesin

        // Benzersiz ID oluştur (Hedef referans üzerinden!)
        String newListId = targetRef.push().getKey();
        if (newListId == null) {
            Log.e(TAG, "Firebase push ID oluşturulamadı.");
            Toast.makeText(getContext(), "Liste kaydedilirken hata oluştu (ID sorunu).", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            buttonSaveList.setEnabled(true);
            switchMakePublic.setEnabled(true);
            return;
        }

        // CardList objesini oluştur ve creator UID'sini ekle
        CardList newCardList = new CardList(listName, listDescription);
        newCardList.setCreatedByUid(currentUserId); // OLUŞTURANIN UID'SİNİ EKLE!

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
                        switchMakePublic.setChecked(false); // Switch'i sıfırla
                        tilListName.setError(null);
                        editTextListName.requestFocus();
                        Log.d(TAG, "Liste kaydedildi. Path: " + targetRef.child(newListId).toString());

                        // İsteğe bağlı: Kullanıcıyı ilgili liste ekranına yönlendir
                        // if (isPublic) { // Belki HomeFragment'a } else { // Belki CardListFragment'a }

                    } else {
                        // Firebase kuralları nedeniyle hata almış olabiliriz (örn: admin olmayan public yazmaya çalışırsa)
                        Toast.makeText(getContext(), "Liste kaydedilirken hata oluştu: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Firebase setValue hatası (" + targetRef.toString() + "): ", task.getException());
                    }
                });
    }
}