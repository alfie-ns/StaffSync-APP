package com.example.staffsyncapp;

// Android libraries for fragment lifecycle and UI setup
import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// AndroidX libraries for fragment navigation and annotations
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

// define databinding for start-up fragment
import com.example.staffsyncapp.databinding.StartUpFragmentBinding;

public class StartUpFragment extends Fragment {

    private StartUpFragmentBinding binding;
    private ApiDataService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // store binding reference
        binding = StartUpFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initially confirm COMP2000 API service is healthy
        apiService = new ApiDataService(requireContext());
        checkApiHealth();

        // set an onclick listener for the continue button to navigate to SecondFragment
        binding.continueButton.setOnClickListener(v ->
                NavHostFragment.findNavController(StartUpFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );
    }

    private void checkApiHealth() {
        apiService.checkHealth(response -> {
            updateApiStatus("API is working".equals(response), response);
        });
    }

    private void updateApiStatus(boolean isHealthy, String message) {
        // null check to prevent crash
        if (getActivity() != null && binding != null) {
            requireActivity().runOnUiThread(() -> {
                // another null check since binding could be nulled between checks
                if (binding != null) {
                    @SuppressLint("UseCompatLoadingForDrawables") Drawable icon = getResources().getDrawable(
                            isHealthy ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert
                    );
                    icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                    binding.apiStatusText.setCompoundDrawables(icon, null, null, null);
                    binding.apiStatusText.setText(message);
                }
            });
        }
    }

    @Override // clean up binding object
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}