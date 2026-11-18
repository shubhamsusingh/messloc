
package com.example.messloc.ui.owner;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.messloc.R;
import com.example.messloc.data.model.owner.OwnerMess;
import com.example.messloc.viewmodel.OwnerMessViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import android.content.Intent;
import java.util.List;

public class OwnerMessDetailsActivity extends AppCompatActivity {

    private OwnerMessViewModel viewModel;
    private String messId;
    private View layoutDetails;
    private View progressDetails;

    private TextView tvName;
    private TextView tvDescription;
    private TextView tvAddress;
    private TextView tvContact;
    private TextView tvPrice;
    private TextView tvFoodType;
    private TextView tvTimings;
    private TextView tvVerification;
    private TextView tvStatus;
    private TextView tvFacilities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_mess_details);

        messId = getIntent().getStringExtra("mess_id");

        if (messId == null || messId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid mess ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btnEditMess).setOnClickListener(v -> {
            Intent intent = new Intent(
                    OwnerMessDetailsActivity.this,
                    EditMessActivity.class
            );

            intent.putExtra("mess_id", messId);
            startActivity(intent);
        });
        findViewById(R.id.btnManageMenu).setOnClickListener(v -> {

            Intent intent = new Intent(
                    OwnerMessDetailsActivity.this,
                    MenuManagementActivity.class
            );

            intent.putExtra("mess_id", messId);
            startActivity(intent);
        });

        View root = findViewById(R.id.detailsRoot);

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

        viewModel = new ViewModelProvider(this)
                .get(OwnerMessViewModel.class);

        observeViewModel();

        viewModel.loadMyMessById(messId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (viewModel != null && messId != null) {
            viewModel.loadMyMessById(messId);
        }
    }
    private void initializeViews() {

        layoutDetails = findViewById(R.id.layoutDetails);
        progressDetails = findViewById(R.id.progressDetails);

        tvName = findViewById(R.id.tvDetailName);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvAddress = findViewById(R.id.tvDetailAddress);
        tvContact = findViewById(R.id.tvDetailContact);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvFoodType = findViewById(R.id.tvDetailFoodType);
        tvTimings = findViewById(R.id.tvDetailTimings);
        tvVerification = findViewById(R.id.tvDetailVerification);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvFacilities = findViewById(R.id.tvDetailFacilities);
    }

    private void observeViewModel() {

        viewModel.getLoading().observe(this, loading -> {

            if (Boolean.TRUE.equals(loading)) {
                progressDetails.setVisibility(View.VISIBLE);
                layoutDetails.setVisibility(View.GONE);
            } else {
                progressDetails.setVisibility(View.GONE);
            }
        });

        viewModel.getSelectedMess().observe(this, mess -> {

            if (mess != null) {
                displayMessDetails(mess);
                layoutDetails.setVisibility(View.VISIBLE);
                progressDetails.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(this, error -> {

            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                ).show();

                finish();
            }
        });
    }

    private void displayMessDetails(OwnerMess mess) {

        tvName.setText(valueOrFallback(mess.getName(), "Unnamed Mess"));

        tvDescription.setText(
                valueOrFallback(mess.getDescription(), "No description available")
        );

        String address = valueOrFallback(mess.getAddress(), "");
        String city = valueOrFallback(mess.getCity(), "");
        String state = valueOrFallback(mess.getState(), "");
        String pincode = valueOrFallback(mess.getPincode(), "");

        String fullAddress = address;

        if (!city.isEmpty()) fullAddress += "\n" + city;
        if (!state.isEmpty()) fullAddress += ", " + state;
        if (!pincode.isEmpty()) fullAddress += " - " + pincode;

        tvAddress.setText(
                fullAddress.trim().isEmpty()
                        ? "Address not available"
                        : fullAddress
        );

        tvContact.setText(
                valueOrFallback(mess.getContactNumber(), "Not available")
        );

        Double price = mess.getMonthlyPrice();

        tvPrice.setText(
                price == null
                        ? "Price not available"
                        : "₹" + price + " / month"
        );

        tvFoodType.setText(
                valueOrFallback(mess.getFoodType(), "Not specified")
        );

        String opening = valueOrFallback(mess.getOpeningTime(), "--:--");
        String closing = valueOrFallback(mess.getClosingTime(), "--:--");

        tvTimings.setText(opening + " - " + closing);

        tvVerification.setText(
                valueOrFallback(mess.getVerificationStatus(), "UNKNOWN")
        );

        tvStatus.setText(
                valueOrFallback(mess.getStatus(), "UNKNOWN")
        );

        List<String> facilities = mess.getFacilities();

        if (facilities == null || facilities.isEmpty()) {
            tvFacilities.setText("No facilities listed");
        } else {
            tvFacilities.setText(android.text.TextUtils.join(", ", facilities));
        }
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value;
    }
}
