package com.example.messloc.ui.main;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.messloc.R;
import com.example.messloc.ui.home.HomeFragment;
import com.example.messloc.ui.profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_home);

        Log.d("MainActivity", "onCreate() is running");

        bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setOnItemSelectedListener(item -> {

            Fragment selectedFragment;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if(itemId==R.id.nav_profile){
                selectedFragment = new ProfileFragment();
            }else{
                return false;

            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.mainFragmentContainer,
                            selectedFragment
                    )
                    .commit();

            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.mainFragmentContainer,
                            new HomeFragment()
                    )
                    .commit();

            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }
}