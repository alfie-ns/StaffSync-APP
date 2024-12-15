package com.example.staffsyncapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.staffsyncapp.databinding.UserProfileFragmentBinding;
import com.example.staffsyncapp.utils.NavigationManager;


/** TODO
* [ ] implement functionality to edit own details
* [ ] test that the details change by looking on adminDashboard fragment
**/

public class UserProfileFragment extends Fragment {
    private UserProfileFragmentBinding binding;
    private NavigationManager navigationManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = UserProfileFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navigationManager = new NavigationManager(this, binding.bottomNavigation);
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_profile);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}