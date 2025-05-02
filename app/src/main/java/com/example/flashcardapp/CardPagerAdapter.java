package com.example.flashcardapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity; // Activity içinden kullanıyorsak
// import androidx.fragment.app.FragmentManager; // Fragment içinden kullanıyorsak
// import androidx.lifecycle.Lifecycle; // Fragment içinden kullanıyorsak
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class CardPagerAdapter extends FragmentStateAdapter {

    private List<Card> cards = new ArrayList<>();

    public CardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    // Veriyi güncelleyen metot
    public void setCards(List<Card> newCards) {
        this.cards.clear();
        if (newCards != null) {
            this.cards.addAll(newCards);
        }
        notifyDataSetChanged(); // Adaptöre değişikliği bildir (ViewPager'ı günceller)
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Belirtilen pozisyon için YENİ bir CardFragment örneği oluştur ve döndür
        if (isValidPosition(position)) {
            Card card = cards.get(position);
            return CardFragment.newInstance(card.getFront(), card.getBack());
        }
        // Geçersiz pozisyon için boş fragment (bir hata durumunda)
        return new Fragment();
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    // Activity'den belirli bir pozisyondaki kartı almak için yardımcı metot
    public Card getCardAt(int position) {
        if (isValidPosition(position)) {
            return cards.get(position);
        }
        return null;
    }

    // Kart ekleme (Listeyi güncelleyip adaptöre bildirme)
    public void addCard(Card card) {
        cards.add(card);
        notifyItemInserted(cards.size() - 1);
    }

    // Kart silme
    public void removeCard(int position) {
        if (isValidPosition(position)) {
            cards.remove(position);
            notifyItemRemoved(position);
            // Kayma nedeniyle diğer elemanların index'i değişebilir,
            // bu yüzden bazen notifyDataSetChanged() daha kolay olabilir ama daha az verimli.
            // Veya notifyItemRangeChanged(position, getItemCount());
        }
    }

    // Kart güncelleme
    public void updateCard(int position, Card updatedCard) {
        if (isValidPosition(position)) {
            cards.set(position, updatedCard);
            notifyItemChanged(position);
        }
    }

    // Geçerli pozisyon kontrolü için yardımcı metot
    private boolean isValidPosition(int position) {
        return position >= 0 && position < cards.size();
    }

    // Mevcut kart listesini almak için (ListDetailActivity'de lazım olabilir)
    public List<Card> getCurrentList() {
        return cards;
    }
}