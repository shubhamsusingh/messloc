package com.example.messloc.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messloc.R;
import com.example.messloc.databinding.ActivityAccountTypeBinding;
import com.google.android.material.card.MaterialCardView;

public class AccountTypeActivity extends AppCompatActivity {

    private ActivityAccountTypeBinding binding;

    private String name;
    private String email;
    private String phone;
    private String password;

    private String selectedAccountType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAccountTypeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.personalCard.setCheckable(true);
        binding.businessCard.setCheckable(true);
        receiveSignupDetails();
        setupListeners();
    }

    private void receiveSignupDetails() {

        name = getIntent().getStringExtra("name");
        email = getIntent().getStringExtra("email");
        phone = getIntent().getStringExtra("phone");
        password = getIntent().getStringExtra("password");

        if (name == null || email == null ||
                phone == null || password == null) {
            finish();
        }
    }

    private void setupListeners() {

        binding.personalCard.setOnClickListener(v ->
                selectAccountType("PERSONAL")
        );

        binding.businessCard.setOnClickListener(v ->
                selectAccountType("BUSINESS")
        );

        binding.continueButton.setOnClickListener(v -> {

            if (selectedAccountType == null) {
                android.widget.Toast.makeText(
                        this,
                        "Please select an account type",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }

            openNextScreen();
        });
    }

    private void selectAccountType(String accountType) {

        selectedAccountType = accountType;

        boolean isPersonal = accountType.equals("PERSONAL");

        updateCardSelection(binding.personalCard, isPersonal);
        updateCardSelection(binding.businessCard, !isPersonal);
    }

    private void updateCardSelection(
            MaterialCardView card,
            boolean isSelected
    ) {
        int primaryColor = getColor(R.color.primary);
        int normalStrokeColor = getColor(R.color.card_stroke);
        int normalBackgroundColor = getColor(R.color.on_primary);

        card.setStrokeColor(
                isSelected ? primaryColor : normalStrokeColor
        );

        card.setStrokeWidth(
                dpToPx(isSelected ? 2 : 1)
        );

        card.setCardBackgroundColor(
                isSelected
                        ? getColor(R.color.card_selected_background)
                        : normalBackgroundColor
        );

        card.setCardElevation(
                dpToPx(isSelected ? 4 : 0)
        );

        card.setChecked(isSelected);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void openNextScreen() {

        Intent intent;

        if (selectedAccountType.equals("PERSONAL")) {

            intent = new Intent(
                    this,
                    PersonalDetailsActivity.class
            );

        } else {

            intent = new Intent(
                    this,
                    OwnerSignupActivity.class
            );
        }

        intent.putExtra("name", name);
        intent.putExtra("email", email);
        intent.putExtra("phone", phone);
        intent.putExtra("password", password);

        startActivity(intent);
    }
}