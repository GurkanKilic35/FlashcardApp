package com.example.flashcardapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import android.view.LayoutInflater;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import androidx.cardview.widget.CardView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import androidx.viewpager2.widget.ViewPager2;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.SharedPreferences;


public class ListDetailActivity extends AppCompatActivity {

    private static final String TAG = "ListDetailActivity";

    private TextView textViewListName, textViewCardCounter;
    private Button buttonAddCard, buttonEditCard, buttonDeleteCard;
    private ProgressBar progressBar;
    private ViewPager2 viewPagerCards;
    private CardPagerAdapter cardPagerAdapter;
    private String listId;
    private String listName;
    private List<Card> cardList = new ArrayList<>();
    private DatabaseReference cardsRef;
    private ValueEventListener cardsListener;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String dbUrl = "https://flashcardapp-2fa06-default-rtdb.europe-west1.firebasedatabase.app/";
    private boolean isAdmin = false;
    private boolean isOwnedList = false;
    private int permissionChecksCompleted = 0;
    private final int TOTAL_PERMISSION_CHECKS = 2;
    private boolean cardsLoaded = false;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "FlashcardAppPrefs";
    private static final String KEY_SHUFFLE_CARDS = "shuffle_cards_on_start";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        textViewListName = findViewById(R.id.textViewDetailListName);
        textViewCardCounter = findViewById(R.id.textViewCardCounter);
        progressBar = findViewById(R.id.progressBarDetail);
        buttonAddCard = findViewById(R.id.buttonAddCard);
        buttonEditCard = findViewById(R.id.buttonEditCard);
        buttonDeleteCard = findViewById(R.id.buttonDeleteCard);
        viewPagerCards = findViewById(R.id.viewPagerCards);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        currentUserId = (currentUser != null) ? currentUser.getUid() : null;
        listId = getIntent().getStringExtra("LIST_ID");
        listName = getIntent().getStringExtra("LIST_NAME");

        if (listId == null) { finish(); return; }
        if (listName != null) { textViewListName.setText(listName); } else { textViewListName.setText("Kartlar");}

        cardPagerAdapter = new CardPagerAdapter(this);
        viewPagerCards.setAdapter(cardPagerAdapter);
        viewPagerCards.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCardCounter(position);
                tryUpdateCrudButtonVisibility();
            }
        });

        cardsRef = FirebaseDatabase.getInstance(dbUrl).getReference("cards").child(listId);

        setupButtonClickListeners();

        hideCrudButtons();
        if (currentUserId != null) {
            checkAdminStatus(currentUserId);
            checkListOwnership(currentUserId, listId);
        } else {
            permissionChecksCompleted = TOTAL_PERMISSION_CHECKS;
            tryUpdateCrudButtonVisibility();
        }

        loadCards();
    }

    // Yetki yoksa butonları gizleyen metot
    private void hideCrudButtons() {
        buttonAddCard.setVisibility(View.GONE);
        buttonEditCard.setVisibility(View.GONE);
        buttonDeleteCard.setVisibility(View.GONE);
    }

    private void checkAdminStatus(String userId) {
        DatabaseReference adminRef = FirebaseDatabase.getInstance(dbUrl).getReference("admins").child(userId);
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isAdmin = snapshot.exists();
                Log.d(TAG, "Admin check completed. isAdmin: " + isAdmin);
                permissionChecksCompleted++;
                tryUpdateCrudButtonVisibility();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Admin check failed: ", error.toException());
                isAdmin = false;
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
                isOwnedList = snapshot.exists();
                Log.d(TAG, "Ownership check completed. isOwned: " + isOwnedList);
                permissionChecksCompleted++;
                tryUpdateCrudButtonVisibility();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Ownership check failed: ", error.toException());
                isOwnedList = false;
                permissionChecksCompleted++;
                tryUpdateCrudButtonVisibility();
            }
        });
    }

    // Tüm yetki kontrolleri bittikten sonra buton görünürlüğünü ayarlar
    private void tryUpdateCrudButtonVisibility() {
        if (permissionChecksCompleted >= TOTAL_PERMISSION_CHECKS && cardsLoaded) {
            boolean canModify = isOwnedList || isAdmin;

            buttonAddCard.setVisibility(canModify ? View.VISIBLE : View.GONE);

            boolean hasCards = cardPagerAdapter.getItemCount() > 0;
            buttonEditCard.setVisibility(canModify && hasCards ? View.VISIBLE : View.GONE);
            buttonDeleteCard.setVisibility(canModify && hasCards ? View.VISIBLE : View.GONE);
        } else if (permissionChecksCompleted >= TOTAL_PERMISSION_CHECKS && !cardsLoaded){
            hideCrudButtons();
        } else {

        }
    }

    private void setupButtonClickListeners() {

        buttonAddCard.setOnClickListener(v -> showAddCardDialog());

        buttonEditCard.setOnClickListener(v -> {
            int currentItem = viewPagerCards.getCurrentItem();
            Card cardToEdit = cardPagerAdapter.getCardAt(currentItem);
            if (cardToEdit != null) {
                showEditCardDialog(cardToEdit);
            }
        });

        buttonDeleteCard.setOnClickListener(v -> {
            int currentItem = viewPagerCards.getCurrentItem();
            Card cardToDelete = cardPagerAdapter.getCardAt(currentItem);
            if (cardToDelete != null) {
                showDeleteConfirmationDialog(cardToDelete);
            }
        });
    }

    private void loadCards() {
        progressBar.setVisibility(View.VISIBLE);
        cardsLoaded = false;

        cardsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Card> loadedCards = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Card card = snapshot.getValue(Card.class);
                            if (card != null) { card.setCardId(snapshot.getKey()); loadedCards.add(card); }
                        } catch (Exception e) { Log.e(TAG, "Kart parse hatası: " + snapshot.getKey(), e); }
                    }
                    Log.d(TAG, "Toplam " + loadedCards.size() + " kart yüklendi.");

                    boolean shouldShuffle = false;
                    if (sharedPreferences != null) {
                        shouldShuffle = sharedPreferences.getBoolean(KEY_SHUFFLE_CARDS, true);
                    }

                    if (shouldShuffle && !loadedCards.isEmpty()) {
                        Collections.shuffle(loadedCards);
                        Log.d(TAG, "Kart listesi tercihe göre karıştırıldı.");
                    }else if (!loadedCards.isEmpty()) {
                        Log.d(TAG, "Kart listesi karıştırılmadı (kullanıcı tercihi veya liste boş).");
                    }
                } else { Log.w(TAG, "Bu liste için kart bulunamadı: " + listId); }

                progressBar.setVisibility(View.GONE);
                cardsLoaded = true;
                cardPagerAdapter.setCards(loadedCards);
                updateCardCounter(viewPagerCards.getCurrentItem());

                tryUpdateCrudButtonVisibility();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                cardsLoaded = true;
                tryUpdateCrudButtonVisibility();
                Toast.makeText(ListDetailActivity.this, "Kartlar yüklenemedi.", Toast.LENGTH_SHORT).show();
            }
        };
        cardsRef.addListenerForSingleValueEvent(cardsListener);
    }

    private void updateCardCounter(int currentPosition) {
        int totalCards = cardPagerAdapter.getItemCount();
        if (totalCards == 0) {
            textViewCardCounter.setText("0 / 0");
        } else {
            textViewCardCounter.setText("Kart " + (currentPosition + 1) + " / " + totalCards);
        }
    }

    private void updateEditDeleteButtonState(boolean active) {
        buttonEditCard.setEnabled(active);
        buttonDeleteCard.setEnabled(active);
    }





    // KART EKLEME
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
        progressBar.setVisibility(View.VISIBLE);
        String newCardId = cardsRef.push().getKey();

        if (newCardId == null) {
            Toast.makeText(this, "Kart ID'si oluşturulamadı.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        Card newCard = new Card(front, back);
        newCard.setCardId(newCardId);

        cardsRef.child(newCardId).setValue(newCard)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ListDetailActivity.this, "Kart eklendi!", Toast.LENGTH_SHORT).show();
                        cardPagerAdapter.addCard(newCard);
                        viewPagerCards.setCurrentItem(cardPagerAdapter.getItemCount() - 1, true);
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
        int positionToDelete = cardPagerAdapter.getCurrentList().indexOf(cardToDelete);
        String cardId = cardToDelete.getCardId();

        cardsRef.child(cardId).removeValue()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ListDetailActivity.this, "Kart silindi!", Toast.LENGTH_SHORT).show();
                        if (positionToDelete != -1) {
                            cardPagerAdapter.removeCard(positionToDelete);
                            updateCardCounter(viewPagerCards.getCurrentItem());
                            tryUpdateCrudButtonVisibility();
                        }
                    }else {
                        Toast.makeText(ListDetailActivity.this, "Kart silinemedi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Kart silme hatası: ", task.getException());
                    }
                });
    }


    // KART DÜZENLEME
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
                            }
                        }
                    } else {
                        Toast.makeText(ListDetailActivity.this, "Kart güncellenemedi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Kart güncelleme hatası: ", task.getException());
                    }
                });
    }


    @Override protected void onDestroy() { super.onDestroy(); }

    @Override public boolean onOptionsItemSelected(MenuItem item) { if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; } return super.onOptionsItemSelected(item); }

}