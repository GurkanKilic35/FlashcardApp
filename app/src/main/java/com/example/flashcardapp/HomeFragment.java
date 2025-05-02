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
import androidx.appcompat.widget.SearchView; // SearchView import'u
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
    private TextView textViewNoLists; // Başlangıçta hiç liste yoksa
    private TextView textViewNoSearchResults; // Arama sonucu yoksa
    private SearchView searchView;

    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;

    // Explicit DB URL (Eğer gerekliyse kullanın)
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

        // --- View ID'lerini Bulma ve Kontrol Etme ---
        recyclerView = view.findViewById(R.id.recyclerViewHome);
        Log.d(TAG, "findViewById(recyclerViewHome): " + (recyclerView == null ? "NULL" : "OK"));
        progressBar = view.findViewById(R.id.progressBarHome);
        Log.d(TAG, "findViewById(progressBarHome): " + (progressBar == null ? "NULL" : "OK"));
        textViewNoLists = view.findViewById(R.id.textViewNoLists);
        Log.d(TAG, "findViewById(textViewNoLists): " + (textViewNoLists == null ? "NULL" : "OK"));
        textViewNoSearchResults = view.findViewById(R.id.textViewNoSearchResults);
        Log.d(TAG, "findViewById(textViewNoSearchResults): " + (textViewNoSearchResults == null ? "NULL" : "OK"));
        searchView = view.findViewById(R.id.searchViewHome);
        Log.d(TAG, "findViewById(searchViewHome): " + (searchView == null ? "NULL" : "OK"));

        // --- RecyclerView ve Adapter Kurulumu ---
        // Önce RecyclerView'ın null olmadığından emin ol
        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            // requireContext() null ise crash olur, bu genellikle daha iyidir
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            // Adaptörü boş liste ile başlat
            adapter = new CardListAdapter(requireContext(), new ArrayList<>());
            recyclerView.setAdapter(adapter);
            Log.d(TAG, "RecyclerView and Adapter setup completed.");
        } else {
            Log.e(TAG, "RecyclerView is NULL! Cannot proceed with setup.");
            // Hata durumunda kullanıcıya bilgi verilebilir
            if (textViewNoLists != null) {
                textViewNoLists.setText("Arayüz yüklenirken hata oluştu.");
                textViewNoLists.setVisibility(View.VISIBLE);
            }
            return; // RecyclerView yoksa devam etmenin anlamı yok
        }

        // --- SearchView Kurulumu ---
        if (searchView != null) {
            Log.d(TAG, "Calling setupSearchView...");
            setupSearchView();
        } else {
            Log.e(TAG, "SearchView is NULL! Cannot setup search.");
        }

        // --- Firebase Referansı ---
        // Explicit URL kullanmak gerekliyse: FirebaseDatabase.getInstance(dbUrl)
        databaseReference = FirebaseDatabase.getInstance(dbUrl).getReference("public_lists");

        // --- Veri Yükleme ---
        Log.d(TAG, "Calling loadPublicLists...");
        loadPublicLists();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Kullanıcı Enter'a basınca filtrelemeyi tekrar tetikleyebiliriz
                if (adapter != null) {
                    adapter.getFilter().filter(query);
                }
                searchView.clearFocus(); // Klavyeyi gizle
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Metin değiştikçe filtrele
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                // Arama sonucu yoksa mesajını yönet (Filtreleme bittikten sonra kontrol etmek daha iyi olabilir ama basit başlangıç)
                // Bu anlık çalıştığı için filtreleme bitmeden sonuç 0 olabilir,
                // en iyi çözüm adapter'dan callback almak veya publishResults sonrası kontrol.
                // Şimdilik bu kısmı yoruma alalım veya daha sonra ekleyelim.
                // checkSearchResults(newText);
                return true;
            }
        });

        // İsteğe bağlı: SearchView kapandığında filtreyi temizle
        searchView.setOnCloseListener(() -> {
            if (adapter != null) {
                adapter.getFilter().filter(""); // Filtreyi temizle
            }
            checkSearchResults(""); // Mesajları güncelle
            return false; // Varsayılan davranışı engelleme
        });
    }

    // Arama sonuçlarına göre mesajları güncelleyen yardımcı metot
    private void checkSearchResults(String query) {
        // Bu metot adaptördeki filtreleme bittikten sonra çağrılmalı.
        // Şimdilik basit bir itemCount kontrolü yapalım.
        if (adapter == null) return;

        boolean hasQuery = query != null && !query.isEmpty();
        boolean hasResults = adapter.getItemCount() > 0;

        if (textViewNoSearchResults != null) {
            // Sadece arama metni varken VE sonuç yoksa "Sonuç Yok" mesajını göster
            textViewNoSearchResults.setVisibility(hasQuery && !hasResults ? View.VISIBLE : View.GONE);
        }

        if (recyclerView != null) {
            // Eğer arama metni var VE sonuç yoksa RecyclerView'ı gizle
            // Diğer durumlarda görünürlüğü onDataChange veya adapter güncellemesi belirler.
            if (hasQuery && !hasResults) {
                recyclerView.setVisibility(View.GONE);
            } else if (!hasQuery && adapter.getItemCount() > 0) {
                // Arama yok ve veri varsa göster (onDataChange de yapar)
                recyclerView.setVisibility(View.VISIBLE);
            }
            // Eğer arama yok ve veri de yoksa onDataChange'deki textViewNoLists gösterilir.
        }
    }


    private void loadPublicLists() {
        // Null kontrolleri eklendi
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        else Log.e(TAG, "progressBar null in loadPublicLists start");

        if (textViewNoLists != null) textViewNoLists.setVisibility(View.GONE);
        else Log.e(TAG, "textViewNoLists null in loadPublicLists start");

        if (textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE);
        else Log.e(TAG, "textViewNoSearchResults null in loadPublicLists start");

        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        else Log.e(TAG, "recyclerView null in loadPublicLists start");


        if (valueEventListener == null) {
            Log.d(TAG, "Creating and attaching ValueEventListener.");
            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d(TAG, ">>> HomeFragment: onDataChange ÇAĞRILDI! <<< Snapshot var mı?: " + dataSnapshot.exists());
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

                    // Adaptör null değilse veriyi set et
                    if (adapter != null) {
                        adapter.setOriginalData(originalLists);
                        Log.d(TAG, "HomeFragment onDataChange: Adapter data set. Item count: " + adapter.getItemCount());
                    } else {
                        Log.e(TAG, "HomeFragment onDataChange: Adapter is NULL!");
                    }

                    // --- Görünürlük Ayarları (Null kontrolleriyle) ---
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    boolean listsAreEmpty = originalLists.isEmpty();

                    if (textViewNoLists != null) textViewNoLists.setVisibility(listsAreEmpty ? View.VISIBLE : View.GONE);
                    if (textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE); // Arama sonucu mesajı başlangıçta gizli
                    if (recyclerView != null) recyclerView.setVisibility(listsAreEmpty ? View.GONE : View.VISIBLE);
                    if (searchView != null) searchView.setVisibility(listsAreEmpty ? View.GONE : View.VISIBLE); // Liste yoksa arama da yok

                    Log.d(TAG, "HomeFragment onDataChange: Visibility updated. RecyclerView visible: " + !listsAreEmpty);

                    // Filtre boşsa arama sonuçları mesajını gizle
                    if (searchView != null && TextUtils.isEmpty(searchView.getQuery())) {
                        checkSearchResults(""); // "Sonuç yok" mesajını gizlemek için
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, ">>> HomeFragment: onCancelled ÇAĞRILDI! <<< Hata: " + databaseError.getMessage(), databaseError.toException());
                    // Null kontrolleriyle UI güncelle
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (textViewNoLists != null) {
                        textViewNoLists.setText("Veri yüklenirken hata oluştu.");
                        textViewNoLists.setVisibility(View.VISIBLE);
                    }
                    if (textViewNoSearchResults != null) textViewNoSearchResults.setVisibility(View.GONE);
                    if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                    if (searchView != null) searchView.setVisibility(View.GONE);
                    // Adaptör verisini temizle
                    if(adapter != null) adapter.setOriginalData(new ArrayList<>());
                }
            };
            // Database referansı null değilse listener ekle
            if (databaseReference != null) {
                Log.d(TAG, "Listener şu yola ekleniyor: " + databaseReference.toString()); // Yolu kontrol et
                databaseReference.addValueEventListener(valueEventListener);
            } else {
                Log.e(TAG, "databaseReference is NULL! Cannot add listener.");
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
        Log.d(TAG, "onDestroyView called.");
        // Listener'ı kaldır
        if (databaseReference != null && valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
            valueEventListener = null; // Referansı null yap
            Log.d(TAG, "ValueEventListener removed.");
        }
        // SearchView listener'ını temizle
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            Log.d(TAG, "SearchView listener cleared.");
        }
        // View referanslarını null yap (Memory leak önlemek için)
        recyclerView = null;
        adapter = null;
        progressBar = null;
        textViewNoLists = null;
        textViewNoSearchResults = null;
        searchView = null;
        Log.d(TAG, "View references nulled.");
    }
}