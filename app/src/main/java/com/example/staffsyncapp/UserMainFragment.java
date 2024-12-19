package com.example.staffsyncapp;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

    private EmployeeAdapter.EmployeeViewModel employeeViewModel;

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

        employeeViewModel = new EmployeeAdapter.EmployeeViewModel();

        apiService = new ApiDataService(requireContext());

        setupUI();
        loadEmployeeData();
        setupClickListeners();
    }

    @Override
    public void onResume() { // resume the employee respective-data-field get
        super.onResume();
        Employee employee = employeeViewModel.getEmployeeLiveData().getValue();
        if (employee != null) {
            updateUIWithEmployeeData(employee);
        } else {
            loadEmployeeData();
        }
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
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int employeeId = prefs.getInt("logged_in_employee_id", -1);

        if (employeeId != -1) { // if ID is valid then fetch employee data by the respective ID
            apiService.getEmployeeById(employeeId, new ApiDataService.EmployeeFetchListener() {
                @Override
                public void onEmployeesFetched(List<Employee> employees) {
                    Log.d(TAG, "Fetched employees: " + employees);
                    if (employees != null && !employees.isEmpty()) {
                        currentEmployee = employees.get(0);
                        updateUIWithEmployeeData(currentEmployee);
                    }
                }

                @Override
                public void onError(String error) {
                    if (getContext() != null) {
                        Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
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