package com.example.staffsyncapp.utils;

import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.staffsyncapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * NavigationManager class to handle bottom-navigation bar
 * thus seamless navigating between employee fragments
 * 
 * @param fragment: the fragment to navigate from i.e. navigation host fragment
 * @param bottomNav: the bottom navigation view to handle navigation
 */

public class NavigationManager {
    private static final String TAG = "NavigationService";
    private final NavController navController;
    private final BottomNavigationView bottomNav;


    public NavigationManager(Fragment fragment, BottomNavigationView bottomNav) {
        this.navController = Navigation.findNavController(fragment.requireActivity(), R.id.nav_host_fragment);
        this.bottomNav = bottomNav;
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            try {
                if (item.getItemId() == R.id.navigation_home) {
                    if (navController.getCurrentDestination().getId() != R.id.employee_navigation_home) {
                        navController.navigate(R.id.employee_navigation_home);
                    }
                    return true;
                } else if (item.getItemId() == R.id.navigation_profile) {
                    if (navController.getCurrentDestination().getId() != R.id.employee_navigation_profile) {
                        navController.navigate(R.id.employee_navigation_profile);
                    }
                    return true;
                } else if (item.getItemId() == R.id.navigation_settings) {
                    if (navController.getCurrentDestination().getId() != R.id.employee_navigation_settings) {
                        navController.navigate(R.id.employee_navigation_settings);
                    }
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Navigation failed: " + e.getMessage());
            }
            return false;
        });
    }

    private void navigateToDestination(int destinationId) {
        try {
            navController.navigate(destinationId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to destination " + destinationId + ": " + e.getMessage());
        }
    }

    public void navigateToLogin() {
        try {
            navController.navigate(R.id.action_EmployeeSettingsFragment_to_SecondFragment);
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to login: " + e.getMessage());
        }
    }
}