
package com.example.messloc.ui.home;

import android.widget.ProgressBar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.viewmodel.HomeViewModel;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private static final String TAG = "HOME_FRAGMENT";
    private ProgressBar progressMesses;
    private HomeViewModel homeViewModel;
    private MessAdapter messAdapter;

    private RecyclerView rvNearbyMesses;
    private LinearLayout layoutEmptyMesses;
    private LinearLayout layoutMessError;

    public HomeFragment() {
        // Required empty public constructor
    }

    // Inflate HomeFragment layout
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        Log.d(TAG, "HomeFragment onCreateView called!");

        return inflater.inflate(
                R.layout.fragment_home,
                container,
                false
        );
    }

    // Initialize views and API
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "HomeFragment onViewCreated called!");

        // Initialize views
        rvNearbyMesses = view.findViewById(R.id.rvNearbyMesses);
        layoutEmptyMesses = view.findViewById(R.id.layoutEmptyMesses);
        layoutMessError = view.findViewById(R.id.layoutMessError);
        progressMesses = view.findViewById(R.id.progressMesses);

        // Initialize RecyclerView
        messAdapter = new MessAdapter();

        rvNearbyMesses.setHasFixedSize(false);   // or just remove the line
        rvNearbyMesses.setNestedScrollingEnabled(false);
        rvNearbyMesses.setLayoutManager(new LinearLayoutManager(requireContext()));

        rvNearbyMesses.setAdapter(messAdapter);

        // Initialize ViewModel
        homeViewModel = new ViewModelProvider(this)
                .get(HomeViewModel.class);

        // Observe loading-independent mess results
        homeViewModel.getMesses().observe(
                getViewLifecycleOwner(),
                messes -> {

                    if (messes == null) {
                        return;
                    }


                    if (!messes.isEmpty()) {

                        Log.d(TAG, "Messes received: " + messes.size());

                        messAdapter.setMesses(messes);

                        rvNearbyMesses.setVisibility(View.VISIBLE);
                        layoutEmptyMesses.setVisibility(View.GONE);
                        layoutMessError.setVisibility(View.GONE);

                    } else {

                        Log.d(TAG, "No messes found");

                        messAdapter.setMesses(new ArrayList<>());

                        rvNearbyMesses.setVisibility(View.GONE);
                        layoutEmptyMesses.setVisibility(View.VISIBLE);
                        layoutMessError.setVisibility(View.GONE);
                    }
                }
        );
        // Observe the loading
        homeViewModel.getLoading().observe(
                getViewLifecycleOwner(),
                isLoading -> {

                    if (Boolean.TRUE.equals(isLoading)) {

                        // Show loading indicator
                        progressMesses.setVisibility(View.VISIBLE);

                        // Hide other states
                        rvNearbyMesses.setVisibility(View.GONE);
                        layoutEmptyMesses.setVisibility(View.GONE);
                        layoutMessError.setVisibility(View.GONE);

                        Log.d(TAG, "Loading messes...");

                    } else {

                        // Hide loading indicator
                        progressMesses.setVisibility(View.GONE);

                        Log.d(TAG, "Loading completed");
                    }
                }
        );

        // Observe API errors
        homeViewModel.getError().observe(
                getViewLifecycleOwner(),
                error -> {

                    if (error != null && !error.isEmpty()) {

                        Log.e(TAG, "API Error: " + error);

                        rvNearbyMesses.setVisibility(View.GONE);
                        layoutEmptyMesses.setVisibility(View.GONE);
                        layoutMessError.setVisibility(View.VISIBLE);
                    }
                }
        );


        // Retry buttons
        view.findViewById(R.id.btnRetryMesses)
                .setOnClickListener(v -> homeViewModel.fetchMesses());

        view.findViewById(R.id.btnRetryError)
                .setOnClickListener(v -> homeViewModel.fetchMesses());

        // Fetch messes
        Log.d(TAG, "Calling fetchMesses()");

        homeViewModel.fetchMesses();
    }
}
