package com.example.staffsyncapp.employee;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.staffsyncapp.api.ApiDataService;
import com.example.staffsyncapp.R;
import com.example.staffsyncapp.databinding.EmployeeSettingsFragmentBinding;
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NavigationManager;
import com.example.staffsyncapp.utils.NotificationService;
import com.google.android.material.button.MaterialButton;

/**
* TODO
*  [X] make View Leave History and Pending Requests
*  [X] notifications
*  [X] dark mode
*  [X] terms and conditions
*  [X] privacy
*/



public class EmployeeSettingsFragment extends Fragment {
    private EmployeeSettingsFragmentBinding binding;
    private LocalDataService dbHelper;
    private SharedPreferences sharedPreferences;
    private NavigationManager navigationManager;
    private ApiDataService apiService;
    private Employee currentEmployee;
    private static final String PREFS_NAME = "employeeSettings";
    private static final String DARK_MODE_KEY = "dark_mode";

    private static final String NOTIFICATIONS_KEY = "notifications_enabled";
    private static final String TERMS_KEY = "terms_accepted";
    private static final String PRIVACY_KEY = "privacy_accepted";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = EmployeeSettingsFragmentBinding.inflate(inflater, container, false);
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
        SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int employeeId = prefs.getInt("logged_in_employee_id", -1);
        String employeeDarkModeKey = DARK_MODE_KEY + "_" + employeeId;
        binding.notificationsSwitch.setChecked(prefs.getBoolean("notifications_enabled_" + employeeId, true));
        // restore dark mode preference
        binding.darkModeSwitch.setChecked(sharedPreferences.getBoolean(employeeDarkModeKey, false));

        // forgot i've already change this in notificationService thus removed it and replaced with notification preference check
        if (employeeId != -1) {
            NotificationService notificationService = new NotificationService(requireContext());
            binding.notificationsSwitch.setChecked(notificationService.isNotificationsEnabled(employeeId));
        }
    }

    private void setupClickListeners() {
        binding.logoutBtn.setOnClickListener(v -> handleLogout());

        binding.notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
            int employeeId = prefs.getInt("logged_in_employee_id", -1);
            prefs.edit().putBoolean("notifications_enabled_" + employeeId, isChecked).apply();

            if (employeeId != -1) {
                NotificationService notificationService = new NotificationService(requireContext());
                notificationService.setNotificationsEnabled(employeeId, isChecked);
            }
        });

        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
            int employeeId = prefs.getInt("logged_in_employee_id", -1);
            String employeeDarkModeKey = DARK_MODE_KEY + "_" + employeeId;

            sharedPreferences.edit().putBoolean(employeeDarkModeKey, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            requireActivity().recreate();
        });

        binding.privacySettings.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.employee_privacy_settings_dialog, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();

            MaterialButton deleteHistoryBtn = dialogView.findViewById(R.id.delete_history_btn);
            deleteHistoryBtn.setOnClickListener(buttonView -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Leave History")
                        .setMessage("Are you sure you want to delete all your past leave history? This will not affect pending requests.")
                        .setPositiveButton("Delete All", (confirmDialog, which) -> {
                            SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
                            int employeeId = prefs.getInt("logged_in_employee_id", -1);

                            if (employeeId != -1) { // if valid
                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                // Only delete requests that have been processed (approved or denied) and thus not pending
                                db.delete("leave_requests",
                                        "employee_id = ? AND status != ?",
                                        new String[]{String.valueOf(employeeId), "pending"}
                                );
                                Toast.makeText(requireContext(), "Past leave history deleted", Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            dialog.show();
        });

        binding.termsDocs.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.employee_terms_documentation_dialog, null);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void handleLogout() {
        try {
            binding.darkModeSwitch.setChecked(false); // reset switch state before logout to ensure it matches system default i.e. off, when logged back in
            SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
            int employeeId = prefs.getInt("logged_in_employee_id", -1);
            String employeeDarkModeKey = DARK_MODE_KEY + "_" + employeeId;

            // reset dark mode to system default and clear preference
            sharedPreferences.edit().remove(employeeDarkModeKey).apply();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            requireActivity().recreate();

            dbHelper.logoutEmployee();

            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.SecondFragment, true)
                    .build();

            Navigation.findNavController(requireView())
                    .navigate(R.id.action_EmployeeSettingsFragment_to_SecondFragment, null, navOptions);

            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("Logout", "Failed to logout: " + e.getMessage());
            Toast.makeText(requireContext(), "Logout failed, please try again", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}