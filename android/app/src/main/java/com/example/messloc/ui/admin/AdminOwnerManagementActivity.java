package com.example.messloc.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.data.model.admin.AdminOwner;
import com.example.messloc.ui.admin.adapter.AdminOwnerAdapter;
import com.example.messloc.viewmodel.admin.AdminViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class AdminOwnerManagementActivity extends AppCompatActivity {

    private AdminViewModel viewModel;
    private AdminOwnerAdapter adapter;

    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_owner_management);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        initializeViews();
        setupRecyclerView();

        viewModel = new ViewModelProvider(this)
                .get(AdminViewModel.class);

        observeViewModel();

        viewModel.loadPendingOwners();
        View root = findViewById(R.id.adminOwnerManagementRoot);

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

    private void initializeViews() {
        progressBar = findViewById(R.id.ownerManagementProgress);
        emptyText = findViewById(R.id.ownerManagementEmpty);
        errorText = findViewById(R.id.ownerManagementError);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rvPendingOwners);

        adapter = new AdminOwnerAdapter(new AdminOwnerAdapter.OwnerActionListener() {
            @Override
            public void onVerify(AdminOwner owner) {
                showConfirmation(
                        "Verify Owner",
                        "Verify and activate " + owner.getName() + "?",
                        () -> viewModel.verifyOwner(owner.getId())
                );
            }

            @Override
            public void onBlock(AdminOwner owner) {
                showConfirmation(
                        "Block Owner",
                        "Are you sure you want to block "
                                + owner.getName() + "?",
                        () -> viewModel.blockOwner(owner.getId())
                );
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setHasFixedSize(false);

        Log.d("OWNER_ADAPTER",
                "RecyclerView height: " + recyclerView.getHeight());
    }

    private void observeViewModel() {

        viewModel.getPendingOwners().observe(this, owners -> {

            Log.d("ADMIN_OWNERS", "LiveData updated");

            if (owners == null) {
                Log.d("ADMIN_OWNERS", "Owners list is NULL");
                return;
            }

            Log.d("ADMIN_OWNERS", "Owners received: " + owners.size());

            for (AdminOwner owner : owners) {
                Log.d("ADMIN_OWNERS",
                        "Name: " + owner.getName()
                                + ", Email: " + owner.getEmail()
                                + ", ID: " + owner.getId());
            }

            adapter.setOwners(owners);

            Log.d("ADMIN_OWNERS",
                    "Adapter item count: " + adapter.getItemCount());

            emptyText.setVisibility(
                    owners.isEmpty() ? View.VISIBLE : View.GONE
            );
        });

        viewModel.getLoading().observe(this, loading -> {
            if (loading != null) {
                progressBar.setVisibility(
                        loading ? View.VISIBLE : View.GONE
                );
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.trim().isEmpty()) {
                errorText.setText(error);
                errorText.setVisibility(View.VISIBLE);
            } else {
                errorText.setVisibility(View.GONE);
            }
        });

        viewModel.getMessage().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void showConfirmation(
            String title,
            String message,
            Runnable onConfirm
    ) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (dialog, which) ->
                        onConfirm.run()
                )
                .show();
    }
}
