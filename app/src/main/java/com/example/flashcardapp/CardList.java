package com.example.flashcardapp;

public class CardList {
    private String listId;
    private String listName;
    private String description;
    private String createdByUid; // YENİ: Listeyi oluşturan kullanıcının UID'si

    public CardList() { } // Boş constructor

    public CardList(String listName, String description) {
        this.listName = listName;
        this.description = description;
    }

    // --- Mevcut Getter/Setterlar ---
    public String getListId() { return listId; }
    public void setListId(String listId) { this.listId = listId; }
    public String getListName() { return listName; }
    public void setListName(String listName) { this.listName = listName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // --- YENİ Getter/Setter ---
    public String getCreatedByUid() { return createdByUid; }
    public void setCreatedByUid(String createdByUid) { this.createdByUid = createdByUid; }
}