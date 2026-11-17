package com.example.messloc.ui.owner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.messloc.databinding.ActivityEditMessBinding;
import com.example.messloc.viewmodel.EditMessViewModel;

public class EditMessActivity extends AppCompatActivity {

    private ActivityEditMessBinding binding;
    private EditMessViewModel viewModel;

    private String messId;

    private final ActivityResultLauncher<Intent> locationPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            Intent data = result.getData();

                            if (data.hasExtra("latitude")) {
                                viewModel.latitude.setValue(
                                        String.valueOf(data.getDoubleExtra("latitude", 0.0))
                                );
                            }

                            if (data.hasExtra("longitude")) {
                                viewModel.longitude.setValue(
                                        String.valueOf(data.getDoubleExtra("longitude", 0.0))
                                );
                            }

                            if (data.hasExtra("address")) {
                                viewModel.address.setValue(
                                        data.getStringExtra("address")
                                );
                            }

                            if (data.hasExtra("city")) {
                                viewModel.city.setValue(
                                        data.getStringExtra("city")
                                );
                            }

                            if (data.hasExtra("state")) {
                                viewModel.state.setValue(
                                        data.getStringExtra("state")
                                );
                            }

                            if (data.hasExtra("pincode")) {
                                viewModel.pincode.setValue(
                                        data.getStringExtra("pincode")
                                );
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEditMessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this)
                .get(EditMessViewModel.class);

        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        // Get mess ID from previous screen
        messId = getIntent().getStringExtra("mess_id");

        if (messId == null || messId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Mess ID not found.",
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            return;
        }

        // Toolbar back button
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Open location picker
        binding.btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(
                    EditMessActivity.this,
                    PickLocationActivity.class
            );
            locationPickerLauncher.launch(intent);
        });

        observeViewModel();

        // Load existing mess details
        viewModel.loadMess(messId);
    }

    private void observeViewModel() {

        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        viewModel.updateResult.observe(this, response -> {
            if (response != null) {
                Toast.makeText(
                        this,
                        "Mess updated successfully.",
                        Toast.LENGTH_SHORT
                ).show();

                setResult(RESULT_OK);
                finish();
            }
        });
    }
}
