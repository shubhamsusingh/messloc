
package com.example.messloc.ui.admin.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.data.model.admin.AdminOwner;

import java.util.ArrayList;
import java.util.List;

public class AdminOwnerAdapter
        extends RecyclerView.Adapter<AdminOwnerAdapter.OwnerViewHolder> {

    public interface OwnerActionListener {
        void onVerify(AdminOwner owner);
        void onBlock(AdminOwner owner);
    }

    private final List<AdminOwner> ownerList = new ArrayList<>();
    private final OwnerActionListener listener;

    public AdminOwnerAdapter(OwnerActionListener listener) {
        this.listener = listener;
    }

    public void setOwners(List<AdminOwner> owners) {
        ownerList.clear();

        if (owners != null) {
            ownerList.addAll(owners);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OwnerViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_owner, parent, false);

        return new OwnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull OwnerViewHolder holder,
            int position
    ) {
        AdminOwner owner = ownerList.get(position);
        Log.d("OWNER_ADAPTER", "Binding position: " + position);


        holder.tvName.setText(valueOrFallback(owner.getName(), "Unnamed Owner"));
        holder.tvEmail.setText(valueOrFallback(owner.getEmail(), "Email unavailable"));
        holder.tvPhone.setText("Phone: " + valueOrFallback(owner.getPhone(), "N/A"));
        holder.tvCity.setText("City: " + valueOrFallback(owner.getCity(), "N/A"));
        holder.tvStatus.setText(valueOrFallback(owner.getStatus(), "UNKNOWN"));

        holder.btnVerify.setOnClickListener(v -> listener.onVerify(owner));
        holder.btnBlock.setOnClickListener(v -> listener.onBlock(owner));
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @Override
    public int getItemCount() {
        return ownerList.size();
    }

    static class OwnerViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvEmail;
        TextView tvPhone;
        TextView tvCity;
        TextView tvStatus;
        Button btnVerify;
        Button btnBlock;

        OwnerViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvOwnerName);
            tvEmail = itemView.findViewById(R.id.tvOwnerEmail);
            tvPhone = itemView.findViewById(R.id.tvOwnerPhone);
            tvCity = itemView.findViewById(R.id.tvOwnerCity);
            tvStatus = itemView.findViewById(R.id.tvOwnerStatus);
            btnVerify = itemView.findViewById(R.id.btnVerifyOwner);
            btnBlock = itemView.findViewById(R.id.btnBlockOwner);
        }
    }
}
