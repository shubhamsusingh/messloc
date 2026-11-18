
package com.example.messloc.ui.subscriptions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messloc.R;
import com.example.messloc.data.model.subscription.Subscription;
import com.example.messloc.ui.subscriptions.adapter.SubscriptionAdapter;
import com.example.messloc.viewmodel.SubscriptionViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubscriptionsFragment extends Fragment {

    private SubscriptionViewModel viewModel;
    private SubscriptionAdapter adapter;

    private RecyclerView subscriptionsRecyclerView;
    private View emptySubscriptionsLayout;
    private View subscriptionsErrorLayout;
    private View subscriptionsProgressBar;

    private TextView activeSubscriptionCount;
    private TextView totalSubscriptionCount;

    private ChipGroup subscriptionFilterGroup;

    private List<Subscription> allSubscriptions = new ArrayList<>();

    private String selectedFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_subscriptions,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupViewModel();
        setupFilters();
        setupRetryButton();
    }

    private void initializeViews(View view) {

        subscriptionsRecyclerView =
                view.findViewById(R.id.subscriptionsRecyclerView);

        emptySubscriptionsLayout =
                view.findViewById(R.id.emptySubscriptionsLayout);

        subscriptionsErrorLayout =
                view.findViewById(R.id.subscriptionsErrorLayout);

        subscriptionsProgressBar =
                view.findViewById(R.id.subscriptionsProgressBar);

        activeSubscriptionCount =
                view.findViewById(R.id.activeSubscriptionCount);

        totalSubscriptionCount =
                view.findViewById(R.id.totalSubscriptionCount);

        subscriptionFilterGroup =
                view.findViewById(R.id.subscriptionFilterGroup);
    }

    private void setupRecyclerView() {

        subscriptionsRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new SubscriptionAdapter(subscription ->
                showCancelConfirmation(subscription)
        );

        subscriptionsRecyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(SubscriptionViewModel.class);

        viewModel.getSubscriptions().observe(
                getViewLifecycleOwner(),
                subscriptions -> {

                    allSubscriptions = subscriptions != null
                            ? subscriptions
                            : new ArrayList<>();

                    updateSummaryCounts();
                    applyFilter();

                    subscriptionsErrorLayout.setVisibility(View.GONE);
                }
        );

        viewModel.getLoading().observe(
                getViewLifecycleOwner(),
                loading -> {

                    boolean isLoading = Boolean.TRUE.equals(loading);

                    subscriptionsProgressBar.setVisibility(
                            isLoading ? View.VISIBLE : View.GONE
                    );
                }
        );

        viewModel.getError().observe(
                getViewLifecycleOwner(),
                error -> {

                    if (error != null && !error.trim().isEmpty()) {

                        subscriptionsErrorLayout.setVisibility(View.VISIBLE);
                        emptySubscriptionsLayout.setVisibility(View.GONE);

                        Toast.makeText(
                                requireContext(),
                                error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );

        viewModel.getMessage().observe(
                getViewLifecycleOwner(),
                message -> {

                    if (message != null && !message.trim().isEmpty()) {

                        Toast.makeText(
                                requireContext(),
                                message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void setupFilters() {

        Chip chipAll = requireView().findViewById(R.id.chipAll);
        Chip chipActive = requireView().findViewById(R.id.chipActive);
        Chip chipExpired = requireView().findViewById(R.id.chipExpired);
        Chip chipCancelled = requireView().findViewById(R.id.chipCancelled);

        chipAll.setOnClickListener(v -> selectFilter("ALL"));
        chipActive.setOnClickListener(v -> selectFilter("ACTIVE"));
        chipExpired.setOnClickListener(v -> selectFilter("EXPIRED"));
        chipCancelled.setOnClickListener(v -> selectFilter("CANCELLED"));

        subscriptionFilterGroup.check(R.id.chipAll);
    }

    private void selectFilter(String filter) {

        selectedFilter = filter;
        applyFilter();
    }

    private void applyFilter() {

        List<Subscription> filteredList = new ArrayList<>();

        for (Subscription subscription : allSubscriptions) {

            String status = subscription.getStatus() != null
                    ? subscription.getStatus().toUpperCase(Locale.ROOT)
                    : "";

            if ("ALL".equals(selectedFilter)
                    || selectedFilter.equals(status)) {

                filteredList.add(subscription);
            }
        }

        adapter.setSubscriptions(filteredList);

        boolean isEmpty = filteredList.isEmpty();

        emptySubscriptionsLayout.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        subscriptionsRecyclerView.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );
    }

    private void updateSummaryCounts() {

        int activeCount = 0;

        for (Subscription subscription : allSubscriptions) {

            if (subscription.getStatus() != null
                    && "ACTIVE".equalsIgnoreCase(subscription.getStatus())) {

                activeCount++;
            }
        }

        activeSubscriptionCount.setText(String.valueOf(activeCount));
        totalSubscriptionCount.setText(
                String.valueOf(allSubscriptions.size())
        );
    }

    private void setupRetryButton() {

        MaterialButton retryButton =
                requireView().findViewById(R.id.retrySubscriptionsButton);

        retryButton.setOnClickListener(v -> {
            subscriptionsErrorLayout.setVisibility(View.GONE);
            viewModel.loadMySubscriptions();
        });
    }

    private void showCancelConfirmation(Subscription subscription) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Subscription")
                .setMessage(
                        "Are you sure you want to cancel your subscription to "
                                + subscription.getMessName() + "?"
                )
                .setNegativeButton("Keep Subscription", (dialog, which) ->
                        dialog.dismiss()
                )
                .setPositiveButton("Cancel Subscription", (dialog, which) ->
                        viewModel.cancelSubscription(subscription.getId())
                )
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (viewModel != null) {
            viewModel.loadMySubscriptions();
        }
    }
}
