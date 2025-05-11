package com.example.flashcardapp;

public class Card {
    private String cardId;
    private String front;
    private String back;

    public Card() {
    }

    public Card(String front, String back) {
        this.front = front;
        this.back = back;
    }

    public String getCardId() {
        return cardId;
    }

    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

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