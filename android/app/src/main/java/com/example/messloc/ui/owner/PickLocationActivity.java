
package com.example.messloc.ui.owner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.config.Configuration;
import androidx.annotation.NonNull;
import org.osmdroid.util.MapTileArea;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.events.MapEventsReceiver;
import com.example.messloc.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

public class PickLocationActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 101;

    private MapView mapView;
    private Marker marker;
    private MyLocationNewOverlay locationOverlay;

    private TextView tvSelectedAddress;
    private MaterialButton btnConfirmLocation;
    private FloatingActionButton btnCurrentLocation;

    private GeoPoint selectedPoint;

    private String selectedAddress = "";
    private String selectedCity = "";
    private String selectedState = "";
    private String selectedPincode = "";
    private int geocodingRequestId = 0;
    private boolean isGeocoding = false;

    // Public OpenStreetMap France Mirror (Standard OSM data, high-availability, zero API key required)
    private static final OnlineTileSourceBase OSM_FRANCE_MIRROR = new XYTileSource(
            "OSM_France",
            0, 19, 256, ".png",
            new String[]{
                    "https://a.tile.openstreetmap.fr/osmfr/",
                    "https://b.tile.openstreetmap.fr/osmfr/",
                    "https://c.tile.openstreetmap.fr/osmfr/"
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();

        // Valid OpenStreetMap Tile Usage Policy User-Agent & Referer format
        String userAgent = "MessLoc/1.0 (contact: sriomsingh543@gmail.com)";
        Configuration.getInstance().setUserAgentValue(userAgent);
        Configuration.getInstance().getAdditionalHttpRequestProperties().put("User-Agent", userAgent);
        Configuration.getInstance().getAdditionalHttpRequestProperties().put("Referer", "https://messloc.app/");

        // Reset tile cache directory to clear cached 403 error tile images from disk
        File tileCache = new File(ctx.getCacheDir(), "osmdroid_tiles_v3");
        if (!tileCache.exists()) {
            tileCache.mkdirs();
        }
        Configuration.getInstance().setOsmdroidTileCache(tileCache);

        SharedPreferences prefs = ctx.getSharedPreferences("osmdroid_prefs", MODE_PRIVATE);
        Configuration.getInstance().load(ctx, prefs);
        Configuration.getInstance().setUserAgentValue(userAgent);
        Configuration.getInstance().save(ctx, prefs);

        setContentView(R.layout.activity_pick_location);

        initializeViews();
        setupToolbar();
        setupMap();
        setupListeners();
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupMap() {

        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        mapView.getController().setZoom(15.0);
        mapView.setTileSource(OSM_FRANCE_MIRROR);
        // Default map location: Dehradun
        GeoPoint defaultPoint = new GeoPoint(
                30.3165,
                78.0322
        );

        mapView.getController().setCenter(defaultPoint);

        marker = new Marker(mapView);
        marker.setPosition(defaultPoint);
        marker.setTitle("Mess Location");
        marker.setDraggable(true);

        marker.setOnMarkerDragListener(
                new Marker.OnMarkerDragListener() {

                    @Override
                    public void onMarkerDrag(Marker marker) {
                    }

                    @Override
                    public void onMarkerDragEnd(Marker marker) {
                        selectedPoint = marker.getPosition();
                        updateAddress();
                    }

                    @Override
                    public void onMarkerDragStart(Marker marker) {
                    }
                }
        );

        mapView.getOverlays().add(marker);
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(
                new MapEventsReceiver() {

                    @Override
                    public boolean singleTapConfirmedHelper(GeoPoint point) {
                        moveMarker(point);
                        return true;
                    }

                    @Override
                    public boolean longPressHelper(GeoPoint point) {
                        moveMarker(point);
                        return true;
                    }
                }
        );

        mapView.getOverlays().add(mapEventsOverlay);
        selectedPoint = defaultPoint;
        updateAddress();
    }

    private void setupListeners() {

        btnCurrentLocation.setOnClickListener(v -> {
            if (hasLocationPermission()) {
                moveToCurrentLocation();
            } else {
                requestLocationPermission();
            }
        });

        btnConfirmLocation.setOnClickListener(v -> confirmLocation());
    }

    private boolean hasLocationPermission() {

        return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST
        );
    }

    private void moveToCurrentLocation() {

        if (!hasLocationPermission()) {
            return;
        }

        try {
            LocationManager locationManager =
                    (LocationManager) getSystemService(LOCATION_SERVICE);

            Location location = null;

            if (locationManager != null) {

                if (locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER)) {

                    location = locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );
                }

                if (location == null &&
                        locationManager.isProviderEnabled(
                                LocationManager.NETWORK_PROVIDER)) {

                    location = locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );
                }
            }

            if (location != null) {

                GeoPoint point = new GeoPoint(
                        location.getLatitude(),
                        location.getLongitude()
                );

                moveMarker(point);

            } else {

                Toast.makeText(
                        this,
                        "Current location unavailable. Please try again or move the marker manually.",
                        Toast.LENGTH_LONG
                ).show();
            }

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "Location permission is required.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void moveMarker(GeoPoint point) {

        selectedPoint = point;

        marker.setPosition(point);

        mapView.getController().animateTo(point);

        mapView.invalidate();

        updateAddress();
    }

    private void updateAddress() {

        if (selectedPoint == null) {
            return;
        }

        // Capture the exact coordinates for this request.
        GeoPoint pointToGeocode = new GeoPoint(
                selectedPoint.getLatitude(),
                selectedPoint.getLongitude()
        );

        // Every new request invalidates previous requests.
        int requestId = ++geocodingRequestId;

        isGeocoding = true;
        tvSelectedAddress.setText("Finding address...");

        new Thread(() -> {

            String addressText = "";
            String city = "";
            String state = "";
            String pincode = "";

            try {
                Geocoder geocoder = new Geocoder(
                        PickLocationActivity.this,
                        Locale.getDefault()
                );

                List<Address> addresses = geocoder.getFromLocation(
                        pointToGeocode.getLatitude(),
                        pointToGeocode.getLongitude(),
                        1
                );

                if (addresses != null && !addresses.isEmpty()) {

                    Address address = addresses.get(0);

                    addressText = address.getAddressLine(0) != null
                            ? address.getAddressLine(0) : "";

                    city = address.getLocality() != null
                            ? address.getLocality() : "";

                    state = address.getAdminArea() != null
                            ? address.getAdminArea() : "";

                    pincode = address.getPostalCode() != null
                            ? address.getPostalCode() : "";
                }

            } catch (IOException | IllegalArgumentException e) {
                Log.e("LOCATION_GEOCODER",
                        "Address lookup failed", e);
            }

            String finalAddressText = addressText;
            String finalCity = city;
            String finalState = state;
            String finalPincode = pincode;

            runOnUiThread(() -> {

                // Ignore results from outdated requests.
                if (requestId != geocodingRequestId) {
                    return;
                }

                selectedAddress = finalAddressText;
                selectedCity = finalCity;
                selectedState = finalState;
                selectedPincode = finalPincode;

                if (selectedAddress.isEmpty()) {
                    tvSelectedAddress.setText(
                            "Address unavailable. You can still confirm coordinates."
                    );
                } else {
                    tvSelectedAddress.setText(selectedAddress);
                }

                isGeocoding = false;
            });

        }).start();
    }

    private void confirmLocation() {

        if (selectedPoint == null) {
            Toast.makeText(
                    this,
                    "Please select a location.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (isGeocoding) {
            Toast.makeText(
                    this,
                    "Please wait while the address is being fetched.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent result = new Intent();

        result.putExtra(
                "latitude",
                selectedPoint.getLatitude()
        );

        result.putExtra(
                "longitude",
                selectedPoint.getLongitude()
        );

        result.putExtra("address", selectedAddress);
        result.putExtra("city", selectedCity);
        result.putExtra("state", selectedState);
        result.putExtra("pincode", selectedPincode);

        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            if (hasLocationPermission()) {
                moveToCurrentLocation();
            } else {
                Toast.makeText(
                        this,
                        "You can still select a location manually on the map.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
