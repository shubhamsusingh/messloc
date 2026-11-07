package com.example.messloc.ui.home;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.data.model.Mess;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessAdapter extends RecyclerView.Adapter<MessAdapter.MessViewHolder> {

    public interface OnMessClickListener {
        void onMessClick(Mess mess);
    }

    private static final int COLOR_VEG = Color.parseColor("#2E7D32");
    private static final int COLOR_NON_VEG = Color.parseColor("#C62828");

    private final List<Mess> messList = new ArrayList<>();
    @Nullable
    private OnMessClickListener clickListener;

    public void setOnMessClickListener(@Nullable OnMessClickListener listener) {
        this.clickListener = listener;
    }

    public void setMesses(List<Mess> messes) {
        messList.clear();

        if (messes != null) {
            messList.addAll(messes);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mess, parent, false);

        return new MessViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MessViewHolder holder, int position) {

        Mess mess = messList.get(position);

        holder.tvName.setText(mess.getName());

        holder.tvLocation.setText(
                mess.getAddress() + ", " + mess.getCity()
        );

        // Food type badge
        boolean isVeg = "VEG".equalsIgnoreCase(mess.getFoodType());
        int typeColor = isVeg ? COLOR_VEG : COLOR_NON_VEG;
        holder.tvFoodType.setText(isVeg ? "Veg" : "Non-Veg");
        holder.tvFoodType.setTextColor(typeColor);
        holder.ivVegMark.setColorFilter(typeColor);

        // Timing
        holder.tvTiming.setText(
                formatTime(mess.getOpeningTime()) + " – " + formatTime(mess.getClosingTime())
        );

        // Description
        String description = mess.getDescription();
        if (description == null || description.trim().isEmpty()) {
            holder.tvDescription.setVisibility(View.GONE);
        } else {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(description);
        }

        // Facility chips
        holder.layoutFacilities.removeAllViews();
        List<String> facilities = mess.getFacilities();
        if (facilities != null) {
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
            for (String facility : facilities) {
                TextView chip = (TextView) inflater.inflate(
                        R.layout.item_facility_chip, holder.layoutFacilities, false);
                chip.setText(facility);
                holder.layoutFacilities.addView(chip);
            }
        }

        // Image: placeholder is set in XML for now.
        // Later, with an imageUrl field on Mess (and Glide added):
        // Glide.with(holder.itemView).load(mess.getImageUrl())
        //         .placeholder(R.drawable.ic_mess_placeholder)
        //         .centerCrop().into(holder.ivMessImage);

        // Price
        holder.tvPrice.setText(
                String.format(Locale.getDefault(), "₹%,.0f", mess.getMonthlyPrice())
        );

        // Clicks (whole card + button)
        View.OnClickListener onClick = v -> {
            if (clickListener != null) {
                clickListener.onMessClick(mess);
            }
        };
        holder.itemView.setOnClickListener(onClick);
        holder.btnViewMess.setOnClickListener(onClick);
    }

    @Override
    public int getItemCount() {
        return messList.size();
    }

    /** Converts "07:30" -> "7:30 AM", "22:00" -> "10:00 PM". Works on any API level. */
    private static String formatTime(@Nullable String time24) {
        if (time24 == null || !time24.contains(":")) {
            return time24 == null ? "--" : time24;
        }
        try {
            String[] parts = time24.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            String amPm = hour >= 12 ? "PM" : "AM";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm);
        } catch (NumberFormatException e) {
            return time24;
        }
    }

    static class MessViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvLocation, tvFoodType, tvTiming,
                tvDescription, tvPrice, btnViewMess;
        ImageView ivVegMark, ivMessImage;
        LinearLayout layoutFacilities;

        public MessViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvMessName);
            tvLocation = itemView.findViewById(R.id.tvMessLocation);
            tvFoodType = itemView.findViewById(R.id.tvFoodType);
            ivMessImage = itemView.findViewById(R.id.ivMessImage);
            ivVegMark = itemView.findViewById(R.id.ivVegMark);
            tvTiming = itemView.findViewById(R.id.tvMessTiming);
            tvDescription = itemView.findViewById(R.id.tvMessDescription);
            layoutFacilities = itemView.findViewById(R.id.layoutFacilities);
            tvPrice = itemView.findViewById(R.id.tvMessPrice);
            btnViewMess = itemView.findViewById(R.id.btnViewMess);
        }
    }
}