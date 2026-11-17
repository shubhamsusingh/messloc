package com.example.messloc.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.messloc.ui.admin.AdminActivity;

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

        authViewModel.loadingMessage.observe(this, message -> {
            Log.d("LOGIN_DEBUG", "Loading message: " + message);
        });

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

                if (response.getUser() == null ||
                        response.getUser().getRole() == null) {

                    Toast.makeText(
                            this,
                            "Unable to identify account role.",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                String role = response.getUser()
                        .getRole()
                        .trim()
                        .toUpperCase();

                Toast.makeText(
                        this,
                        response.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();

                Intent intent;

                switch (role) {

                    case "USER":
                    case "OWNER":
                        intent = new Intent(
                                this,
                                MainActivity.class
                        );
                        break;

                    case "ADMIN":
                        intent = new Intent(
                                this,
                                AdminActivity.class
                        );
                        break;

                    default:
                        Toast.makeText(
                                this,
                                "Unsupported account role: " + role,
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                }

                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                startActivity(intent);
                finish();

            }
        });

        authViewModel.loadingMessage.observe(this, message -> {
            binding.loadingOverlay.loadingMessage.setText(message);
        });

        authViewModel.errorMessage.observe(this, error -> {

            if (error != null) {
                Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}