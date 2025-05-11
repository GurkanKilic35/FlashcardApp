package com.example.flashcardapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;

public class CardPagerAdapter extends FragmentStateAdapter {

    private List<Card> cards = new ArrayList<>();

    public CardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void setCards(List<Card> newCards) {
        this.cards.clear();
        if (newCards != null) {
            this.cards.addAll(newCards);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (isValidPosition(position)) {
            Card card = cards.get(position);
            return CardFragment.newInstance(card.getFront(), card.getBack());
        }
        return new Fragment();
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public Card getCardAt(int position) {
        if (isValidPosition(position)) {
            return cards.get(position);
        }
        return null;
    }

    // Kart ekleme
    public void addCard(Card card) {
        cards.add(card);
        notifyItemInserted(cards.size() - 1);
    }

    // Kart silme
    public void removeCard(int position) {
        if (isValidPosition(position)) {
            cards.remove(position);
            notifyItemRemoved(position);
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

    // Mevcut kart listesini almak için
    public List<Card> getCurrentList() {
        return cards;
    }
}