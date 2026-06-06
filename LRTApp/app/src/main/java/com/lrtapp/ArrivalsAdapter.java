package com.lrtapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern Card-based adapter for LRT arrivals.
 * Groups arrivals by platform into CardView items.
 */
public class ArrivalsAdapter extends RecyclerView.Adapter<ArrivalsAdapter.CardViewHolder> {

    private List<PlatformCard> cards = new ArrayList<>();

    /** A platform card = platform header + list of arrivals */
    static class PlatformCard {
        int platformId;
        List<Arrival> arrivals;

        PlatformCard(int platformId, List<Arrival> arrivals) {
            this.platformId = platformId;
            this.arrivals = arrivals;
        }
    }

    public void setArrivals(List<Arrival> list) {
        if (list == null || list.isEmpty()) {
            cards = new ArrayList<>();
            notifyDataSetChanged();
            return;
        }

        // Group arrivals by platform
        Map<Integer, List<Arrival>> grouped = new LinkedHashMap<>();
        int currentPlatform = -1;
        List<Arrival> currentList = null;

        for (Arrival a : list) {
            if (a == null) continue;
            if (a.isPlatformHeader) {
                currentPlatform = a.platformId;
                currentList = new ArrayList<>();
                grouped.put(currentPlatform, currentList);
            } else if (currentPlatform > 0 && currentList != null) {
                currentList.add(a);
            }
        }

        // Build card list
        cards = new ArrayList<>();
        for (Map.Entry<Integer, List<Arrival>> entry : grouped.entrySet()) {
            cards.add(new PlatformCard(entry.getKey(), entry.getValue()));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_platform_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.bind(cards.get(position));
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView platformHeader;
        LinearLayout cardContent; // Contains arrival rows

        CardViewHolder(View itemView) {
            super(itemView);
            platformHeader = itemView.findViewById(R.id.platformHeader);
            cardContent = itemView.findViewById(R.id.cardContent);
        }

        void bind(PlatformCard card) {
            platformHeader.setText("月台 " + card.platformId);

            // Remove old arrival rows except the first one (template)
            while (cardContent.getChildCount() > 1) {
                cardContent.removeViewAt(cardContent.getChildCount() - 1);
            }

            // Get the template arrival row
            View templateRow = cardContent.getChildAt(0);

            if (card.arrivals == null || card.arrivals.isEmpty()) {
                templateRow.setVisibility(View.GONE);
                return;
            }

            // Bind first arrival to template
            bindRow(templateRow, card.arrivals.get(0));
            templateRow.setVisibility(View.VISIBLE);

            // Add additional rows
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            for (int i = 1; i < card.arrivals.size(); i++) {
                View row = inflater.inflate(R.layout.item_arrival_row, cardContent, false);
                bindRow(row, card.arrivals.get(i));
                cardContent.addView(row);
            }

            // Add "no more arrivals" for empty platform
            if (card.arrivals.size() <= 1) {
                addNoMoreRow();
            }
        }

        private void bindRow(View row, Arrival arrival) {
            TextView routeText = row.findViewById(R.id.routeText);
            TextView destinationText = row.findViewById(R.id.destinationText);
            TextView arrivalText = row.findViewById(R.id.arrivalText);
            TextView trainLengthIcon = row.findViewById(R.id.trainLengthIcon);

            String routeNo = arrival.getRouteNo();
            routeText.setText(routeNo);
            com.lrtapp.RouteColors.applyBadge(routeText, routeNo);

            destinationText.setText(arrival.getDestinationTc());

            String timeStr = arrival.getArrivalDisplay();
            if (timeStr != null && timeStr.equals("-")) {
                arrivalText.setTextColor(0xFF999999);
            } else {
                int routeColor = com.lrtapp.RouteColors.getTimeColor(routeNo);
                arrivalText.setTextColor(routeColor);
            }
            arrivalText.setText(timeStr != null ? timeStr : "-");

            if (arrival.trainLength >= 2) {
                trainLengthIcon.setText("🚃🚃");
                trainLengthIcon.setTextSize(16f);
            } else {
                trainLengthIcon.setText("🚃");
                trainLengthIcon.setTextSize(16f);
            }
        }

        private void addNoMoreRow() {
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            View row = inflater.inflate(R.layout.item_arrival_row, cardContent, false);
            row.findViewById(R.id.routeText).setVisibility(View.GONE);
            row.findViewById(R.id.trainLengthIcon).setVisibility(View.GONE);
            row.findViewById(R.id.arrivalText).setVisibility(View.GONE);
            TextView dest = row.findViewById(R.id.destinationText);
            dest.setText("(暫無後續班次)");
            dest.setTextSize(12);
            dest.setTextColor(0xFFBBBBBB);
            dest.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            dest.setPadding(0, 8, 0, 8);
            cardContent.addView(row);
        }
    }
}
