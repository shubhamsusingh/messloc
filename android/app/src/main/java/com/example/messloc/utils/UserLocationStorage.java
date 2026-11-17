
package com.example.messloc.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class UserLocationStorage {

    private static final String PREF_NAME = "user_location";

    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_UPDATED_AT = "updated_at";

    private final SharedPreferences preferences;

    public UserLocationStorage(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveCoordinates(double latitude, double longitude) {
        preferences.edit()
                .putString(KEY_LATITUDE, String.valueOf(latitude))
                .putString(KEY_LONGITUDE, String.valueOf(longitude))
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public void saveAddress(String address) {
        preferences.edit()
                .putString(KEY_ADDRESS, address)
                .apply();
    }

    public double getLatitude() {
        return Double.parseDouble(
                preferences.getString(KEY_LATITUDE, "0.0")
        );
    }

    public double getLongitude() {
        return Double.parseDouble(
                preferences.getString(KEY_LONGITUDE, "0.0")
        );
    }

    public String getAddress() {
        return preferences.getString(KEY_ADDRESS, null);
    }

    public long getLastUpdated() {
        return preferences.getLong(KEY_UPDATED_AT, 0);
    }

    public boolean hasLocation() {
        return preferences.contains(KEY_LATITUDE)
                && preferences.contains(KEY_LONGITUDE);
    }

    public void clearLocation() {
        preferences.edit().clear().apply();
    }
}
