package com.example.flashcardapp; // Kendi paket adınız

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Opsiyonel: Başlık eklemek için

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.android.material.textfield.TextInputLayout; // Yeni import
import com.google.android.material.textfield.TextInputEditText; // Yeni import
import android.view.LayoutInflater; // Layout inflate etmek için

import android.animation.Animator; // Yeni
import android.animation.AnimatorListenerAdapter; // Yeni
import android.animation.ObjectAnimator; // Yeni
import androidx.cardview.widget.CardView; // Yeni
import android.view.animation.AccelerateDecelerateInterpolator; // Yeni
import android.view.animation.DecelerateInterpolator; // Yeni

import androidx.viewpager2.widget.ViewPager2; // Yeni
import androidx.appcompat.app.AlertDialog; // Dialog için
import android.content.DialogInterface; // Dialog için
import android.widget.EditText; // Dialog içindeki EditText için
import android.widget.LinearLayout; // Dialog layout'u için
import com.google.android.gms.tasks.OnCompleteListener; // Firebase işlemleri için
import com.google.android.gms.tasks.Task; // Firebase işlemleri için
import java.util.HashMap; // updateChildren için
import java.util.Map; // updateChildren için

import java.util.ArrayList;
import java.util.Collections; // Kartları karıştırmak için (opsiyonel)
import java.util.List;


public class ListDetailActivity extends AppCompatActivity {

    private static final String TAG = "ListDetailActivity";

    // --- Değişen veya Kaldırılan View Referansları ---
    private TextView textViewListName, textViewCardCounter;

    private Button buttonAddCard, buttonEditCard, buttonDeleteCard; // Bunlar kaldı
    private ProgressBar progressBar;
    private ViewPager2 viewPagerCards; // Yeni
    private CardPagerAdapter cardPagerAdapter; // Yeni

    // --- Değişen veya Kaldırılan Durum Değişkenleri ---
    private String listId;
    private String listName;
    private List<Card> cardList = new ArrayList<>(); // Liste hala lazım


    // --- Firebase Değişkenleri (Aynı) ---
    private DatabaseReference cardsRef;
    private ValueEventListener cardsListener;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String dbUrl = "https://flashcardapp-2fa06-default-rtdb.europe-west1.firebasedatabase.app/";

    // --- YENİ: Yetki Kontrol Değişkenleri ---
    private boolean isAdmin = false;
    private boolean isOwnedList = false;
    private int permissionChecksCompleted = 0; // Tamamlanan asenkron kontrol sayısı
    private final int TOTAL_PERMISSION_CHECKS = 2; // Toplam kontrol sayısı (admin + sahip)
    private boolean cardsLoaded = false; // Kartların yüklenip yüklenmediği



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        // --- View ID'lerini bulma (Aynı) ---
        textViewListName = findViewById(R.id.textViewDetailListName);
        textViewCardCounter = findViewById(R.id.textViewCardCounter);
        progressBar = findViewById(R.id.progressBarDetail);
        buttonAddCard = findViewById(R.id.buttonAddCard);
        buttonEditCard = findViewById(R.id.buttonEditCard);
        buttonDeleteCard = findViewById(R.id.buttonDeleteCard);
        viewPagerCards = findViewById(R.id.viewPagerCards);

        // --- Auth ve Intent verisi (Aynı) ---
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        currentUserId = (currentUser != null) ? currentUser.getUid() : null;
        listId = getIntent().getStringExtra("LIST_ID");
        listName = getIntent().getStringExtra("LIST_NAME");
        // ... (listId null kontrolü ve listName ayarlama aynı) ...
        if (listId == null) { finish(); return; }
        if (listName != null) { textViewListName.setText(listName); } else { textViewListName.setText("Kartlar");}

        // --- ViewPager Adapter Kurulumu (Aynı) ---
        cardPagerAdapter = new CardPagerAdapter(this);
        viewPagerCards.setAdapter(cardPagerAdapter);
        viewPagerCards.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCardCounter(position);
                // Buton görünürlüğünü tekrar kontrol et (kart listesi durumu değişmiş olabilir)
                tryUpdateCrudButtonVisibility();
            }
        });

        // --- Firebase Referansı (Aynı) ---
        cardsRef = FirebaseDatabase.getInstance(dbUrl).getReference("cards").child(listId);

        // --- Buton Listener'ları (Aynı, ama butonlar başlangıçta gizli olacak) ---
        setupButtonClickListeners();

        // --- YENİ: Yetki Kontrollerini Başlat ve Butonları Başlangıçta Gizle ---
        hideCrudButtons(); // Başlangıçta butonları gizle
        if (currentUserId != null) {
            checkAdminStatus(currentUserId);
            checkListOwnership(currentUserId, listId);
        } else {
            // Kullanıcı giriş yapmamışsa kontrolleri tamamlanmış say ve butonlar gizli kalsın
            permissionChecksCompleted = TOTAL_PERMISSION_CHECKS;
            tryUpdateCrudButtonVisibility();
        }

        // --- Kartları Yükle (Aynı) ---
        loadCards();
    }

    // Başlangıçta veya yetki yoksa butonları gizleyen metot
    private void hideCrudButtons() {
        buttonAddCard.setVisibility(View.GONE);
        buttonEditCard.setVisibility(View.GONE);
        buttonDeleteCard.setVisibility(View.GONE);
    }

    // Kullanıcının admin olup olmadığını kontrol eder
    private void checkAdminStatus(String userId) {
        DatabaseReference adminRef = FirebaseDatabase.getInstance(dbUrl).getReference("admins").child(userId);
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isAdmin = snapshot.exists(); // Admin listesinde varsa true
                Log.d(TAG, "Admin check completed. isAdmin: " + isAdmin);
                permissionChecksCompleted++;
                tryUpdateCrudButtonVisibility(); // Buton görünürlüğünü kontrol etmeyi dene
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Admin check failed: ", error.toException());
                isAdmin = false; // Hata durumunda admin değil varsay
                permissionChecksCompleted++;
                tryUpdateCrudButtonVisibility();
            }
        });
    }

    // Kullanıcının listeye sahip olup olmadığını kontrol eder
    private void checkListOwnership(String userId, String listIdToCheck) {
        DatabaseReference ownerRef = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId).child("my_lists").child(listIdToCheck);
        ownerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isOwnedList = snapshot.exists(); // Kullanıcının listelerinde varsa true
                Log.d(TAG, "Ownership check completed. isOwned: " + isOwnedList);
                permissionChecksCompleted++;
                tryUpdateCrudButtonVisibility();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Ownership check failed: ", error.toException());
                isOwnedList = false; // Hata durumunda sahip değil varsay
                permissionChecksCompleted++;
                tryUpdateCrudButtonVisibility();
            }
        });
    }

    // Tüm yetki kontrolleri bittikten sonra buton görünürlüğünü ayarlar
    private void tryUpdateCrudButtonVisibility() {
        Log.d(TAG, "Trying to update button visibility. Checks completed: " + permissionChecksCompleted + ", Cards loaded: " + cardsLoaded);
        // Sadece tüm kontroller bittiyse VE kartlar yüklendiyse karar ver
        if (permissionChecksCompleted >= TOTAL_PERMISSION_CHECKS && cardsLoaded) {
            boolean canModify = isOwnedList || isAdmin; // Sahipse VEYA admin ise değiştirebilir
            Log.d(TAG, "Updating CRUD button visibility. CanModify: " + canModify);

            buttonAddCard.setVisibility(canModify ? View.VISIBLE : View.GONE);

            // Edit/Delete butonları hem yetkiye hem de kart olup olmamasına bağlı
            boolean hasCards = cardPagerAdapter.getItemCount() > 0;
            buttonEditCard.setVisibility(canModify && hasCards ? View.VISIBLE : View.GONE);
            buttonDeleteCard.setVisibility(canModify && hasCards ? View.VISIBLE : View.GONE);
        } else if (permissionChecksCompleted >= TOTAL_PERMISSION_CHECKS && !cardsLoaded){
            // Kontroller bitti ama kartlar henüz yüklenmediyse, butonlar gizli kalmalı.
            // loadCards içindeki onDataChange tekrar çağıracak.
            Log.d(TAG, "Checks completed but cards not loaded yet, buttons remain hidden.");
            hideCrudButtons();
        } else {
            // Henüz tüm kontroller bitmediyse bekle
            Log.d(TAG, "Checks not completed yet.");
        }
    }

    private void setupButtonClickListeners() {
        // CardView listener kaldırıldı

        // Diğer butonlar mevcut kart index'ini ViewPager'dan almalı
        buttonAddCard.setOnClickListener(v -> showAddCardDialog()); // Bu aynı

        buttonEditCard.setOnClickListener(v -> {
            int currentItem = viewPagerCards.getCurrentItem();
            Card cardToEdit = cardPagerAdapter.getCardAt(currentItem);
            if (cardToEdit != null) {
                showEditCardDialog(cardToEdit); // Dialog'u aç
            }
        });

        buttonDeleteCard.setOnClickListener(v -> {
            int currentItem = viewPagerCards.getCurrentItem();
            Card cardToDelete = cardPagerAdapter.getCardAt(currentItem);
            if (cardToDelete != null) {
                showDeleteConfirmationDialog(cardToDelete); // Onay dialogunu aç
            }
        });
    }

    // Kartları yükleyen metot (içinde tryUpdateCrudButtonVisibility çağrısı var)
    private void loadCards() {
        progressBar.setVisibility(View.VISIBLE);
        cardsLoaded = false; // Yükleme başlarken false yap
        // updateEditDeleteButtonState(false); // Bu metot kaldırıldı

        cardsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // ... (Kartları çekip `loadedCards` listesine ekleme kısmı aynı) ...
                List<Card> loadedCards = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Card card = snapshot.getValue(Card.class);
                            if (card != null) { card.setCardId(snapshot.getKey()); loadedCards.add(card); }
                        } catch (Exception e) { Log.e(TAG, "Kart parse hatası: " + snapshot.getKey(), e); }
                    }
                    Log.d(TAG, "Toplam " + loadedCards.size() + " kart yüklendi.");
                    Collections.shuffle(loadedCards);
                    Log.d(TAG, "Kart listesi karıştırıldı.");
                } else { Log.w(TAG, "Bu liste için kart bulunamadı: " + listId); }

                progressBar.setVisibility(View.GONE);
                cardsLoaded = true; // Kartlar yüklendi
                cardPagerAdapter.setCards(loadedCards); // Adaptörü güncelle
                updateCardCounter(viewPagerCards.getCurrentItem()); // Sayacı güncelle

                // --- YENİ: Buton görünürlüğünü şimdi kontrol et ---
                tryUpdateCrudButtonVisibility();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ... (Hata yönetimi aynı) ...
                progressBar.setVisibility(View.GONE);
                cardsLoaded = true; // Hata da olsa yükleme bitti sayılır
                tryUpdateCrudButtonVisibility(); // Butonlar gizli kalmalı
                Toast.makeText(ListDetailActivity.this, "Kartlar yüklenemedi.", Toast.LENGTH_SHORT).show();
            }
        };
        cardsRef.addListenerForSingleValueEvent(cardsListener);
    }

    // Kart sayacını güncelleyen metot
    private void updateCardCounter(int currentPosition) {
        int totalCards = cardPagerAdapter.getItemCount();
        if (totalCards == 0) {
            textViewCardCounter.setText("0 / 0");
        } else {
            textViewCardCounter.setText("Kart " + (currentPosition + 1) + " / " + totalCards);
        }
    }

    // Edit/Delete buton durumunu ayarlar (Kart varsa aktif)
    private void updateEditDeleteButtonState(boolean active) {
        // TODO: Liste sahipliği kontrolü eklenebilir
        buttonEditCard.setEnabled(active);
        buttonDeleteCard.setEnabled(active);
    }



    // --- CRUD Metotları ---

    // KART EKLEME (Dialog Layout Kullanarak)
    private void showAddCardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Yeni Kart Ekle");
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_card, null);
        builder.setView(dialogView);
        final TextInputLayout tilFront = dialogView.findViewById(R.id.tilCardFront);
        final TextInputEditText inputFront = dialogView.findViewById(R.id.editTextCardFront);
        final TextInputLayout tilBack = dialogView.findViewById(R.id.tilCardBack);
        final TextInputEditText inputBack = dialogView.findViewById(R.id.editTextCardBack);

        builder.setPositiveButton("Kaydet", null);
        builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                tilFront.setError(null); tilBack.setError(null);
                String frontText = inputFront.getText().toString().trim();
                String backText = inputBack.getText().toString().trim();
                boolean hasError = false;
                if (TextUtils.isEmpty(frontText)) { tilFront.setError("Ön yüz boş bırakılamaz."); hasError = true; }
                if (TextUtils.isEmpty(backText)) { tilBack.setError("Arka yüz boş bırakılamaz."); hasError = true; }
                if (!hasError) { addNewCardToFirebase(frontText, backText); dialog.dismiss(); }
            });
        });
        dialog.show();
    }

    private void addNewCardToFirebase(String front, String back) {
        progressBar.setVisibility(View.VISIBLE); // İşlem göstergesi
        String newCardId = cardsRef.push().getKey(); // Yeni ID al

        if (newCardId == null) {
            Toast.makeText(this, "Kart ID'si oluşturulamadı.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        Card newCard = new Card(front, back);
        newCard.setCardId(newCardId); // ID'yi de set edelim (lokal liste için)

        cardsRef.child(newCardId).setValue(newCard)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ListDetailActivity.this, "Kart eklendi!", Toast.LENGTH_SHORT).show();
                        // Adaptöre yeni kartı ekle (Bu notifyItemInserted çağırır)
                        cardPagerAdapter.addCard(newCard);
                        // Kullanıcıyı yeni eklenen karta götür (opsiyonel)
                        viewPagerCards.setCurrentItem(cardPagerAdapter.getItemCount() - 1, true); // Animasyonlu geçiş
                        // Sayacı ve butonları güncelle
                        updateCardCounter(viewPagerCards.getCurrentItem());
                        tryUpdateCrudButtonVisibility();
                    } else {
                        Toast.makeText(ListDetailActivity.this, "Kart eklenemedi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Kart ekleme hatası: ", task.getException());
                    }
                });
    }


    // KART SİLME
    private void showDeleteConfirmationDialog(Card cardToDelete) {
        if (cardToDelete == null || cardToDelete.getCardId() == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Kartı Sil")
                .setMessage("'" + cardToDelete.getFront() + "' kartını silmek istediğinizden emin misiniz?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Evet, Sil", (dialog, whichButton) -> { deleteCardFromFirebase(cardToDelete); })
                .setNegativeButton("İptal", null).show();
    }

    private void deleteCardFromFirebase(Card cardToDelete) {
        progressBar.setVisibility(View.VISIBLE);
        int positionToDelete = cardPagerAdapter.getCurrentList().indexOf(cardToDelete); // Silinecek pozisyonu bul
        String cardId = cardToDelete.getCardId();

        cardsRef.child(cardId).removeValue()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ListDetailActivity.this, "Kart silindi!", Toast.LENGTH_SHORT).show();
                        // Adaptörden kartı sil (Bu notifyItemRemoved çağırır)
                        if (positionToDelete != -1) {
                            cardPagerAdapter.removeCard(positionToDelete);
                            // Sayacı ve buton durumunu güncelle
                            updateCardCounter(viewPagerCards.getCurrentItem());
                            tryUpdateCrudButtonVisibility();
                        }
                    }else {
                        Toast.makeText(ListDetailActivity.this, "Kart silinemedi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Kart silme hatası: ", task.getException());
                    }
                });
    }


    // KART DÜZENLEME (Dialog Layout Kullanarak)
    private void showEditCardDialog(Card cardToEdit) {
        if (cardToEdit == null || cardToEdit.getCardId() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kartı Düzenle");
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_card, null);
        builder.setView(dialogView);
        final TextInputLayout tilFront = dialogView.findViewById(R.id.tilCardFront);
        final TextInputEditText inputFront = dialogView.findViewById(R.id.editTextCardFront);
        final TextInputLayout tilBack = dialogView.findViewById(R.id.tilCardBack);
        final TextInputEditText inputBack = dialogView.findViewById(R.id.editTextCardBack);
        inputFront.setText(cardToEdit.getFront());
        inputBack.setText(cardToEdit.getBack());
        builder.setPositiveButton("Kaydet", null);
        builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                tilFront.setError(null); tilBack.setError(null);
                String newFrontText = inputFront.getText().toString().trim();
                String newBackText = inputBack.getText().toString().trim();
                boolean hasError = false;
                if (TextUtils.isEmpty(newFrontText)) { tilFront.setError("Ön yüz boş bırakılamaz."); hasError = true; }
                if (TextUtils.isEmpty(newBackText)) { tilBack.setError("Arka yüz boş bırakılamaz."); hasError = true; }
                if (!hasError) { updateCardInFirebase(cardToEdit, newFrontText, newBackText); dialog.dismiss(); }
            });
        });
        dialog.show();
    }

    private void updateCardInFirebase(Card originalCard, String newFront, String newBack) {
        // ... (Metodun başı aynı: progressBar, pozisyon bulma, id alma, map oluşturma) ...
        progressBar.setVisibility(View.VISIBLE);
        int positionToUpdate = cardPagerAdapter.getCurrentList().indexOf(originalCard);
        String cardId = originalCard.getCardId();
        Map<String, Object> updatedValues = new HashMap<>();
        updatedValues.put("front", newFront); updatedValues.put("back", newBack);

        cardsRef.child(cardId).updateChildren(updatedValues)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ListDetailActivity.this, "Kart güncellendi!", Toast.LENGTH_SHORT).show();
                        if (positionToUpdate != -1) {
                            Card updatedCard = cardPagerAdapter.getCardAt(positionToUpdate);
                            if(updatedCard != null){
                                updatedCard.setFront(newFront); updatedCard.setBack(newBack);
                                cardPagerAdapter.updateCard(positionToUpdate, updatedCard);
                                // Not: Mevcut fragment'ın anında güncellenmesi için ek kod gerekebilir.
                                // Şimdilik sadece adaptördeki veri güncellendi.
                                // Görünürlük zaten doğru olmalı, tekrar kontrol etmeye gerek yok.
                            }
                        }
                    } else {
                        Toast.makeText(ListDetailActivity.this, "Kart güncellenemedi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Kart güncelleme hatası: ", task.getException());
                    }
                });
    }

    // Edit ve Delete butonlarının aktif/pasif durumunu ayarlar


    // Eski flipCard metodunu animasyonlu versiyonla değiştiriyoruz


    // Listener'ı kaldırmak için (eğer addValueEventListener kullanıldıysa)
    @Override protected void onDestroy() { super.onDestroy(); } // Listener kaldırma eklenebilir


    // ActionBar geri butonu için (opsiyonel)
    @Override public boolean onOptionsItemSelected(MenuItem item) { if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; } return super.onOptionsItemSelected(item); }

}