package com.example.flashcardapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CardListAdapter extends RecyclerView.Adapter<CardListAdapter.CardListViewHolder> implements Filterable {

    private Context context;
    private List<CardList> cardListsFiltered;
    private List<CardList> cardListsOriginal;

    public CardListAdapter(Context context, List<CardList> initialList) {
        this.context = context;
        this.cardListsOriginal = new ArrayList<>(initialList);
        this.cardListsFiltered = new ArrayList<>(initialList);
    }

    @NonNull
    @Override
    public CardListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new CardListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardListViewHolder holder, int position) {
        CardList currentList = cardListsFiltered.get(position);
        holder.textViewListName.setText(currentList.getListName());
        holder.textViewListDescription.setText(currentList.getDescription());

        holder.itemView.setOnClickListener(v -> {
            CardList clickedList = cardListsFiltered.get(holder.getAdapterPosition());
            if (clickedList != null && clickedList.getListId() != null) {
                Intent intent = new Intent(context, ListDetailActivity.class);
                intent.putExtra("LIST_ID", clickedList.getListId());
                intent.putExtra("LIST_NAME", clickedList.getListName());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardListsFiltered.size();
    }

    public void setOriginalData(List<CardList> newOriginalList) {
        if (newOriginalList != null) {
            this.cardListsOriginal = new ArrayList<>(newOriginalList);
            this.cardListsFiltered = new ArrayList<>(newOriginalList);
            notifyDataSetChanged();
        }
    }

    public List<CardList> getCurrentList() { return cardListsFiltered; }

    public CardList getItemAt(int position) {
        if (position >= 0 && position < cardListsFiltered.size()) {
            return cardListsFiltered.get(position);
        }
        return null;
    }

    // Bu metot, Swipe-to-delete'i doğru implemente etmek için
    // Fragment'tan çağrılıp hem orijinal hem filtrelenmiş listeden silmeli
    // VEYA sadece görsel silme yapıp liste yenilenmeli.
    // Şimdilik sadece görsel silme olarak bırakalım.
    public void removeItemVisually(int position) {
        if (position >= 0 && position < cardListsFiltered.size()) {
            cardListsFiltered.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, cardListsFiltered.size());
        }
    }

    @Override
    public Filter getFilter() {
        return cardFilter;
    }

    private Filter cardFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CardList> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(cardListsOriginal);
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                for (CardList item : cardListsOriginal) {
                    if ((item.getListName() != null && item.getListName().toLowerCase(Locale.getDefault()).contains(filterPattern)) ||
                            (item.getDescription() != null && item.getDescription().toLowerCase(Locale.getDefault()).contains(filterPattern))) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            cardListsFiltered.clear();
            if (results.values != null) {
                cardListsFiltered.addAll((List<CardList>) results.values);
            }
            notifyDataSetChanged();
        }
    };

    // ============================================ //
    // === CardListViewHolder SINIFI BURADA OLMALI === //
    // ============================================ //
    public static class CardListViewHolder extends RecyclerView.ViewHolder {
        TextView textViewListName;
        TextView textViewListDescription;

        public CardListViewHolder(@NonNull View itemView) {
            super(itemView);
            // ID'lerin list_item.xml'deki ile eşleştiğinden emin olun
            textViewListName = itemView.findViewById(R.id.textViewListName);
            textViewListDescription = itemView.findViewById(R.id.textViewListDescription);
        }
    }
    // ============================================ //

} // Adapter Sonu