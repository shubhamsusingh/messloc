
package com.example.messloc.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.messloc.R;
import com.example.messloc.data.model.UserProfile;
import com.example.messloc.ui.auth.LoginActivity;
import com.example.messloc.ui.auth.OwnerSignupActivity;
import com.example.messloc.utils.TokenManager;
import com.example.messloc.utils.UserLocationStorage;
import com.example.messloc.viewmodel.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private ProfileViewModel profileViewModel;
    private LinearLayout myMessesButton;
    private TextView profileName;
    private TextView profileEmail;
    private TextView profileRole;

    private TextView detailName;
    private TextView detailEmail;
    private TextView detailPhone;
    private TextView detailAddress;
    UserLocationStorage locationStorage;
    private LinearLayout logoutButton;
    private LinearLayout registerMessCard;

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Log.d("Profile", "Profile called!");

        return inflater.inflate(
                R.layout.fragment_profile,
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
        setupViewModel();
        setupListeners();

        profileViewModel.loadProfile();

       locationStorage =
                new UserLocationStorage(requireContext());

        // Display previously saved address

    }

    private void initializeViews(View view) {

        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
//        My messes
        myMessesButton = view.findViewById(R.id.myMessesButton);

        myMessesButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    MyMessesActivity.class
            );
            startActivity(intent);
        });
        detailName = view.findViewById(R.id.detailName);
        detailEmail = view.findViewById(R.id.detailEmail);
        detailPhone = view.findViewById(R.id.detailPhone);
        detailAddress = view.findViewById(R.id.detailAddress);

        logoutButton = view.findViewById(R.id.logoutButton);
        registerMessCard = view.findViewById(R.id.registerMessCard);

        // Add this ID to the role TextView in your XML.
        profileRole = view.findViewById(R.id.profileRole);
    }

    private void setupViewModel() {

        profileViewModel = new ViewModelProvider(this)
                .get(ProfileViewModel.class);

        profileViewModel.getProfile().observe(
                getViewLifecycleOwner(),
                this::displayProfile
        );

        profileViewModel.getLoading().observe(
                getViewLifecycleOwner(),
                isLoading -> {
                    if (Boolean.TRUE.equals(isLoading)) {
                        profileName.setText("Loading profile...");
                    }
                }
        );

        profileViewModel.getErrorMessage().observe(
                getViewLifecycleOwner(),
                message -> {
                    if (message != null && !message.isEmpty()) {
                        Toast.makeText(
                                requireContext(),
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void displayProfile(UserProfile profile) {

        if (profile == null) {
            return;
        }

        profileName.setText(valueOrFallback(
                profile.getName(), "User"
        ));

        profileEmail.setText(valueOrFallback(
                profile.getEmail(), "No email available"
        ));

        detailName.setText(valueOrFallback(
                profile.getName(), "Not available"
        ));

        detailEmail.setText(valueOrFallback(
                profile.getEmail(), "Not available"
        ));

        detailPhone.setText(valueOrFallback(
                profile.getPhone(), "Not provided"
        ));

        detailAddress.setText(valueOrFallback(
                profile.getCity(), locationStorage.getAddress()
        ));

        if (profileRole != null) {
            profileRole.setText(valueOrFallback(
                    profile.getRole(), "USER"
            ));
            setupOwnerSection(profile.getRole());
        }

    }
    private void setupOwnerSection(String role) {

        if (role == null) {
            role = "USER";
        }

        boolean isOwner = role.equalsIgnoreCase("OWNER");
        boolean isUser = role.equalsIgnoreCase("USER");

        // Register Your Mess
        if (registerMessCard != null) {
            registerMessCard.setVisibility(
                    (isUser ) ? View.VISIBLE : View.GONE
            );
        }

        // My Messes
        if (myMessesButton != null) {
            myMessesButton.setVisibility(
                    isOwner ? View.VISIBLE : View.GONE
            );
        }
    }
    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value;
    }

    private void setupListeners() {

        registerMessCard.setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    OwnerSignupActivity.class
            );
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(
                            "Logout",
                            (dialog, which) -> logout()
                    )
                    .show();
        });
    }

    private void logout() {

        new TokenManager(requireContext()).clearSession();

        Intent intent = new Intent(
                requireActivity(),
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (profileViewModel != null) {
            profileViewModel.loadProfile();
        }
    }
}
