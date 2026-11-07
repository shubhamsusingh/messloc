package com.example.messloc.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.messloc.R;
import com.example.messloc.databinding.ActivitySignupBinding;
import com.example.messloc.ui.auth.LoginActivity;
import com.example.messloc.viewmodel.AuthViewModel;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(
                this,
                R.layout.activity_signup
        );

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        binding.setAuthViewModel(authViewModel);
        binding.setLifecycleOwner(this);
        binding.emailEditText.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s, int start, int before, int count) {

                        binding.emailInputLayout.setError(null);
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {}
                }
        );
        observeViewModel();
    }

    private void observeViewModel() {

        authViewModel.authResult.observe(this, response -> {

            if (response != null && response.isSuccess()) {

                Toast.makeText(
                        this,
                        response.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();

                startActivity(
                        new Intent(this, LoginActivity.class)
                );

                finish();
            }
        });

        authViewModel.errorMessage.observe(this, error -> {

            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}