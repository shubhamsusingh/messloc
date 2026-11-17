
package com.example.messloc.ui.mess.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.data.model.menu.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class DailyMealAdapter extends RecyclerView.Adapter<DailyMealAdapter.MealViewHolder> {

    private final List<MealSection> meals = new ArrayList<>();

    public static class MealSection {
        private final String name;
        private final String icon;
        private final List<MenuItem> items;

        public MealSection(String name, String icon, List<MenuItem> items) {
            this.name = name;
            this.icon = icon;
            this.items = items;
        }
    }

    public void setMeals(List<MealSection> newMeals) {
        meals.clear();

        if (newMeals != null) {
            meals.addAll(newMeals);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_meal, parent, false);

        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MealViewHolder holder,
            int position
    ) {
        MealSection meal = meals.get(position);

        holder.tvMealIcon.setText(meal.icon);
        holder.tvMealName.setText(meal.name);

        List<MenuItem> items = meal.items;

        int count = items == null ? 0 : items.size();

        holder.tvMealCount.setText(
                count + (count == 1 ? " item" : " items")
        );

        StringBuilder content = new StringBuilder();

        if (items == null || items.isEmpty()) {
            content.append("No items added yet");
        } else {
            for (MenuItem item : items) {
                content.append("• ")
                        .append(item.getFoodName());

                if (item.getDescription() != null
                        && !item.getDescription().trim().isEmpty()) {
                    content.append("\n  ")
                            .append(item.getDescription());
                }

                content.append("\n\n");
            }
        }

        holder.tvMealItems.setText(content.toString().trim());
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {

        TextView tvMealIcon;
        TextView tvMealName;
        TextView tvMealCount;
        TextView tvMealItems;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);

            tvMealIcon = itemView.findViewById(R.id.tvMealIcon);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvMealCount = itemView.findViewById(R.id.tvMealCount);
            tvMealItems = itemView.findViewById(R.id.tvMealItems);
        }
    }
}
