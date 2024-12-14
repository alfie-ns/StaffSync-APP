package com.example.staffsyncapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.staffsyncapp.databinding.UserMainBodyFragmentBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * TODO
 *  [ ] book leave
 *  [ ] pending requests
 *  [ ] view leave history
 **/
public class UserMainFragment extends Fragment {
    private UserMainBodyFragmentBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = UserMainBodyFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;

        // Remove the inflateMenu call since it's already set in XML
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                // Already on home, do nothing or refresh
                return true;
            }
            else if (itemId == R.id.navigation_profile) {
                navigateToFragment(R.id.action_UserMainFragment_to_UserProfileFragment);
                return true;
            }
            else if (itemId == R.id.navigation_settings) {
                navigateToFragment(R.id.action_UserMainFragment_to_UserSettingsFragment);
                return true;
            }

            return false;
        });
    }

    /**
     * Handles fragment navigation using the Navigation component.
     * - Takes a navigation action ID from the nav_graph and attempts to navigate,
     * providing error handling for failed navigation attempts.
     * 
     * @param actionId The ID of the navigation action defined in nav_graph.xml
     */
    private void navigateToFragment(int actionId) { // navigate to another fragment
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(actionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}