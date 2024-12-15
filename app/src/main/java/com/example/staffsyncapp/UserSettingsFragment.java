package com.example.staffsyncapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.staffsyncapp.databinding.UserSettingsFragmentBinding;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NavigationManager;
import com.example.staffsyncapp.utils.NotificationService;

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
    private NavigationManager navigationManager;
    private LocalDataService dbHelper;
    private NotificationService notificationService;
    private static final String TAG = "UserSettingsFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = UserSettingsFragmentBinding.inflate(inflater, container, false);
        dbHelper = new LocalDataService(requireContext());
        notificationService = new NotificationService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navigationManager = new NavigationManager(this, binding.bottomNavigation);
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_settings);

        setupLogoutButton();
    }

    private void setupLogoutButton() {
        binding.logoutBtn.setOnClickListener(v -> {
            dbHelper.logoutUser();
            navigationManager.navigateToLogin();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}