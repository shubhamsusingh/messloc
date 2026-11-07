package com.example.messloc.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.messloc.R;
import com.example.messloc.ui.auth.LoginActivity;
import com.example.messloc.utils.TokenManager;

public class ProfileFragment extends Fragment {
    public ProfileFragment(){}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Log.d("Profile","Profile called!");
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

        LinearLayout btnLogout = view.findViewById(R.id.logoutButton);

        btnLogout.setOnClickListener(v -> {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Logout", (dialog, which) -> logout())
                    .show();
        });
    }

    private void logout() {

        // Clear saved authentication session
        new TokenManager(requireContext()).clearSession();

        // Navigate to LoginActivity
        Intent intent = new Intent(
                requireActivity(),
                LoginActivity.class
        );

        // Clear previous activities from back stack
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        requireActivity().finish();
    }
}
