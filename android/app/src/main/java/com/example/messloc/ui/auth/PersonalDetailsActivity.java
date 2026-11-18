
package com.example.messloc.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.messloc.R;
import com.example.messloc.data.model.RegisterRequest;
import com.example.messloc.databinding.ActivityPersonalDetailsBinding;
import com.example.messloc.ui.main.MainActivity;
import com.example.messloc.viewmodel.AuthViewModel;
import com.google.android.material.textfield.TextInputLayout;

public class PersonalDetailsActivity extends AppCompatActivity {

    private ActivityPersonalDetailsBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPersonalDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        binding.completeRegistrationButton.setOnClickListener(v ->
                validateAndRegister()
        );
    }

    private void validateAndRegister() {

        clearErrors();

        String name = getText(binding.nameEditText);
        String email = getText(binding.emailEditText);
        String phone = getText(binding.phoneEditText);

        // Do not trim passwords.
        String password = binding.passwordEditText.getText().toString();
        String confirmPassword =
                binding.confirmPasswordEditText.getText().toString();

        // Optional location fields
        String city = getOptionalText(binding.cityEditText);
        String state = getOptionalText(binding.stateEditText);
        String address = getOptionalText(binding.addressEditText);

        if (name.length() < 2 || name.length() > 50) {
            binding.nameInputLayout.setError(
                    "Name must be between 2 and 50 characters."
            );
            binding.nameEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError("Enter a valid email address.");
            binding.emailEditText.requestFocus();
            return;
        }

        if (!phone.matches("\\d{10}")) {
            binding.phoneInputLayout.setError(
                    "Enter a valid 10-digit phone number."
            );
            binding.phoneEditText.requestFocus();
            return;
        }

        if (password.length() < 8) {
            binding.passwordInputLayout.setError(
                    "Password must contain at least 8 characters."
            );
            binding.passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordInputLayout.setError(
                    "Passwords do not match."
            );
            binding.confirmPasswordEditText.requestFocus();
            return;
        }

        RegisterRequest request = new RegisterRequest(
                name,
                email,
                phone,
                password,
                city,
                state,
                address,
                null,
                null
        );

        authViewModel.register(request);
    }

    private void observeViewModel() {

        authViewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);

            binding.completeRegistrationButton.setEnabled(!loading);
            binding.completeRegistrationButton.setText(
                    loading ? "Creating Account..." : "Complete Registration"
            );
        });

        authViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getAuthResult().observe(this, response -> {
            if (response != null && response.isSuccess()) {
                Toast.makeText(
                        this,
                        response.getMessage() != null
                                ? response.getMessage()
                                : "Registration successful!",
                        Toast.LENGTH_SHORT
                ).show();

                Intent intent = new Intent(
                        PersonalDetailsActivity.this,
                        MainActivity.class
                );

                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                startActivity(intent);
                finish();
            }
        });
    }

    private String getText(
            com.google.android.material.textfield.TextInputEditText editText
    ) {
        return editText.getText() == null
                ? ""
                : editText.getText().toString().trim();
    }

    private String getOptionalText(
            com.google.android.material.textfield.TextInputEditText editText
    ) {
        String value = getText(editText);
        return value.isEmpty() ? null : value;
    }

    private void clearErrors() {
        binding.nameInputLayout.setError(null);
        binding.emailInputLayout.setError(null);
        binding.phoneInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);
        binding.confirmPasswordInputLayout.setError(null);
    }
}
