
package com.example.messloc.ui.admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.data.model.admin.AdminMess;

import java.util.ArrayList;
import java.util.List;

public class AdminMessAdapter
        extends RecyclerView.Adapter<AdminMessAdapter.MessViewHolder> {

    public interface MessActionListener {
        void onApprove(AdminMess mess);
        void onReject(AdminMess mess);
    }

    private final List<AdminMess> messList = new ArrayList<>();
    private final MessActionListener listener;

    public AdminMessAdapter(MessActionListener listener) {
        this.listener = listener;
    }

    public void setMesses(List<AdminMess> messes) {
        messList.clear();

        if (messes != null) {
            messList.addAll(messes);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_mess, parent, false);

        return new MessViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MessViewHolder holder,
            int position
    ) {
        AdminMess mess = messList.get(position);

        holder.tvMessName.setText(
                mess.getName() != null ? mess.getName() : "Unnamed Mess"
        );

        holder.tvMessId.setText("ID: " + mess.getId());

        holder.tvMessStatus.setText(
                mess.getVerificationStatus() != null
                        ? mess.getVerificationStatus()
                        : "UNKNOWN"
        );

        holder.btnApprove.setOnClickListener(v ->
                listener.onApprove(mess)
        );

        holder.btnReject.setOnClickListener(v ->
                listener.onReject(mess)
        );
    }

    @Override
    public int getItemCount() {
        return messList.size();
    }

    static class MessViewHolder extends RecyclerView.ViewHolder {

        TextView tvMessName;
        TextView tvMessId;
        TextView tvMessStatus;
        Button btnApprove;
        Button btnReject;

        public MessViewHolder(@NonNull View itemView) {
            super(itemView);

            tvMessName = itemView.findViewById(R.id.tvMessName);
            tvMessId = itemView.findViewById(R.id.tvMessId);
            tvMessStatus = itemView.findViewById(R.id.tvMessStatus);
            btnApprove = itemView.findViewById(R.id.btnApproveMess);
            btnReject = itemView.findViewById(R.id.btnRejectMess);
        }
    }
}
