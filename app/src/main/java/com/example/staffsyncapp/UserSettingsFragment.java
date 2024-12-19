package com.example.staffsyncapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.staffsyncapp.databinding.UserSettingsFragmentBinding;
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NavigationManager;
import com.example.staffsyncapp.ApiDataService;
import com.example.staffsyncapp.utils.NotificationService;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;
import java.util.Locale;

/**
* TODO
*  [ ] make View Leave History and Pending Requests
*  [ ] notifications
*  [ ] dark mode(also implement in adminDashboard
*  [ ] terms and conditions
*  [ ] privacy
**/



public class UserSettingsFragment extends Fragment {
    private UserSettingsFragmentBinding binding;
    private LocalDataService dbHelper;
    private SharedPreferences sharedPreferences;
    private NavigationManager navigationManager;
    private ApiDataService apiService;
    private Employee currentEmployee;
    private static final String PREFS_NAME = "UserSettings";
    private static final String DARK_MODE_KEY = "dark_mode";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = UserSettingsFragmentBinding.inflate(inflater, container, false);
        dbHelper = new LocalDataService(requireContext());
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navigationManager = new NavigationManager(this, binding.bottomNavigation);
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_settings);

        setupUI();
        setupClickListeners();
    }

    private void setupUI() {
        // Restore dark mode preference
        binding.darkModeSwitch.setChecked(sharedPreferences.getBoolean(DARK_MODE_KEY, false));
    }

    private void setupClickListeners() {
        binding.logoutBtn.setOnClickListener(v -> handleLogout());

        binding.notificationSettings.setOnClickListener(v -> {
            // TODO: Implement notification settings
            Toast.makeText(requireContext(), "Notification settings coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(DARK_MODE_KEY, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            requireActivity().recreate();
        });

        binding.privacySettings.setOnClickListener(v -> {
            // TODO: implement privacy settings
            Toast.makeText(requireContext(), "Privacy settings coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.termsDocs.setOnClickListener(v -> {
            // TODO: Implement terms & documentation view
            Toast.makeText(requireContext(), "Documentation coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleLogout() {
        try {
            dbHelper.logoutUser();

            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.SecondFragment, true)
                    .build();

            Navigation.findNavController(requireView())
                    .navigate(R.id.action_UserSettingsFragment_to_SecondFragment, null, navOptions);

            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Logout failed, please try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}