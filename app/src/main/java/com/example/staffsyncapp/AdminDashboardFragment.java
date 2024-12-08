package com.example.staffsyncapp;

// Android libraries for UI, data handling, and permissions
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

// additional AndroidX imports for fragment and annotations
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

// data-binding and utility classes specific to the project
import com.example.staffsyncapp.databinding.AdminDashboardFragmentBinding;
import com.example.staffsyncapp.utils.LocalDataService;

// navigation utilities for fragment transitions
import androidx.navigation.NavOptions;

// context, SharedPreferences
import android.content.Context;
import android.content.SharedPreferences;

// Android activity result and permission handling utilities

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Java utilities for collections and list handling
import java.util.List;

//import android.widget.ProgressBar; TODO loading bar [ ]

import com.example.staffsyncapp.models.Employee;

public class AdminDashboardFragment extends Fragment {

    /*
    TODO:
        - [X] AdminDashboardFragmentBinding
        - [X] incorporate local db
        - [X] logout functionality
        - [ ] notifications 1: get notifications alerting
        - [X] fetch ALL employees from comp2000
        - [X] list employees searched for; filteredList
        - [X] EmployeeList2: when an id is searched for, allow to remove the id and thus the app stops searching for it
        - [ ] notifications 2; make notification allow to be pressed
              on to take them to the relevant info, add actions
              to notifications, it should let the admin
              approve or deny directly from the notification(maybe,
              and if so it should probably give some insight in
              the notification(maybe using Gemini?)
        - [ ] employee account to submit leave requests 


    */

    // Constants
    private static final String TAG = "AdminDashboard";
    private static final String PREFS_NAME = "AdminSettings";

    // Core variables
    private AdminDashboardFragmentBinding binding;
    private LocalDataService dbHelper;
    private ApiDataService employeeDataService;
    private ProgressBar progressBar;
    private int totalEmployeeCount = 0;

    // Employee search functionality variables
    private EditText searchEmployeeInput;

    private com.example.staffsyncapp.EmployeeAdapter employeeAdapter;

    // employee list collapse functionality variables
    private boolean isEmployeeListExpanded = true;
    private ImageButton collapseButton;
    private View employeeListContent;
    private static final String EMPLOYEE_LIST_EXPANDED_KEY = "employee_list_expanded"; // toggle-state key

    // shared preferences' used to store the toggle state
    private SharedPreferences sharedPreferences;
    
    //----------------------------------------------------------------------------------------------
    // Classes
    private class SearchTextWatcher implements TextWatcher { // search text change class; watches for changes in search input and triggers the filtering
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            handleSearchTextChange(s.toString().trim());
        }
    
        @Override
        public void afterTextChanged(Editable s) {} // all 3 methods must be implemented in TextWatcher even if empty
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) { // set up binding; database and API services
        binding = AdminDashboardFragmentBinding.inflate(inflater, container, false);
        dbHelper = new LocalDataService(requireContext());
        employeeDataService = new ApiDataService(requireContext());
        return binding.getRoot();
    }
    @Override // override to initialise UI components and set up listeners
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); // initialise shared preferences

        fetchAndShowEmployees(); // initial load

        // dynamic employee list with RecyclerView [X]

        RecyclerView recyclerView = binding.recyclerViewEmployees; // 1- bind to respective .xml

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)); // 2- set layout manager

        recyclerView.setNestedScrollingEnabled(false); // 3- disable nested scrolling

        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER); // 4- disable over-scrolling

        recyclerView.setVisibility(View.VISIBLE); // 5- show list

        setupClickListeners(); // check each click listener
        setupSearchFunctionality(); // setup employee list search functionality

        collapseButton = binding.collapseEmployeeList; // collapse list button
        employeeListContent = binding.employeeListContent;

        collapseButton.setOnClickListener(v -> toggleEmployeeList()); // when clicked switch state

        isEmployeeListExpanded = sharedPreferences.getBoolean(EMPLOYEE_LIST_EXPANDED_KEY, true);
        updateEmployeeListVisibility();

    }
    //----------------------------------------------------------------------------------------------
    // Search functionality
    private void handleSearchTextChange(String searchText) {
        if (binding.searchByIdCheckbox.isChecked()) {
            handleIdSearch(searchText);
        } else {
            handleNameSearch(searchText);
        }
    }
    
    private void handleIdSearch(String searchText) {
        if (!searchText.isEmpty()) {
            try {
                int employeeId = Integer.parseInt(searchText);
                searchEmployeeById(employeeId);
            } catch (NumberFormatException e) {
                showInvalidIdError();
            }
        } else {
            fetchAndShowEmployees();  // Reset when empty
        }
    }
    
    private void handleNameSearch(String searchText) {
        if (employeeAdapter != null) {
            employeeAdapter.filter(searchText);
        }
    }
    
    private void showInvalidIdError() {
        Toast.makeText(requireContext(),
                "Please enter a valid ID number",
                Toast.LENGTH_SHORT).show();
    }
    
    private void setupSearchFunctionality() {
        EditText searchInput = binding.searchEmployee;
        searchInput.addTextChangedListener(new SearchTextWatcher());
    }
    //----------------------------------------------------------------------------------------------
    // Employee management
    private void setupEmployeeAdapter(List<Employee> employees) {
        employeeAdapter = new EmployeeAdapter(employees);

        // Set up the delete listener; 
        employeeAdapter.setOnEmployeeDeleteListener(employee -> {
            employeeDataService.deleteEmployee(
                    employee.getId(),
                    new ApiDataService.EmployeeDeleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            // refresh the list after deletion
                            fetchAndShowEmployees();
                            Toast.makeText(requireContext(),
                                    "Employee deleted successfully",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(requireContext(),
                                    "Error: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        binding.recyclerViewEmployees.setAdapter(employeeAdapter);
    }
    
    private void fetchAndShowEmployees() {
        ApiDataService.getAllEmployees(new ApiDataService.EmployeeFetchListener() {
            @Override
            public void onEmployeesFetched(List<Employee> employees) {
                if (getContext() == null) return;

                // hide loading indicator
                binding.progressBar.setVisibility(View.GONE);

                if (employees != null && !employees.isEmpty()) {
                    totalEmployeeCount = employees.size();
                    // 1- initialise employee list adapter && assign it
                    setupEmployeeAdapter(employees);
                    // 2- display the populated list; update total count
                    binding.recyclerViewEmployees.setVisibility(View.VISIBLE);
                    binding.totalEmployeesCount.setText(String.valueOf(employees.size()));
                } else {
                    Toast.makeText(requireContext(), "No employees found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (getContext() == null) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void searchEmployeeById(int id) {
        binding.progressBar.setVisibility(View.VISIBLE); // show loading indicator

        // Create interface to handle the response
        ApiDataService.EmployeeFetchListener listener = new ApiDataService.EmployeeFetchListener() {
            @Override
            public void onEmployeesFetched(List<Employee> employees) {
                binding.progressBar.setVisibility(View.GONE);
                if (employees != null && !employees.isEmpty()) {
                    if (employeeAdapter != null) {
                        employeeAdapter.updateDisplayList(employees); // update existing adapter
                    } else {
                        setupEmployeeAdapter(employees); // create new adapter if none exists
                    }
                    // don't update total count as it should show ALL employees
                }
            }

            @Override
            public void onError(String error) {
                if (getContext() == null) return;

                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Error: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        };

        // Call the API
        employeeDataService.getEmployeeById(id, listener);
    }
    //----------------------------------------------------------------------------------------------
    // Dialog management
    private void showAddEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.admin_add_employee_dialog, null);
        builder.setView(dialogView);

        // get references to form fields
        EditText firstNameInput = dialogView.findViewById(R.id.firstNameInput);
        EditText lastNameInput = dialogView.findViewById(R.id.lastNameInput);
        EditText emailInput = dialogView.findViewById(R.id.emailInput);
        EditText departmentInput = dialogView.findViewById(R.id.departmentInput);
        EditText salaryInput = dialogView.findViewById(R.id.salaryInput);
        EditText joiningDateInput = dialogView.findViewById(R.id.joiningDateInput);

        AlertDialog dialog = builder.create();

        // handle save button click
        dialogView.findViewById(R.id.saveButton).setOnClickListener(v -> {
            // validate inputs
            if (validateInputs(firstNameInput, lastNameInput, emailInput,
                    departmentInput, salaryInput, joiningDateInput)) {

                double salary = Double.parseDouble(salaryInput.getText().toString());

                // call API to add employee; passing the relevant JSON
                employeeDataService.addEmployee(
                        firstNameInput.getText().toString(),
                        lastNameInput.getText().toString(),
                        emailInput.getText().toString(),
                        departmentInput.getText().toString(),
                        salary,
                        joiningDateInput.getText().toString(),
                        new ApiDataService.EmployeeAddListener() {
                            @Override
                            public void onSuccess(String message) {
                                dialog.dismiss();
                                Toast.makeText(requireContext(),
                                        "Employee added successfully",
                                        Toast.LENGTH_SHORT).show();
                                fetchAndShowEmployees(); // Refresh list
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(requireContext(),
                                        "Error: " + error,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
        });

        dialog.show();
    }
    //----------------------------------------------------------------------------------------------
    // Validation
    private boolean validateInputs(EditText... inputs) {
        // 1- basic empty check for each field
        for (EditText input : inputs) {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) { // if any field is empty
                input.setError("This field is required");
                return false;
            }
        }
    
        // -2 check specific field validations
        String email = inputs[2].getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputs[2].setError("Please enter a valid email address");
            return false;
        }
    
        // -3 validate salary is an integer > 0
        try {
            double salary = Double.parseDouble(inputs[4].getText().toString().trim());
            if (salary <= 0) {
                inputs[4].setError("Salary must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            inputs[4].setError("Please enter a valid salary");
            return false;
        }
    
        // -4 validate date format (YYYY-MM-DD)
        String date = inputs[5].getText().toString().trim();
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            inputs[5].setError("Date must be in YYYY-MM-DD format");
            return false;
        }
        // 5- success
        return true;
    }
    //----------------------------------------------------------------------------------------------
    // UI setup
    private void setupClickListeners() {

        // click listener to get all employees and display them
        binding.totalEmployeesCard.setOnClickListener(v -> fetchAndShowEmployees());

        binding.addEmployeeBtn.setOnClickListener(v -> { // form to add new employee to the comp2000-api database
            Log.d(TAG, "Add employee button clicked");
            showAddEmployeeDialog();
        });

        binding.checkIncrementsBtn.setOnClickListener(v -> {
            Log.d(TAG, "Checking salary increments...");
            showSalaryIncrementStatus();
        });

        // logout button functionality
        binding.logoutBtn.setOnClickListener(v -> {
            Log.d(TAG, "processing admin logout...");
            try {
                // clear admin session
                dbHelper.logoutAdmin();

                // create navigation options to prevent back navigation
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.SecondFragment, true)
                        .build();

                // navigate back to login fragment
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_AdminDashboardFragment_to_SecondFragment,
                                null,
                                navOptions);

                // show success message
                Toast.makeText(requireContext(),
                        "Logged out successfully",
                        Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "Logout failed", e);
                Toast.makeText(requireContext(),
                        "Logout failed; please try again",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void toggleEmployeeList() {
        isEmployeeListExpanded = !isEmployeeListExpanded;
        // save state to SharedPreferences whenever it changes
        sharedPreferences.edit()
                .putBoolean(EMPLOYEE_LIST_EXPANDED_KEY, isEmployeeListExpanded)
                .apply();
        updateEmployeeListVisibility();
    }

    private void updateEmployeeListVisibility() {
        if (isEmployeeListExpanded) {
            // expand animation
            employeeListContent.setVisibility(View.VISIBLE);
            employeeListContent.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
            collapseButton.animate()
                    .rotation(0f)
                    .setDuration(200)
                    .start();
        } else {
            // collapse animation
            employeeListContent.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() ->
                            employeeListContent.setVisibility(View.GONE))
                    .start();
            collapseButton.animate()
                    .rotation(180f)
                    .setDuration(200)
                    .start();
        }
    }
    //----------------------------------------------------------------------------------------------
    // Salary increment functions
    private void showIncrementDialog(String message, int eligibleCount) {
        // create and show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.admin_salary_increment_dialog, null);
        TextView statusText = dialogView.findViewById(R.id.salary_status_text);
        MaterialButton processButton = dialogView.findViewById(R.id.process_increments_button);

        // set the text and enable/disable process button based on eligible employees
        statusText.setText(message);
        processButton.setEnabled(eligibleCount > 0);

        AlertDialog dialog = builder.setTitle("Salary Increment Status")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        // Handle process button click
        processButton.setOnClickListener(v -> {
            // TODO [ ]: Implement the actual salary increment processing
            Snackbar.make(binding.getRoot(),
                    "Processed salary increments for " + eligibleCount + " employee(s)",
                    Snackbar.LENGTH_LONG).show();
            dialog.dismiss();
        });

        dialog.show();
    }
    
    private void showSalaryIncrementStatus() { // show salary increment status for all employees with showIncrementDialog()
        ApiDataService.getIncrementStatus(new ApiDataService.IncrementStatusListener() {
            @Override
            public void onSuccess(List<ApiDataService.IncrementStatus> statusList) {
                StringBuilder messageBuilder = new StringBuilder();
                final int[] eligibleCount = {0};

                for (ApiDataService.IncrementStatus status : statusList) { // iterate through each employee
                    int daysUntilIncrement = 365 - (int)status.daysSince;
                    String formattedSalary = String.format("£%.2f", status.salary);

                    if (daysUntilIncrement <= 0) { // if increment is due
                        messageBuilder.append("✓ ").append(status.name)
                                .append(" - Due for 5% increase (Current: ")
                                .append(formattedSalary).append(")\n\n");
                        eligibleCount[0]++;
                    } else if (daysUntilIncrement <= 30) { // if increment is due in 30 days
                        messageBuilder.append("⏰ ").append(status.name)
                                .append(" - Due in ").append(daysUntilIncrement)
                                .append(" days (Current: ").append(formattedSalary).append(")\n\n");
                    } else { // if increment is due in more than 30 days
                        messageBuilder.append("• ").append(status.name)
                                .append(" - Due in ").append(daysUntilIncrement)
                                .append(" days (Current: ").append(formattedSalary).append(")\n\n");
                    }
                }

                showIncrementDialog(messageBuilder.toString(), eligibleCount[0]); // show dialog with message and count
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
   //----------------------------------------------------------------------------------------------
    // save toggled state
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) { // save collapsed state
        super.onSaveInstanceState(outState);
        outState.putBoolean("isEmployeeListExpanded", isEmployeeListExpanded);
    }
    // clean up binding -------------------------------------------------------------------------------
    @Override // freeze resources when view's destroyed
    public void onDestroyView() { // clean up binding
        super.onDestroyView();
        binding = null;
    }}

