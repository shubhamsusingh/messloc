package com.example.messloc.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.data.model.owner.OwnerMess;
import com.example.messloc.ui.owner.OwnerMessDetailsActivity;
import com.example.messloc.viewmodel.OwnerMessViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.messloc.ui.owner.RegisterMessActivity;
import java.util.ArrayList;
import java.util.List;

public class MyMessesActivity extends AppCompatActivity {

    private RecyclerView rvMyMesses;
    private ProgressBar progressMyMesses;
    private LinearLayout layoutEmptyMesses, layoutMessError;
    private TextView tvMessError;
    private Button btnRetry;
    private MaterialToolbar toolbar;

    private OwnerMessViewModel viewModel;
    public interface OnMessClickListener {
        void onMessClick(OwnerMess mess);
    }
    private OwnerMessAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_messes);

        initializeViews();
        setupRecyclerView();
        setupViewModel();
        setupListeners();

        View root = findViewById(R.id.myMessesRoot);
        toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
            );

            view.setPadding(
                    view.getPaddingLeft(),
                    insets.top,
                    view.getPaddingRight(),
                    view.getPaddingBottom()
            );

            return windowInsets;
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (viewModel != null) {
            viewModel.loadMyMesses();
        }
    }
    private void initializeViews() {

//        ImageView btnBack = findViewById(R.id.btnBack);
        toolbar = findViewById(R.id.toolbar);
        rvMyMesses = findViewById(R.id.rvMyMesses);
        progressMyMesses = findViewById(R.id.progressMyMesses);
        layoutEmptyMesses = findViewById(R.id.layoutEmptyMesses);
        layoutMessError = findViewById(R.id.layoutMessError);
        tvMessError = findViewById(R.id.tvMessError);
        btnRetry = findViewById(R.id.btnRetry);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());

        toolbar.setOnMenuItemClickListener(item -> {

            if (item.getItemId() == R.id.action_add_mess) {
                openAddMessScreen();
                return true;
            }

            return false;
        });
    }
    private void openAddMessScreen() {
        Intent intent = new Intent(
                MyMessesActivity.this,
                RegisterMessActivity.class
        );

        startActivity(intent);
    }
    private void setupRecyclerView() {

        adapter = new OwnerMessAdapter(mess -> {
            Intent intent = new Intent(
                    MyMessesActivity.this,
                    OwnerMessDetailsActivity.class
            );

            intent.putExtra("mess_id", mess.getId());
            startActivity(intent);
        });

        rvMyMesses.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvMyMesses.setAdapter(adapter);
    }

    private void setupViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(OwnerMessViewModel.class);

        viewModel.getMyMesses().observe(this, messes -> {

            if (messes == null || messes.isEmpty()) {
                adapter.setMesses(new ArrayList<>());
                showEmpty();
            } else {
                adapter.setMesses(messes);
                showList();
            }
        });

        viewModel.getLoading().observe(this, loading -> {

            if (Boolean.TRUE.equals(loading)) {
                showLoading();
            }
        });

        viewModel.getError().observe(this, error -> {

            if (error != null && !error.trim().isEmpty()) {
                showError(error);
            }
        });
    }

    private void setupListeners() {

        btnRetry.setOnClickListener(v -> {
            viewModel.loadMyMesses();
        });
    }

    private void showLoading() {
        progressMyMesses.setVisibility(View.VISIBLE);
        rvMyMesses.setVisibility(View.GONE);
        layoutEmptyMesses.setVisibility(View.GONE);
        layoutMessError.setVisibility(View.GONE);
    }

    private void showList() {
        progressMyMesses.setVisibility(View.GONE);
        rvMyMesses.setVisibility(View.VISIBLE);
        layoutEmptyMesses.setVisibility(View.GONE);
        layoutMessError.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressMyMesses.setVisibility(View.GONE);
        rvMyMesses.setVisibility(View.GONE);
        layoutEmptyMesses.setVisibility(View.VISIBLE);
        layoutMessError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressMyMesses.setVisibility(View.GONE);
        rvMyMesses.setVisibility(View.GONE);
        layoutEmptyMesses.setVisibility(View.GONE);
        layoutMessError.setVisibility(View.VISIBLE);
        tvMessError.setText(message);
    }

    // RecyclerView Adapter
    private static class OwnerMessAdapter
            extends RecyclerView.Adapter<OwnerMessAdapter.MessViewHolder> {

        private final List<OwnerMess> messes = new ArrayList<>();
        private final OnMessClickListener listener;

        OwnerMessAdapter(OnMessClickListener listener) {
            this.listener = listener;
        }
        void setMesses(List<OwnerMess> newMesses) {
            messes.clear();

            if (newMesses != null) {
                messes.addAll(newMesses);
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
                    .inflate(R.layout.item_owner_mess, parent, false);

            return new MessViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull MessViewHolder holder,
                int position
        ) {
            OwnerMess mess = messes.get(position);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessClick(mess);
                }
            });
            holder.tvMessName.setText(
                    valueOrFallback(mess.getName(), "Unnamed Mess")
            );

            holder.tvMessCity.setText(
                    valueOrFallback(mess.getCity(), "City not available")
            );

            Double price = mess.getMonthlyPrice();

            holder.tvMessPrice.setText(
                    price == null
                            ? "Monthly price not available"
                            : "₹" + price + " / month"
            );

            String verification = valueOrFallback(
                    mess.getVerificationStatus(), "UNKNOWN"
            );

            String status = valueOrFallback(
                    mess.getStatus(), "UNKNOWN"
            );

            holder.tvVerificationStatus.setText(verification);
            holder.tvOperationalStatus.setText(status);

            holder.tvVerificationStatus.setBackgroundColor(
                    getVerificationColor(verification)
            );

            holder.tvVerificationStatus.setTextColor(
                    getVerificationTextColor(verification)
            );

            holder.tvOperationalStatus.setBackgroundColor(
                    "ACTIVE".equalsIgnoreCase(status)
                            ? 0xFFE8F5E9
                            : 0xFFFFEBEE
            );

            holder.tvOperationalStatus.setTextColor(
                    "ACTIVE".equalsIgnoreCase(status)
                            ? 0xFF2E7D32
                            : 0xFFC62828
            );
        }

        @Override
        public int getItemCount() {
            return messes.size();
        }

        private static String valueOrFallback(
                String value,
                String fallback
        ) {
            return value == null || value.trim().isEmpty()
                    ? fallback
                    : value;
        }

        private static int getVerificationColor(String status) {

            if ("APPROVED".equalsIgnoreCase(status)) {
                return 0xFFE8F5E9;
            }

            if ("REJECTED".equalsIgnoreCase(status)) {
                return 0xFFFFEBEE;
            }

            return 0xFFFFF3E0;
        }

        private static int getVerificationTextColor(String status) {

            if ("APPROVED".equalsIgnoreCase(status)) {
                return 0xFF2E7D32;
            }

            if ("REJECTED".equalsIgnoreCase(status)) {
                return 0xFFC62828;
            }

            return 0xFFE65100;
        }

        static class MessViewHolder extends RecyclerView.ViewHolder {

            TextView tvMessName;
            TextView tvMessCity;
            TextView tvMessPrice;
            TextView tvVerificationStatus;
            TextView tvOperationalStatus;

            MessViewHolder(@NonNull View itemView) {
                super(itemView);

                tvMessName = itemView.findViewById(R.id.tvMessName);
                tvMessCity = itemView.findViewById(R.id.tvMessCity);
                tvMessPrice = itemView.findViewById(R.id.tvMessPrice);
                tvVerificationStatus = itemView.findViewById(
                        R.id.tvVerificationStatus
                );
                tvOperationalStatus = itemView.findViewById(
                        R.id.tvOperationalStatus
                );
            }
        }
    }
}
