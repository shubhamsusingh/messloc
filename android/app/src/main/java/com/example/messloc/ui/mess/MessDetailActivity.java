package com.example.messloc.ui.mess;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.messloc.data.model.subscription.Subscription;
import com.example.messloc.viewmodel.SubscriptionViewModel;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messloc.R;
import com.example.messloc.data.model.Mess;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import android.content.Intent;
import com.example.messloc.ui.payment.PaymentCheckoutActivity;
import java.util.List;
import java.util.Locale;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.ui.mess.adapter.DailyMealAdapter;
import com.example.messloc.data.model.menu.DailyMenu;
import com.example.messloc.data.model.menu.MenuItem;
import com.example.messloc.data.model.menu.WeeklyMenuResponse;
import com.example.messloc.viewmodel.MenuViewModel;
import com.google.android.material.tabs.TabLayout;

import android.widget.ProgressBar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
public class MessDetailActivity extends AppCompatActivity {

    private static final int COLOR_VEG = Color.parseColor("#2E7D32");
    private static final int COLOR_NON_VEG = Color.parseColor("#C62828");
    // Public weekly menu
    private MenuViewModel menuViewModel;

    private TextView tvPublicMenuWeekRange;
    private TextView tvPublicMenuEmpty;
    private TabLayout tabPublicMenuDays;
    private RecyclerView rvPublicDailyMenu;
    private ProgressBar progressPublicMenu;

    private DailyMealAdapter publicMealAdapter;

    private final List<DailyMenu> publicWeeklyMenus = new ArrayList<>();

    private LocalDate publicWeekStart;
    private LocalDate publicSelectedDate;
    private SubscriptionViewModel subscriptionViewModel;
    private String messId;
    private MaterialToolbar toolbar;
    private TextView tvMessName, tvFoodType, tvRating, tvMessPrice, tvBottomPrice;
    private TextView tvMessAddress, tvMessTimings, tvMessDescription;
    private ImageView ivVegMark, ivMessImage;
    private LinearLayout layoutFacilities;
    private View cardFacilities;
    private MaterialButton btnSubscribe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess_detail);

        initViews();
        initPublicMenuViews();
        toolbar.setNavigationOnClickListener(v -> finish());

        // Receive Mess object or Mess ID
        Mess mess;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            mess = getIntent().getSerializableExtra("mess_data", Mess.class);
        } else {
            mess = (Mess) getIntent().getSerializableExtra("mess_data");
        }

        if (mess != null) {
            bindMessDetails(mess);

            messId = mess.getId();
            if (messId != null && !messId.trim().isEmpty()) {
                setupSubscriptionViewModel();
            }
            if (messId != null && !messId.trim().isEmpty()) {
                setupPublicMenuViewModel();

                publicWeekStart = LocalDate.now()
                        .with(DayOfWeek.MONDAY);

                publicSelectedDate = publicWeekStart;

                menuViewModel.loadPublicWeeklyMenu(
                        messId,
                        publicWeekStart.toString()
                );
            }
        }

        btnSubscribe.setOnClickListener(v -> {
            if (messId == null || messId.trim().isEmpty()) {
                Toast.makeText(
                        this,
                        "Invalid mess. Please try again.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            subscriptionViewModel.createSubscription(messId, "monthly");
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mess_detail), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void setupSubscriptionViewModel() {

        subscriptionViewModel = new ViewModelProvider(this)
                .get(SubscriptionViewModel.class);

        // Loading state
        subscriptionViewModel.getLoading().observe(this, loading -> {

            boolean isLoading = Boolean.TRUE.equals(loading);

            btnSubscribe.setEnabled(!isLoading);

            btnSubscribe.setText(
                    isLoading ? "Subscribing..." : "Subscribe Now"
            );
        });

        // Error response
        subscriptionViewModel.getError().observe(this, error -> {

            if (error != null && !error.trim().isEmpty()) {

                Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        // Successful subscription
        subscriptionViewModel.getSelectedSubscription().observe(this, subscription -> {

            if (subscription == null) return;

            btnSubscribe.setText("Subscribed");
            btnSubscribe.setEnabled(false);

            Toast.makeText(
                    this,
                    "Successfully subscribed to " + subscription.getMessName(),
                    Toast.LENGTH_LONG
            ).show();
        });
    }
    private void setupPublicMenuViewModel() {
        menuViewModel = new ViewModelProvider(this)
                .get(MenuViewModel.class);

        menuViewModel.getLoading().observe(this, loading -> {
            progressPublicMenu.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE
            );
        });

        menuViewModel.getWeeklyMenu().observe(this, response -> {
            if (response == null) return;

            publicWeeklyMenus.clear();

            if (response.getMenus() != null) {
                publicWeeklyMenus.addAll(response.getMenus());
            }

            tvPublicMenuWeekRange.setText(
                    response.getStartDate() + "  —  " + response.getEndDate()
            );

            setupPublicMenuTabs();
        });
        menuViewModel.getError().observe(this, error -> {
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void initViews() {
        toolbar = findViewById(R.id.toolbarMessDetail);
        tvMessName = findViewById(R.id.tvMessName);
        tvFoodType = findViewById(R.id.tvFoodType);
        tvRating = findViewById(R.id.tvRating);
        tvMessPrice = findViewById(R.id.tvMessPrice);
        tvBottomPrice = findViewById(R.id.tvBottomPrice);
        tvMessAddress = findViewById(R.id.tvMessAddress);
        tvMessTimings = findViewById(R.id.tvMessTimings);
        tvMessDescription = findViewById(R.id.tvMessDescription);
        ivVegMark = findViewById(R.id.ivVegMark);
        ivMessImage = findViewById(R.id.ivMessImage);
        layoutFacilities = findViewById(R.id.layoutFacilities);
        cardFacilities = findViewById(R.id.cardFacilities);
        btnSubscribe = findViewById(R.id.btnSubscribe);
    }
    private void setupPublicMenuTabs() {
        tabPublicMenuDays.removeAllTabs();

        DateTimeFormatter dayFormatter =
                DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

        for (int i = 0; i < 7; i++) {
            LocalDate date = publicWeekStart.plusDays(i);

            String tabTitle = date.format(dayFormatter)
                    + "\n" + date.getDayOfMonth();

            TabLayout.Tab tab = tabPublicMenuDays.newTab()
                    .setText(tabTitle);

            tabPublicMenuDays.addTab(tab, false);
        }

        tabPublicMenuDays.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        publicSelectedDate =
                                publicWeekStart.plusDays(tab.getPosition());

                        displayPublicSelectedDayMenu();
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        publicSelectedDate =
                                publicWeekStart.plusDays(tab.getPosition());

                        displayPublicSelectedDayMenu();
                    }
                }
        );

        tabPublicMenuDays.selectTab(tabPublicMenuDays.getTabAt(0));
    }
    private void displayPublicSelectedDayMenu() {
        DailyMenu selectedMenu = null;

        for (DailyMenu menu : publicWeeklyMenus) {
            if (menu.getMenuDate() != null
                    && menu.getMenuDate().equals(publicSelectedDate.toString())) {
                selectedMenu = menu;
                break;
            }
        }

        List<DailyMealAdapter.MealSection> sections = new ArrayList<>();

        if (selectedMenu != null) {
            addPublicMealSection(
                    sections, "Breakfast", "🌅",
                    selectedMenu.getBreakfast()
            );

            addPublicMealSection(
                    sections, "Lunch", "🍛",
                    selectedMenu.getLunch()
            );

            addPublicMealSection(
                    sections, "Dinner", "🌙",
                    selectedMenu.getDinner()
            );
        }

        publicMealAdapter.setMeals(sections);

        boolean hasMenu = !sections.isEmpty();

        rvPublicDailyMenu.setVisibility(
                hasMenu ? View.VISIBLE : View.GONE
        );

        tvPublicMenuEmpty.setVisibility(
                hasMenu ? View.GONE : View.VISIBLE
        );
    }
    private void addPublicMealSection(
            List<DailyMealAdapter.MealSection> sections,
            String name,
            String icon,
            List<MenuItem> items
    ) {
        if (items != null && !items.isEmpty()) {
            sections.add(
                    new DailyMealAdapter.MealSection(name, icon, items)
            );
        }
    }
    private void bindMessDetails(Mess mess) {
        toolbar.setTitle(mess.getName());
        tvMessName.setText(mess.getName());

        // Food Type Badge
        boolean isVeg = "VEG".equalsIgnoreCase(mess.getFoodType());
        int typeColor = isVeg ? COLOR_VEG : COLOR_NON_VEG;
        tvFoodType.setText(isVeg ? "Veg" : "Non-Veg");
        tvFoodType.setTextColor(typeColor);
        ivVegMark.setColorFilter(typeColor);

        // Rating Badge
        tvRating.setText(
                String.format(Locale.getDefault(), "%.1f (%d+)", mess.getRating(), mess.getReviewCount())
        );

        // Price
        String priceFormatted = String.format(Locale.getDefault(), "₹%,.0f / month", mess.getMonthlyPrice());
        String bottomPriceFormatted = String.format(Locale.getDefault(), "₹%,.0f/mo", mess.getMonthlyPrice());
        tvMessPrice.setText(priceFormatted);
        tvBottomPrice.setText(bottomPriceFormatted);

        // Address
        String fullAddress = mess.getAddress();
        if (mess.getCity() != null && !mess.getCity().isEmpty()) {
            fullAddress += ", " + mess.getCity();
        }
        tvMessAddress.setText(fullAddress);

        // Timings
        tvMessTimings.setText(
                formatTime(mess.getOpeningTime()) + " – " + formatTime(mess.getClosingTime())
        );

        // Description
        String description = mess.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            tvMessDescription.setText(description);
        } else {
            tvMessDescription.setText("No description provided for this mess.");
        }

        // Facilities Chips
        layoutFacilities.removeAllViews();
        List<String> facilities = mess.getFacilities();
        if (facilities != null && !facilities.isEmpty()) {
            cardFacilities.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (String facility : facilities) {
                TextView chip = (TextView) inflater.inflate(R.layout.item_facility_chip, layoutFacilities, false);
                chip.setText(facility);
                layoutFacilities.addView(chip);
            }
        } else {
            cardFacilities.setVisibility(View.GONE);
        }
    }
    private void initPublicMenuViews() {
        tvPublicMenuWeekRange = findViewById(R.id.tvPublicMenuWeekRange);
        tvPublicMenuEmpty = findViewById(R.id.tvPublicMenuEmpty);
        tabPublicMenuDays = findViewById(R.id.tabPublicMenuDays);
        rvPublicDailyMenu = findViewById(R.id.rvPublicDailyMenu);
        progressPublicMenu = findViewById(R.id.progressPublicMenu);

        publicMealAdapter = new DailyMealAdapter();

        rvPublicDailyMenu.setLayoutManager(new LinearLayoutManager(this));
        rvPublicDailyMenu.setAdapter(publicMealAdapter);
        rvPublicDailyMenu.setNestedScrollingEnabled(false);
    }
    private static String formatTime(@Nullable String time24) {
        if (time24 == null || !time24.contains(":")) {
            return time24 == null ? "--" : time24;
        }
        try {
            String[] parts = time24.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            String amPm = hour >= 12 ? "PM" : "AM";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm);
        } catch (NumberFormatException e) {
            return time24;
        }
    }
}