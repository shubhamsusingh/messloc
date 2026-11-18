
package com.example.messloc.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messloc.databinding.ActivitySignupBinding;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {

        binding.personalCard.setOnClickListener(v -> {
            startActivity(new Intent(
                    SignupActivity.this,
                    PersonalDetailsActivity.class
            ));
        });

        binding.businessCard.setOnClickListener(v -> {
            startActivity(new Intent(
                    SignupActivity.this,
                    OwnerSignupActivity.class
            ));
        });

        binding.loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(
                    SignupActivity.this,
                    LoginActivity.class
            ));
            finish();
        });
    }
}
