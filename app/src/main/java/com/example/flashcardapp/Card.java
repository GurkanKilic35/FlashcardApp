package com.example.flashcardapp; // Kendi paket adınız

public class Card {
    private String cardId; // Firebase'deki unique ID
    private String front;  // Kartın ön yüzü
    private String back;   // Kartın arka yüzü

    // Firebase için boş constructor
    public Card() {
    }

    public Card(String front, String back) {
        this.front = front;
        this.back = back;
    }

    // --- Getter'lar ---
    public String getCardId() {
        return cardId;
    }

    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

    // --- Setter'lar ---
    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public void setBack(String back) {
        this.back = back;
    }
}