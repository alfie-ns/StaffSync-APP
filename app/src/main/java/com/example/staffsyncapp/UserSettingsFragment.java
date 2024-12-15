package com.example.staffsyncapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.staffsyncapp.databinding.UserSettingsFragmentBinding;
import com.example.staffsyncapp.utils.LocalDataService;

/*
* TODO
*  [ ] make View Leave History and Pending Requests
*  [ ] notifications
*  [ ] dark mode(also implement in adminDashboard
*  [ ] terms and conditions
*  [ ] privacy
* */

public class UserSettingsFragment extends Fragment {
    private UserSettingsFragmentBinding binding;
    private LocalDataService dbHelper;
    private static final String TAG = "UserSettingsFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = UserSettingsFragmentBinding.inflate(inflater, container, false);
        dbHelper = new LocalDataService(requireContext());
        setupLogoutButton();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }

    private void setupLogoutButton() {
        binding.logoutBtn.setOnClickListener(v -> {
            Log.d(TAG, "Processing user logout; showing confirmation dialog");

            // utilise AlertDialog and show confirmation
            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Logout")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Log.d(TAG, "Processing user logout...");
                        try {
                            // clear user session
                            dbHelper.logoutUser();

                            // create navigation options to prevent back navigation
                            NavOptions navOptions = new NavOptions.Builder()
                                    .setPopUpTo(R.id.SecondFragment, true)
                                    .build();

                            // navigate back to login fragment
                            NavHostFragment.findNavController(this)
                                    .navigate(R.id.action_UserSettingsFragment_to_SecondFragment,
                                            null,
                                            navOptions);

                            // show success message
                            Toast.makeText(requireContext(),
                                    "Logged out successfully",
                                    Toast.LENGTH_SHORT).show();

                        } catch (Exception e) {
                            Log.e(TAG, "Logout failed", e);
                            Toast.makeText(requireContext(),
                                    "Logout failed; please try again",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)  // dismisses dialog if user clicks No
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}