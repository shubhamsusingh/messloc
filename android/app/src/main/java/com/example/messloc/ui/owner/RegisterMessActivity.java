package com.example.messloc.ui.owner;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.messloc.R;
import com.example.messloc.databinding.ActivityRegisterMessBinding;
import com.example.messloc.utils.TokenManager;
import com.example.messloc.viewmodel.RegisterMessViewModel;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
public class RegisterMessActivity extends AppCompatActivity {

    private ActivityRegisterMessBinding binding;
    private RegisterMessViewModel viewModel;
    private ActivityResultLauncher<Intent> locationPickerLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null) {

                        Intent data = result.getData();

                        double latitude = data.getDoubleExtra("latitude", 0);
                        double longitude = data.getDoubleExtra("longitude", 0);

                        String address = data.getStringExtra("address");
                        String city = data.getStringExtra("city");
                        String state = data.getStringExtra("state");
                        String pincode = data.getStringExtra("pincode");

                        viewModel.latitude.setValue(String.valueOf(latitude));
                        viewModel.longitude.setValue(String.valueOf(longitude));

                        if (address != null && !address.trim().isEmpty()) {
                            viewModel.address.setValue(address);
                        }

                        if (city != null && !city.trim().isEmpty()) {
                            viewModel.city.setValue(city);
                        }

                        if (state != null && !state.trim().isEmpty()) {
                            viewModel.state.setValue(state);
                        }

                        if (pincode != null && !pincode.trim().isEmpty()) {
                            viewModel.pincode.setValue(pincode);
                        }

                        Toast.makeText(
                                this,
                                "Location selected successfully.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
        // Initialize Data Binding
        binding = DataBindingUtil.setContentView(
                this,
                R.layout.activity_register_mess
        );

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this)
                .get(RegisterMessViewModel.class);

        // Connect ViewModel with XML
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        // Set owner JWT
        TokenManager tokenManager = new TokenManager(this);

        // Replace with your actual TokenManager getter
        viewModel.setOwnerToken(tokenManager.getToken());

        // Back button
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(
                    RegisterMessActivity.this,
                    PickLocationActivity.class
            );

            locationPickerLauncher.launch(intent);
        });
        // Observe API results
        observeViewModel();
        View root = findViewById(R.id.rootRegisterMess);

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
    private void openLocationPicker() {
        Intent intent = new Intent(
                RegisterMessActivity.this,
                PickLocationActivity.class
        );

        locationPickerLauncher.launch(intent);
    }
    private void observeViewModel() {

        viewModel.errorMessage.observe(this, message -> {

            if (message != null && !message.isEmpty()) {

                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        viewModel.registerResult.observe(this, response -> {

            if (response != null && response.isSuccess()) {

                String messId = response.getData() != null
                        ? response.getData().getId()
                        : null;

                Toast.makeText(
                        this,
                        "Mess registered successfully! Awaiting admin approval.",
                        Toast.LENGTH_LONG
                ).show();

                finish();
            }
        });
    }

}