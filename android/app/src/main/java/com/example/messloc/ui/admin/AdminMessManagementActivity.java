
package com.example.messloc.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.data.model.admin.AdminMess;
import com.example.messloc.ui.admin.adapter.AdminMessAdapter;
import com.example.messloc.viewmodel.admin.AdminViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import java.util.List;

public class AdminMessManagementActivity extends AppCompatActivity {

    private AdminViewModel viewModel;
    private AdminMessAdapter adapter;

    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_mess_management);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        initializeViews();
        setupRecyclerView();

        viewModel = new ViewModelProvider(this)
                .get(AdminViewModel.class);

        observeViewModel();

        viewModel.loadPendingMesses();

        View root = findViewById(R.id.adminMessManagementRoot);

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
        progressBar = findViewById(R.id.messManagementProgress);
        emptyText = findViewById(R.id.messManagementEmpty);
        errorText = findViewById(R.id.messManagementError);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rvPendingMesses);

        adapter = new AdminMessAdapter(new AdminMessAdapter.MessActionListener() {
            @Override
            public void onApprove(AdminMess mess) {
                showConfirmation(
                        "Approve Mess",
                        "Are you sure you want to approve "
                                + mess.getName() + "?",
                        () -> viewModel.approveMess(mess.getId())
                );
            }

            @Override
            public void onReject(AdminMess mess) {
                showConfirmation(
                        "Reject Mess",
                        "Are you sure you want to reject "
                                + mess.getName() + "?",
                        () -> viewModel.rejectMess(mess.getId())
                );
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {

        viewModel.getPendingMesses().observe(this, messes -> {
            if (messes == null) {
                return;
            }

            adapter.setMesses(messes);

            boolean isEmpty = messes.isEmpty();

            emptyText.setVisibility(
                    isEmpty ? View.VISIBLE : View.GONE
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
