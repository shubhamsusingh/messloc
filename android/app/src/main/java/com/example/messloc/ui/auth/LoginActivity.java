package com.example.messloc.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.messloc.R;
import com.example.messloc.databinding.ActivityLoginBinding;
import com.example.messloc.ui.home.HomeActivity;
import com.example.messloc.ui.main.MainActivity;
import com.example.messloc.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(
                this,
                R.layout.activity_login
        );


        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        binding.setAuthViewModel(authViewModel);
        binding.setLifecycleOwner(this);

        observeViewModel();
        binding.registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    SignupActivity.class
            );

            startActivity(intent);
        });
    }

    private void observeViewModel() {

        authViewModel.authResult.observe(this, response -> {

            if (response != null && response.isSuccess()) {

                Toast.makeText(
                        this,
                        response.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();

                Intent intent = new Intent(
                        this,
                        MainActivity.class
                );

                startActivity(intent);
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