package com.example.flashcardapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.List;
import androidx.appcompat.widget.SearchView;
import android.text.TextUtils;

public class CardListFragment extends Fragment {

    private static final String TAG = "CardListFragment";
    private RecyclerView recyclerView;
    private CardListAdapter adapter;
    private ProgressBar progressBar;
    private TextView textViewNoLists;
    private TextView textViewNoSearchResults;
    private SearchView searchView;
    private FirebaseAuth mAuth;
    private DatabaseReference userListsRef;
    private ValueEventListener valueEventListener;
    private ItemTouchHelper itemTouchHelper;

    @ColorInt private int swipeBackgroundColor;
    @ColorInt private int swipeIconColor;
    private Drawable deleteIcon;

    private String dbUrl = "https://flashcardapp-2fa06-default-rtdb.europe-west1.firebasedatabase.app/";

    public CardListFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewCardList);
        progressBar = view.findViewById(R.id.progressBarCardList);
        textViewNoLists = view.findViewById(R.id.textViewNoCardLists);
        textViewNoSearchResults = view.findViewById(R.id.textViewNoCardListSearchResults);
        searchView = view.findViewById(R.id.searchViewCardList);

        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new CardListAdapter(requireContext(), new ArrayList<>());
            recyclerView.setAdapter(adapter);
        } else {
            return;
        }

        // Tema Renkleri ve Swipe Kurulumu
        loadThemeColorsAndIcon();
        ItemTouchHelper.SimpleCallback simpleCallback = createItemTouchHelperCallback();
        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        if (searchView != null) {
            setupSearchView();
        }

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            userListsRef = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId).child("my_lists");
            loadUserLists();
        } else {
            handleUserNotLoggedIn();
        }

    }

    // Arama işlemi için metot
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (adapter != null) { adapter.getFilter().filter(query); }
                searchView.clearFocus();
                checkSearchResultsVisibility(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) { adapter.getFilter().filter(newText); }
                checkSearchResultsVisibility(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            if (adapter != null) { adapter.getFilter().filter(""); }
            checkSearchResultsVisibility("");
            return false;
        });
    }

    // Arama sonuçlarına göre mesajları güncelleyen yardımcı metot
    private void checkSearchResultsVisibility(String query) {
        if (adapter == null || textViewNoSearchResults == null) return;

        boolean hasQuery = !TextUtils.isEmpty(query);

        boolean hasResults = adapter.getItemCount() > 0;

        textViewNoSearchResults.setVisibility(hasQuery && !hasResults ? View.VISIBLE : View.GONE);

        if (recyclerView != null) {
            recyclerView.setVisibility(hasQuery && !hasResults ? View.GONE : View.VISIBLE);
        }
    }

    // Swipe işlemi için tema renklerini ve ikonu alan metot
    private void loadThemeColorsAndIcon() {
        Context context = getContext();
        if (context == null) {
            swipeBackgroundColor = Color.LTGRAY;
            swipeIconColor = Color.DKGRAY;
            deleteIcon = null;
            return;
        }

        TypedValue typedValueBackground = new TypedValue();
        // ---- DEĞİŞİKLİK: Tam yolu kullan ----
        // Kütüphanenin R sınıfına tam yoluyla erişiyoruz:
        boolean resolvedBg = context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorErrorContainer, typedValueBackground, true);
        // ---- DEĞİŞİKLİK SONU ----
        swipeBackgroundColor = resolvedBg ? typedValueBackground.data : Color.RED; // Fallback color
        if(!resolvedBg) Log.w(TAG, "Tema niteliği 'colorErrorContainer' bulunamadı!");


        TypedValue typedValueIcon = new TypedValue();
        // ---- DEĞİŞİKLİK: Tam yolu kullan ----
        // Kütüphanenin R sınıfına tam yoluyla erişiyoruz:
        boolean resolvedIcon = context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnErrorContainer, typedValueIcon, true);
        // ---- DEĞİŞİKLİK SONU ----
        swipeIconColor = resolvedIcon ? typedValueIcon.data : Color.WHITE; // Fallback color
        if(!resolvedIcon) Log.w(TAG, "Tema niteliği 'colorOnErrorContainer' bulunamadı!");

        // Silme ikonunu al (BURADA DEĞİŞİKLİK YOK, projenin R'ı kullanılır)
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_menu_delete);
        if (deleteIcon != null) {
            // Drawable'ı değiştirebilmek için mutate() önemli!
            deleteIcon = deleteIcon.mutate();
            deleteIcon.setTint(swipeIconColor);
        } else {
            Log.w(TAG, "Silme ikonu (R.drawable.ic_menu_delete) bulunamadı!");
        }
    }

    // Swipe işlemi için metot
    private ItemTouchHelper.SimpleCallback createItemTouchHelperCallback() {
        return new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CardList listToDelete = adapter.getItemAt(position);
                    if (listToDelete != null) {
                        showDeleteConfirmationDialog(listToDelete, position);
                    } else {
                        adapter.notifyItemChanged(position);
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (getContext() == null || deleteIcon == null) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    return;
                }
                View itemView = viewHolder.itemView;
                Drawable background = new ColorDrawable(swipeBackgroundColor);
                int itemViewHeight = itemView.getHeight();
                float density = getResources().getDisplayMetrics().density;
                int desiredIconSizeDp = 24;
                int iconSizePx = (int) (desiredIconSizeDp * density);
                int iconMargin = (int) (16 * density);
                int iconTop = itemView.getTop() + (itemViewHeight - iconSizePx) / 2;
                int iconBottom = iconTop + iconSizePx;
                int iconLeft, iconRight;

                if (dX > 0) { // Sağa kaydırma
                    iconLeft = itemView.getLeft() + iconMargin;
                    iconRight = iconLeft + iconSizePx;
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX), itemView.getBottom());
                } else if (dX < 0) { // Sola kaydırma
                    iconRight = itemView.getRight() - iconMargin;
                    iconLeft = iconRight - iconSizePx;
                    background.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                } else {
                    background.setBounds(0, 0, 0, 0);
                    iconLeft = iconRight = 0;
                }
                background.draw(c);
                if (iconLeft != iconRight) {
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteIcon.draw(c);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
    }

    private void showDeleteConfirmationDialog(CardList listToDelete, int position) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Listeyi Sil")
                .setMessage("'" + listToDelete.getListName() + "' listesini silmek istediğinizden emin misiniz? Bu işlem geri alınamaz ve listedeki tüm kartlar da silinir.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Evet, Sil", (dialog, whichButton) -> deleteListFromFirebase(listToDelete, position))
                .setNegativeButton("İptal", (dialog, which) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    private void deleteListFromFirebase(CardList listToDelete, int position) {

        String listId = listToDelete.getListId();
        if (getContext() == null) return;
        if (listId == null || userListsRef == null) {
            Toast.makeText(getContext(), "Liste silinemedi (ID veya referans hatası).", Toast.LENGTH_SHORT).show();
            if(adapter != null) adapter.notifyItemChanged(position);
            return;
        }
        userListsRef.child(listId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "'" + listToDelete.getListName() + "' listesi silindi.", Toast.LENGTH_SHORT).show();
                        DatabaseReference cardsToDeleteRef = FirebaseDatabase.getInstance(dbUrl).getReference("cards").child(listId);
                        cardsToDeleteRef.removeValue().addOnCompleteListener(cardsTask -> {
                            if (cardsTask.isSuccessful()) { Log.d(TAG, "Listeye ait kartlar başarıyla silindi: " + listId); }
                            else { Log.w(TAG, "Kartlar silinirken hata oluştu: " + listId, cardsTask.getException()); }
                        });
                    } else {
                        Log.e(TAG, "Liste silinirken hata oluştu (my_lists): " + listId, task.getException());
                        Toast.makeText(getContext(), "Liste silinemedi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        if(adapter != null) adapter.notifyItemChanged(position);
                    }
                });
    }

    // Firebase'den listeleri yükle
    private void loadUserLists() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (textViewNoLists != null) textViewNoLists.setVisibility(View.GONE);
        if (textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (searchView != null) searchView.setVisibility(View.GONE);

        if (valueEventListener == null) {
            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<CardList> loadedLists = new ArrayList<>();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            try {
                                CardList cardList = snapshot.getValue(CardList.class);
                                if (cardList != null) { cardList.setListId(snapshot.getKey()); loadedLists.add(cardList); }
                            } catch (Exception e) { Log.e(TAG, "User list parse error: " + snapshot.getKey(), e); }
                        }
                    }

                    if (adapter == null) { Log.e(TAG, "CardListFragment onDataChange: Adapter is NULL!"); if(progressBar != null) progressBar.setVisibility(View.GONE); return; }

                    // Adaptöre veriyi ver
                    adapter.setOriginalData(loadedLists);

                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    // UI durumunu ilk yükleme için ayarla
                    boolean listsAreEmpty = loadedLists.isEmpty();
                    if (textViewNoLists != null) textViewNoLists.setVisibility(listsAreEmpty ? View.VISIBLE : View.GONE);
                    if (recyclerView != null) recyclerView.setVisibility(listsAreEmpty ? View.GONE : View.VISIBLE);
                    if (searchView != null) searchView.setVisibility(listsAreEmpty ? View.GONE : View.VISIBLE);
                    if (textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Firebase user list read error: ", error.toException());
                    if(progressBar != null) progressBar.setVisibility(View.GONE);
                    if(textViewNoLists != null) { textViewNoLists.setText("Listeler yüklenirken hata oluştu."); textViewNoLists.setVisibility(View.VISIBLE); }
                    if(textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE);
                    if(recyclerView != null) recyclerView.setVisibility(View.GONE);
                    if(searchView != null) searchView.setVisibility(View.GONE);
                    if(adapter != null) adapter.setOriginalData(new ArrayList<>());
                }
            };
            if (userListsRef != null) {
                userListsRef.addValueEventListener(valueEventListener);
            }
        }
    }

    private void handleUserNotLoggedIn() {
        if(progressBar != null) progressBar.setVisibility(View.GONE);
        if(textViewNoLists != null) { textViewNoLists.setText("Listelerinizi görmek için giriş yapmalısınız."); textViewNoLists.setVisibility(View.VISIBLE); }
        if(textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE);
        if(recyclerView != null) recyclerView.setVisibility(View.GONE);
        if(searchView != null) searchView.setVisibility(View.GONE);
        if(adapter != null) adapter.setOriginalData(new ArrayList<>());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListsRef != null && valueEventListener != null) {
            userListsRef.removeEventListener(valueEventListener); valueEventListener = null; Log.d(TAG, "ValueEventListener removed.");
        }
        if (itemTouchHelper != null) { itemTouchHelper.attachToRecyclerView(null); itemTouchHelper = null; Log.d(TAG,"ItemTouchHelper detached."); }
        if (searchView != null) { searchView.setOnQueryTextListener(null); Log.d(TAG, "SearchView listener cleared.");}
        recyclerView = null; adapter = null; progressBar = null; textViewNoLists = null; textViewNoSearchResults = null; searchView = null; Log.d(TAG, "View references nulled.");
    }
}