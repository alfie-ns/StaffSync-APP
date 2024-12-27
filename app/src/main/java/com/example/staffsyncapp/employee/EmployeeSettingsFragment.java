package com.example.staffsyncapp.employee;

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

import com.example.staffsyncapp.api.ApiDataService;
import com.example.staffsyncapp.R;
import com.example.staffsyncapp.databinding.EmployeeSettingsFragmentBinding;
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NavigationManager;

/**
* TODO
*  [ ] make View Leave History and Pending Requests
*  [ ] notifications
*  [X] dark mode
*  [ ] terms and conditions
*  [ ] privacy
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
        // restore dark mode preference
        binding.darkModeSwitch.setChecked(sharedPreferences.getBoolean(employeeDarkModeKey, false));
    }

    private void setupClickListeners() {
        binding.logoutBtn.setOnClickListener(v -> handleLogout());

        binding.notificationSettings.setOnClickListener(v -> {
            // TODO: Implement notification settings
            Toast.makeText(requireContext(), "Notification settings coming soon", Toast.LENGTH_SHORT).show();
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