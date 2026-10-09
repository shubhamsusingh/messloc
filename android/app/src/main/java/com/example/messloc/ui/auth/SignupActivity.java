package com.example.messloc.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.example.messloc.R;
import com.example.messloc.databinding.ActivitySignupBinding;
import com.example.messloc.viewmodel.AuthViewModel;

public class SignupActivity extends AppCompatActivity {
    private AuthViewModel viewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_signup);

        ActivitySignupBinding binding =
                ActivitySignupBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        viewModel = new AuthViewModel();
        binding.setSignupModel(viewModel);
        binding.setLifecycleOwner(this);

        binding.loginTextView.setOnClickListener(v -> {

            Intent intent =
                    new Intent(SignupActivity.this, LoginActivity.class);

            startActivity(intent);

        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}