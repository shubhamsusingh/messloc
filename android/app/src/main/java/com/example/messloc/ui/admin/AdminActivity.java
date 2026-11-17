
package com.example.messloc.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
import com.example.messloc.ui.auth.LoginActivity;
import com.example.messloc.utils.TokenManager;
import com.example.messloc.R;
import com.example.messloc.data.model.admin.AdminHealth;
import com.example.messloc.data.model.admin.AdminMess;
import com.example.messloc.data.model.admin.AdminOwner;
import com.example.messloc.viewmodel.admin.AdminViewModel;

import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private AdminViewModel adminViewModel;

    private TextView totalMessesCount;
    private TextView pendingMessesCount;
    private TextView pendingOwnersCount;
    private TextView adminHealthText;
    private TextView adminErrorText;
    private TextView adminRetry;

    private ProgressBar adminProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        initializeViews();

        adminViewModel = new ViewModelProvider(this)
                .get(AdminViewModel.class);

        observeViewModel();

        loadDashboard();

        adminRetry.setOnClickListener(v -> {
            adminViewModel.clearMessages();
            loadDashboard();
        });
        findViewById(R.id.manageMesses).setOnClickListener(v -> {
            startActivity(new Intent(
                    AdminActivity.this,
                    AdminMessManagementActivity.class
            ));
        });
        findViewById(R.id.manageOwners).setOnClickListener(v -> {
            startActivity(new Intent(
                    AdminActivity.this,
                    AdminOwnerManagementActivity.class
            ));
        });
        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Logout", (dialog, which) -> {
                        TokenManager tokenManager = new TokenManager(this);
                        tokenManager.clearSession();

                        Intent intent = new Intent(
                                AdminActivity.this,
                                LoginActivity.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    })
                    .show();
        });
        View root = findViewById(R.id.adminManagementRoot);

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

        totalMessesCount = findViewById(R.id.totalMessesCount);
        pendingMessesCount = findViewById(R.id.pendingMessesCount);
        pendingOwnersCount = findViewById(R.id.pendingOwnersCount);

        adminHealthText = findViewById(R.id.adminHealthText);
        adminErrorText = findViewById(R.id.adminErrorText);
        adminRetry = findViewById(R.id.adminRetry);

        adminProgress = findViewById(R.id.adminProgress);
    }

    private void loadDashboard() {
        adminViewModel.checkAdminAccess();
        adminViewModel.loadAllMesses();
        adminViewModel.loadPendingMesses();
        adminViewModel.loadPendingOwners();
    }

    private void observeViewModel() {

        adminViewModel.getAllMesses().observe(this, messes -> {
            if (messes != null) {
                totalMessesCount.setText(String.valueOf(messes.size()));
            }
        });

        adminViewModel.getPendingMesses().observe(this, messes -> {
            if (messes != null) {
                pendingMessesCount.setText(String.valueOf(messes.size()));
            }
        });

        adminViewModel.getPendingOwners().observe(this, owners -> {
            if (owners != null) {
                pendingOwnersCount.setText(String.valueOf(owners.size()));
            }
        });

        adminViewModel.getAdminHealth().observe(this, health -> {
            if (health != null) {
                showAdminHealth(health);
            }
        });

        adminViewModel.getLoading().observe(this, loading -> {
            if (loading != null) {
                adminProgress.setVisibility(
                        loading ? View.VISIBLE : View.GONE
                );
            }
        });

        adminViewModel.getError().observe(this, error -> {
            if (error != null && !error.trim().isEmpty()) {
                adminErrorText.setText(error);
                adminErrorText.setVisibility(View.VISIBLE);
                adminRetry.setVisibility(View.VISIBLE);
            } else {
                adminErrorText.setVisibility(View.GONE);
                adminRetry.setVisibility(View.GONE);
            }
        });

        adminViewModel.getMessage().observe(this, message -> {
            if (message != null) {
                adminErrorText.setVisibility(View.GONE);
                adminRetry.setVisibility(View.GONE);
            }
        });
    }

    private void showAdminHealth(AdminHealth health) {

        String username = health.getUsername();

        if (username == null || username.trim().isEmpty()) {
            username = "Admin";
        }

        adminHealthText.setText(
                "Admin access verified\nLogged in as: " + username
        );
    }
}
