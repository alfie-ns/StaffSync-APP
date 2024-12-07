package com.example.staffsyncapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.staffsyncapp.databinding.UserSettingsFragmentBinding;

/*
* TODO
*  [ ] notifications
*  [ ] dark mode(also implement in adminDashboard
*  [ ] terms and conditions
*  [ ] privacy
* */

public class UserSettingsFragment extends Fragment {
    private UserSettingsFragmentBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = UserSettingsFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}