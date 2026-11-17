
package com.example.messloc.ui.subscriptions.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.data.model.subscription.Subscription;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClick(Subscription subscription);
    }

    private final List<Subscription> subscriptions = new ArrayList<>();
    private final OnCancelClickListener listener;

    public SubscriptionAdapter(OnCancelClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubscriptionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subscription, parent, false);

        return new SubscriptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SubscriptionViewHolder holder,
            int position
    ) {
        Subscription subscription = subscriptions.get(position);
        holder.bind(subscription, listener);
    }

    @Override
    public int getItemCount() {
        return subscriptions.size();
    }

    public void setSubscriptions(List<Subscription> newSubscriptions) {
        subscriptions.clear();

        if (newSubscriptions != null) {
            subscriptions.addAll(newSubscriptions);
        }

        notifyDataSetChanged();
    }

    static class SubscriptionViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvMessName;
        private final TextView tvStatus;
        private final TextView tvPlan;
        private final TextView tvPrice;
        private final TextView tvDates;
        private final MaterialButton btnCancel;

        public SubscriptionViewHolder(@NonNull View itemView) {
            super(itemView);

            tvMessName = itemView.findViewById(R.id.tvSubscriptionMessName);
            tvStatus = itemView.findViewById(R.id.tvSubscriptionStatus);
            tvPlan = itemView.findViewById(R.id.tvSubscriptionPlan);
            tvPrice = itemView.findViewById(R.id.tvSubscriptionPrice);
            tvDates = itemView.findViewById(R.id.tvSubscriptionDates);
            btnCancel = itemView.findViewById(R.id.btnCancelSubscription);
        }

        void bind(
                Subscription subscription,
                OnCancelClickListener listener
        ) {
            tvMessName.setText(
                    subscription.getMessName() != null
                            ? subscription.getMessName()
                            : "Mess"
            );

            tvPlan.setText(
                    subscription.getPlanName() != null
                            ? subscription.getPlanName()
                            : "Subscription Plan"
            );

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(
                    new Locale.Builder()
                            .setLanguage("en")
                            .setRegion("IN")
                            .build()
            );

            tvPrice.setText(currencyFormat.format(subscription.getPrice()));

            String startDate = subscription.getStartDate() != null
                    ? subscription.getStartDate() : "--";

            String endDate = subscription.getEndDate() != null
                    ? subscription.getEndDate() : "--";

            tvDates.setText(startDate + "  –  " + endDate);

            String status = subscription.getStatus() != null
                    ? subscription.getStatus().toUpperCase(Locale.ROOT)
                    : "UNKNOWN";

            tvStatus.setText(status);

            int statusColor;

            switch (status) {
                case "ACTIVE":
                    statusColor = android.R.color.holo_green_dark;
                    break;

                case "CANCELLED":
                    statusColor = android.R.color.holo_red_dark;
                    break;

                case "EXPIRED":
                    statusColor = android.R.color.darker_gray;
                    break;

                default:
                    statusColor = android.R.color.darker_gray;
                    break;
            }

            tvStatus.setTextColor(
                    ContextCompat.getColor(itemView.getContext(), statusColor)
            );

            boolean isActive = "ACTIVE".equals(status);

            btnCancel.setVisibility(
                    isActive ? View.VISIBLE : View.GONE
            );

            btnCancel.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancelClick(subscription);
                }
            });
        }
    }
}
