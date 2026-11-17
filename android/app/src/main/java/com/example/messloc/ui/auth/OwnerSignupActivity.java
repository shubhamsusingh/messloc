package com.example.messloc.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.databinding.DataBindingUtil;

import com.example.messloc.R;
import com.example.messloc.databinding.ActivityOwnerSignupBinding;
import com.example.messloc.viewmodel.OwnerSignupViewModel;

public class OwnerSignupActivity extends AppCompatActivity {

    private ActivityOwnerSignupBinding binding;
    private OwnerSignupViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(
                this,
                R.layout.activity_owner_signup
        );

        viewModel = new ViewModelProvider(this)
                .get(OwnerSignupViewModel.class);

        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        setupToolbar();
        observeViewModel();
        setupClickListeners();
        View root = findViewById(R.id.ownerSignUpRoot);

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

    private void setupToolbar() {

        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void observeViewModel() {

        // Loading state
        viewModel.loading.observe(this, isLoading -> {

            boolean loading = Boolean.TRUE.equals(isLoading);

            binding.loadingContainer.setVisibility(
                    loading ? View.VISIBLE : View.GONE
            );

            binding.btnRegister.setEnabled(!loading);
        });

        // Error messages
        viewModel.errorMessage.observe(this, message -> {

            if (message != null && !message.trim().isEmpty()) {

                binding.tvError.setText(message);
                binding.tvError.setVisibility(View.VISIBLE);

            } else {

                binding.tvError.setVisibility(View.GONE);
            }
        });

        // Successful registration
        viewModel.signupResult.observe(this, response -> {

            if (response == null) {
                return;
            }

            binding.formContainer.setVisibility(View.GONE);
            binding.successContainer.setVisibility(View.VISIBLE);

            String message = response.getMessage();

            if (message != null && !message.trim().isEmpty()) {
                binding.tvSuccessMessage.setText(message);
            }
        });
    }

    private void setupClickListeners() {

        binding.btnBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this,LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}