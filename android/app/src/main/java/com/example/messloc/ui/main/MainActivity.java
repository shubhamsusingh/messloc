
package com.example.messloc.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import android.location.Geocoder;
import android.os.Build;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.messloc.R;
import com.example.messloc.ui.home.HomeFragment;
import com.example.messloc.ui.profile.ProfileFragment;
import com.example.messloc.ui.subscriptions.SubscriptionsFragment;
import com.example.messloc.utils.UserLocationStorage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService geocoderExecutor =
            Executors.newSingleThreadExecutor();
    // Permission launcher
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {

                        boolean fineGranted = Boolean.TRUE.equals(
                                permissions.get(Manifest.permission.ACCESS_FINE_LOCATION)
                        );

                        boolean coarseGranted = Boolean.TRUE.equals(
                                permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION)
                        );

                        if (fineGranted || coarseGranted) {
                            getUserLocation();
                        } else {
                            Toast.makeText(
                                    this,
                                    "Location permission denied. Nearby messes may not be available.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_home);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (getIntent().getBooleanExtra("open_subscriptions", false)) {
            openSubscriptionsFragment();
            getIntent().removeExtra("open_subscriptions");
        }
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.rootLayout),
                (view, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            view.getPaddingLeft(),
                            systemBars.top,
                            view.getPaddingRight(),
                            view.getPaddingBottom()
                    );

                    return insets;
                }
        );

        bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setOnItemSelectedListener(item -> {

            Fragment selectedFragment;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId==R.id.nav_subscriptions) {
               selectedFragment = new SubscriptionsFragment();
            }else  {
                return false;
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mainFragmentContainer, selectedFragment)
                    .commit();

            return true;
        });

        if (savedInstanceState == null) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mainFragmentContainer, new HomeFragment())
                    .commit();

            bottomNavigation.setSelectedItemId(R.id.nav_home);

            // Ask for location permission when app opens
            checkLocationPermission();
        }
    }

    private void checkLocationPermission() {

        boolean fineGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            getUserLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent != null && intent.getBooleanExtra("open_subscriptions", false)) {
            openSubscriptionsFragment();
            intent.removeExtra("open_subscriptions");
        }
    }
    private void openSubscriptionsFragment() {
        bottomNavigation.setSelectedItemId(R.id.nav_subscriptions);
    }
    private void getUserLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CancellationTokenSource cancellationTokenSource =
                new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken()
        ).addOnSuccessListener(location -> {

            if (location != null) {

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                UserLocationStorage locationStorage =
                        new UserLocationStorage(this);

                locationStorage.saveCoordinates(latitude, longitude);
                Log.d("USER_LOCATION", "Latitude: " + latitude);
                Log.d("USER_LOCATION", "Longitude: " + longitude);
                getAddressFromLocation(latitude, longitude);
                // TODO: Send coordinates to your nearby mess API

            } else {
                Log.e("USER_LOCATION", "Location is null");
                Toast.makeText(
                        this,
                        "Unable to get location. Please try again.",
                        Toast.LENGTH_SHORT
                ).show();
            }

        }).addOnFailureListener(e -> {
            Log.e("USER_LOCATION", "Failed to get location", e);
        });
    }

    private void getAddressFromLocation(double latitude, double longitude) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            // Android 13 and above: asynchronous API
            geocoder.getFromLocation(latitude, longitude, 1, addresses -> {

                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);

                    runOnUiThread(() -> showLocationAddress(address));
                } else {
                    runOnUiThread(() ->
                            showLocationAddress("Address unavailable"));
                }
            });

        } else {

            // Android versions below 13: execute in background
            geocoderExecutor.execute(() -> {
                try {
                    List<android.location.Address> addresses =
                            geocoder.getFromLocation(latitude, longitude, 1);

                    String address = "Address unavailable";

                    if (addresses != null && !addresses.isEmpty()) {
                        address = addresses.get(0).getAddressLine(0);
                    }

                    String finalAddress = address;

                    runOnUiThread(() -> showLocationAddress(finalAddress));

                } catch (IOException e) {
                    Log.e("GEOCODER", "Unable to get address", e);

                    runOnUiThread(() ->
                            showLocationAddress("Address unavailable"));
                }
            });
        }
    }

    private void showLocationAddress(String address) {

        Log.d("USER_ADDRESS", address);

        if (address != null && !address.equals("Address unavailable")) {

            UserLocationStorage locationStorage =
                    new UserLocationStorage(this);

            locationStorage.saveAddress(address);
        }

        Bundle bundle = new Bundle();
        bundle.putString("user_location_address", address);

        getSupportFragmentManager().setFragmentResult(
                "location_address",
                bundle
        );
    }
}
