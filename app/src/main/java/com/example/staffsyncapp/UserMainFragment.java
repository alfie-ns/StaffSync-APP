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
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.NavigationManager;
import android.widget.Toast;
import com.example.staffsyncapp.utils.LocalDataService;

import java.util.List;

/**
 * TODO
 *  [ ] book leave
 *  [ ] pending requests
 *  [ ] view leave history
 **/
public class UserMainFragment extends Fragment {
    private UserMainBodyFragmentBinding binding;
    private ApiDataService apiService;
    private Employee currentEmployee;
    private NavigationManager navigationManager;

    // TODO: get employee ID from local database
    //LocalDataService dbHelper = new LocalDataService(requireContext());
    //int employeeId = dbHelper.getLoggedInEmployeeId();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = UserMainBodyFragmentBinding.inflate(inflater, container, false);
        apiService = new ApiDataService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navigationManager = new NavigationManager(this, binding.bottomNavigation);
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_home);

        apiService = new ApiDataService(requireContext());

        setupUI();
        loadEmployeeData();
        setupClickListeners();
    }

    private void setupUI() {
        // set initial UI state
        binding.daysRemaining.setText("Days Remaining: 24/30");
        binding.employeeId.setText("Loading...");
        binding.department.setText("Loading...");
        binding.yearsOfService.setText("Loading...");
        binding.nextReview.setText("Loading...");
        binding.notificationStatus.setText("Notifications: Enabled");
    }

    private void loadEmployeeData() {
        // [ ] TODO: get employee ID from shared preferences or passed arguments
        int employeeId = 1; // TODO: employee = ?

        apiService.getEmployeeById(employeeId, new ApiDataService.EmployeeFetchListener() {
            @Override
            public void onEmployeesFetched(List<Employee> employees) {
                if (employees != null && !employees.isEmpty()) {
                    currentEmployee = employees.get(0);
                    updateUIWithEmployeeData(currentEmployee);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithEmployeeData(Employee employee) {
        binding.employeeId.setText("My Employee ID: #" + employee.getId());
        binding.department.setText("Department: " + employee.getDepartment());

        // Calculate years of service
        // TODO: implement proper date calculation
        binding.yearsOfService.setText("Years of Service: 2.5 years");

        // Calculate next review
        // TODO: implement proper review date calculation
        binding.nextReview.setText("Next Salary Review: 3 months");
    }

    private void setupClickListeners() {
        // Settings navigation
        binding.userSettingsIcon.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_UserMainFragment_to_UserSettingsFragment);
        });

        // Book leave button
        binding.bookLeaveBtn.setOnClickListener(v -> {
            // TODO: implement leave booking navigation
            Toast.makeText(requireContext(), "Leave booking coming soon", Toast.LENGTH_SHORT).show();
        });

        // Edit profile
        binding.userEditIcon.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_UserMainFragment_to_UserProfileFragment);
        });

        // View history
        binding.viewHistoryBtn.setOnClickListener(v -> {
            // TODO: implement history view
            Toast.makeText(requireContext(), "Leave history coming soon", Toast.LENGTH_SHORT).show();
        });

        // Pending requests
        binding.pendingRequestsBtn.setOnClickListener(v -> {
            // TODO: implement pending requests view
            Toast.makeText(requireContext(), "Pending requests coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}