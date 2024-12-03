package com.example.staffsyncapp;

// Android libraries for fragment lifecycle and UI setup
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

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // inflate the layout using view binding
        binding = StartUpFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // set an onclick listener for the continue button to navigate to SecondFragment
        binding.continueButton.setOnClickListener(v ->
                NavHostFragment.findNavController(StartUpFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );
    }

    @Override // clean up binding object
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}