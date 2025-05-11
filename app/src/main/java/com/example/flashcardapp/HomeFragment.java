package com.example.flashcardapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private RecyclerView recyclerView;
    private CardListAdapter adapter;
    private ProgressBar progressBar;
    private TextView textViewNoLists;
    private TextView textViewNoSearchResults;
    private SearchView searchView;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    private String dbUrl = "https://flashcardapp-2fa06-default-rtdb.europe-west1.firebasedatabase.app/";

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "Inflating layout: R.layout.fragment_home");
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated started.");

        recyclerView = view.findViewById(R.id.recyclerViewHome);
        progressBar = view.findViewById(R.id.progressBarHome);
        textViewNoLists = view.findViewById(R.id.textViewNoLists);
        textViewNoSearchResults = view.findViewById(R.id.textViewNoSearchResults);
        searchView = view.findViewById(R.id.searchViewHome);

        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new CardListAdapter(requireContext(), new ArrayList<>());
            recyclerView.setAdapter(adapter);
        } else {
            if (textViewNoLists != null) {
                textViewNoLists.setText("Arayüz yüklenirken hata oluştu.");
                textViewNoLists.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (searchView != null) {
            setupSearchView();
        }

        databaseReference = FirebaseDatabase.getInstance(dbUrl).getReference("public_lists");

        loadPublicLists();
    }

    // Arama işlemi için metot
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { // Metin gönderildiğinde filtrele
                if (adapter != null) {
                    adapter.getFilter().filter(query);
                }
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) { // Metin değiştikçe filtrele

                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return true;
            }
        });

        // SearchView kapandığında filtreyi temizle
        searchView.setOnCloseListener(() -> {
            if (adapter != null) {
                adapter.getFilter().filter("");
            }
            checkSearchResults("");
            return false;
        });
    }

    // Arama sonuçlarına göre mesajları güncelleyen yardımcı metot
    private void checkSearchResults(String query) {
        if (adapter == null) return;

        boolean hasQuery = query != null && !query.isEmpty();
        boolean hasResults = adapter.getItemCount() > 0;

        if (textViewNoSearchResults != null) {
            textViewNoSearchResults.setVisibility(hasQuery && !hasResults ? View.VISIBLE : View.GONE);
        }

        if (recyclerView != null) {
            if (hasQuery && !hasResults) {
                recyclerView.setVisibility(View.GONE);
            } else if (!hasQuery && adapter.getItemCount() > 0) {
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    // Firebase'den public listeleri yükle
    private void loadPublicLists() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        if (textViewNoLists != null) textViewNoLists.setVisibility(View.GONE);

        if (textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE);

        if (recyclerView != null) recyclerView.setVisibility(View.GONE);

        if (valueEventListener == null) {
            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<CardList> originalLists = new ArrayList<>();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            try {
                                CardList cardList = snapshot.getValue(CardList.class);
                                if (cardList != null) {
                                    cardList.setListId(snapshot.getKey());
                                    originalLists.add(cardList);
                                }
                            } catch (Exception e) { Log.e(TAG, "Data parsing error: " + snapshot.getKey(), e); }
                        }
                    }
                    Log.d(TAG, "HomeFragment onDataChange: Firebase'den " + originalLists.size() + " liste çekildi.");

                    if (adapter != null) {
                        adapter.setOriginalData(originalLists);
                        Log.d(TAG, "HomeFragment onDataChange: Adapter data set. Item count: " + adapter.getItemCount());
                    } else {
                        Log.e(TAG, "HomeFragment onDataChange: Adapter is NULL!");
                    }

                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    boolean listsAreEmpty = originalLists.isEmpty();

                    if (textViewNoLists != null) textViewNoLists.setVisibility(listsAreEmpty ? View.VISIBLE : View.GONE);
                    if (textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE);
                    if (recyclerView != null) recyclerView.setVisibility(listsAreEmpty ? View.GONE : View.VISIBLE);
                    if (searchView != null) searchView.setVisibility(listsAreEmpty ? View.GONE : View.VISIBLE);

                    Log.d(TAG, "HomeFragment onDataChange: Visibility updated. RecyclerView visible: " + !listsAreEmpty);

                    if (searchView != null && TextUtils.isEmpty(searchView.getQuery())) {
                        checkSearchResults("");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (textViewNoLists != null) {
                        textViewNoLists.setText("Veri yüklenirken hata oluştu.");
                        textViewNoLists.setVisibility(View.VISIBLE);
                    }
                    if (textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE);
                    if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                    if (searchView != null) searchView.setVisibility(View.GONE);
                    if(adapter != null) adapter.setOriginalData(new ArrayList<>());
                }
            };
            if (databaseReference != null) {
                databaseReference.addValueEventListener(valueEventListener);
            } else {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (textViewNoLists != null) {
                    textViewNoLists.setText("Veritabanı bağlantı hatası.");
                    textViewNoLists.setVisibility(View.VISIBLE);
                }
            }
        } else {
            Log.w(TAG, "ValueEventListener zaten var, tekrar eklenmiyor.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseReference != null && valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
            valueEventListener = null;
        }
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
        }
        recyclerView = null;
        adapter = null;
        progressBar = null;
        textViewNoLists = null;
        textViewNoSearchResults = null;
        searchView = null;
    }
}