
package com.example.messloc.ui.owner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.messloc.R;
import com.example.messloc.data.model.menu.DailyMenu;
import com.example.messloc.data.model.menu.MenuItem;
import com.example.messloc.data.model.menu.WeeklyMenuResponse;
import com.example.messloc.viewmodel.MenuViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.ui.mess.adapter.DailyMealAdapter;
import com.example.messloc.ui.mess.adapter.DailyMealAdapter.MealSection;
import java.util.ArrayList;
import java.util.Locale;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MenuManagementActivity extends AppCompatActivity {

    private MenuViewModel viewModel;
    private DailyMealAdapter mealAdapter;
    private String messId;
    private LocalDate weekStart;
    private TabLayout tabMenuDays;
    private androidx.recyclerview.widget.RecyclerView rvDailyMenu;

    private View layoutMenuContent;

    private List<DailyMenu> weeklyMenus = new ArrayList<>();
    private LocalDate selectedDate;
    private TextView tvWeekRange;
    private ProgressBar progressMenu;

    private MaterialButton btnPreviousWeek;
    private MaterialButton btnNextWeek;

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_management);

        messId = getIntent().getStringExtra("mess_id");


        findViewById(R.id.btnCreateMenu).setOnClickListener(v -> {
            Intent intent = new Intent(
                    MenuManagementActivity.this,
                    CreateMenuActivity.class
            );

            intent.putExtra("mess_id", messId);
            startActivity(intent);
        });


        if (messId == null || messId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid mess ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        weekStart = LocalDate.now().with(
                java.time.DayOfWeek.MONDAY
        );

        viewModel = new ViewModelProvider(this)
                .get(MenuViewModel.class);

        setupObservers();
        setupListeners();

        loadWeeklyMenu();
        View root = findViewById(R.id.menuRoot);

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

    @Override
    protected void onResume() {
        super.onResume();

        if (viewModel != null && messId != null) {
            viewModel.loadOwnerWeeklyMenu(
                    messId,
                    weekStart.toString()
            );
        }
    }
    private void initializeViews() {
        tvWeekRange = findViewById(R.id.tvWeekRange);
        progressMenu = findViewById(R.id.progressMenu);
        layoutMenuContent = findViewById(R.id.layoutMenuContent);

        btnPreviousWeek = findViewById(R.id.btnPreviousWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);

        tabMenuDays = findViewById(R.id.tabMenuDays);
        rvDailyMenu = findViewById(R.id.rvDailyMenu);
        rvDailyMenu = findViewById(R.id.rvDailyMenu);

        mealAdapter = new DailyMealAdapter();

        rvDailyMenu.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvDailyMenu.setAdapter(mealAdapter);
    }

    private void displaySelectedDayMenu() {

        if (selectedDate == null) {
            return;
        }

        DailyMenu selectedMenu = null;

        for (DailyMenu menu : weeklyMenus) {

            if (menu.getMenuDate() != null
                    && menu.getMenuDate().equals(selectedDate.toString())) {

                selectedMenu = menu;
                break;
            }
        }

        List<MealSection> sections = new ArrayList<>();

        if (selectedMenu != null) {

            sections.add(new MealSection(
                    "Breakfast",
                    "🌅",
                    selectedMenu.getBreakfast()
            ));

            sections.add(new MealSection(
                    "Lunch",
                    "🍛",
                    selectedMenu.getLunch()
            ));

            sections.add(new MealSection(
                    "Dinner",
                    "🌙",
                    selectedMenu.getDinner()
            ));

        } else {

            sections.add(new MealSection(
                    "Breakfast", "🌅", null
            ));

            sections.add(new MealSection(
                    "Lunch", "🍛", null
            ));

            sections.add(new MealSection(
                    "Dinner", "🌙", null
            ));
        }

        mealAdapter.setMeals(sections);
    }

    private void setupDayTabs() {
        tabMenuDays.removeAllTabs();

        DateTimeFormatter dayFormatter =
                DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);

            String dayName = date.format(dayFormatter);
            String dayNumber = String.valueOf(date.getDayOfMonth());

            TabLayout.Tab tab = tabMenuDays.newTab();
            tab.setText(dayName + "\n" + dayNumber);

            tabMenuDays.addTab(tab);
        }

        tabMenuDays.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        selectedDate = weekStart.plusDays(tab.getPosition());
                        displaySelectedDayMenu();
                        // RecyclerView display will be connected in the next step.
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                }
        );

        selectedDate = weekStart;
        tabMenuDays.selectTab(tabMenuDays.getTabAt(0));
    }
    private void setupListeners() {

        btnPreviousWeek.setOnClickListener(v -> {
            weekStart = weekStart.minusWeeks(1);
            loadWeeklyMenu();
        });

        btnNextWeek.setOnClickListener(v -> {
            weekStart = weekStart.plusWeeks(1);
            loadWeeklyMenu();
        });
    }

    private void setupObservers() {

        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);

            progressMenu.setVisibility(
                    isLoading ? View.VISIBLE : View.GONE
            );

            if (isLoading) {
                layoutMenuContent.setVisibility(View.GONE);
            }
        });

        viewModel.getWeeklyMenu().observe(this, response -> {
            if (response != null) {
                displayWeeklyMenu(response);
                layoutMenuContent.setVisibility(View.VISIBLE);
            }
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
    }

    private void loadWeeklyMenu() {
        tvWeekRange.setText(
                weekStart.format(dateFormatter)
                        + " - "
                        + weekStart.plusDays(6).format(dateFormatter)
        );

        setupDayTabs();

        viewModel.loadOwnerWeeklyMenu(
                messId,
                weekStart.toString()
        );
    }
    private void displayWeeklyMenu(WeeklyMenuResponse response) {

        weeklyMenus.clear();

        if (response.getMenus() != null) {
            weeklyMenus.addAll(response.getMenus());
        }

        displaySelectedDayMenu();
    }

    private void appendMeal(
            StringBuilder content,
            String mealName,
            List<MenuItem> items
    ) {

        content.append(mealName).append("\n");

        if (items == null || items.isEmpty()) {
            content.append("  No items added\n\n");
            return;
        }

        for (MenuItem item : items) {

            content.append("  • ")
                    .append(item.getFoodName());

            if (item.getDescription() != null
                    && !item.getDescription().trim().isEmpty()) {

                content.append(" - ")
                        .append(item.getDescription());
            }

            content.append("\n");
        }

        content.append("\n");
    }
}
