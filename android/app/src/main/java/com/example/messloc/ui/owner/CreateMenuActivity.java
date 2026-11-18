
package com.example.messloc.ui.owner;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.messloc.R;
import com.example.messloc.data.model.menu.CreateMenuRequest;
import com.example.messloc.data.model.menu.MenuItemRequest;
import com.example.messloc.viewmodel.MenuViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CreateMenuActivity extends AppCompatActivity {

    private MenuViewModel viewModel;

    private String messId;
    private LocalDate selectedDate;

    private MaterialButton btnSelectMenuDate;
    private MaterialButton btnSubmitMenu;

    private TextInputEditText etBreakfastName;
    private TextInputEditText etBreakfastDescription;

    private TextInputEditText etLunchName;
    private TextInputEditText etLunchDescription;

    private TextInputEditText etDinnerName;
    private TextInputEditText etDinnerDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_menu);

        messId = getIntent().getStringExtra("mess_id");

        if (messId == null || messId.trim().isEmpty()) {
            Toast.makeText(this, "Mess ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        initializeViewModel();
        setupDatePicker();
        setupToolbar();
        setupListeners();
        View root = findViewById(R.id.menuCreateRoot);

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

    private void initializeViews() {

        MaterialToolbar toolbar = findViewById(R.id.toolbarCreateMenu);

        btnSelectMenuDate = findViewById(R.id.btnSelectMenuDate);
        btnSubmitMenu = findViewById(R.id.btnSubmitMenu);

        etBreakfastName = findViewById(R.id.etBreakfastName);
        etBreakfastDescription = findViewById(R.id.etBreakfastDescription);

        etLunchName = findViewById(R.id.etLunchName);
        etLunchDescription = findViewById(R.id.etLunchDescription);

        etDinnerName = findViewById(R.id.etDinnerName);
        etDinnerDescription = findViewById(R.id.etDinnerDescription);
    }

    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(MenuViewModel.class);

        viewModel.getLoading().observe(this, isLoading -> {
            btnSubmitMenu.setEnabled(!Boolean.TRUE.equals(isLoading));
            btnSubmitMenu.setText(
                    Boolean.TRUE.equals(isLoading)
                            ? "Creating Menu..."
                            : "Create Menu"
            );
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        viewModel.getSelectedMenu().observe(this, menu -> {
            if (menu != null) {
                String message = viewModel.getMessage().getValue();

                Toast.makeText(
                        this,
                        message != null ? message : "Menu created successfully",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarCreateMenu);

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDatePicker() {

        selectedDate = LocalDate.now();
        updateSelectedDate();

        btnSelectMenuDate.setOnClickListener(v -> {

            DatePickerDialog datePicker = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {

                        selectedDate = LocalDate.of(
                                year,
                                month + 1,
                                dayOfMonth
                        );

                        updateSelectedDate();
                    },
                    selectedDate.getYear(),
                    selectedDate.getMonthValue() - 1,
                    selectedDate.getDayOfMonth()
            );

            datePicker.show();
        });
    }

    private void updateSelectedDate() {

        String formattedDate = selectedDate.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );

        findViewById(R.id.tvSelectedMenuDate);

        android.widget.TextView tvSelectedMenuDate =
                findViewById(R.id.tvSelectedMenuDate);

        tvSelectedMenuDate.setText(formattedDate);
    }

    private void setupListeners() {

        btnSubmitMenu.setOnClickListener(v -> submitMenu());
    }

    private void submitMenu() {

        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        List<MenuItemRequest> items = new ArrayList<>();

        addMenuItem(
                items,
                "BREAKFAST",
                etBreakfastName,
                etBreakfastDescription
        );

        addMenuItem(
                items,
                "LUNCH",
                etLunchName,
                etLunchDescription
        );

        addMenuItem(
                items,
                "DINNER",
                etDinnerName,
                etDinnerDescription
        );

        if (items.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter at least one food item",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String menuDate = selectedDate.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );

        CreateMenuRequest request = new CreateMenuRequest(
                messId,
                menuDate,
                items
        );

        viewModel.createMenu(request);
    }

    private void addMenuItem(
            List<MenuItemRequest> items,
            String mealType,
            TextInputEditText nameField,
            TextInputEditText descriptionField
    ) {

        String foodName = getText(nameField);
        String description = getText(descriptionField);

        if (foodName.isEmpty()) {
            return;
        }

        items.add(new MenuItemRequest(
                mealType,
                foodName,
                description
        ));
    }

    private String getText(TextInputEditText field) {

        if (field.getText() == null) {
            return "";
        }

        return field.getText().toString().trim();
    }
}
