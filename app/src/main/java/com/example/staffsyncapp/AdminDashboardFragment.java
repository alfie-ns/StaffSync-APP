package com.example.staffsyncapp;

// Android libraries for UI, data handling, and permissions
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
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
        - [x] AdminDashboardFragmentBinding
        - [ ] incorporate DatabaseHelper db
        - [x] logout functionality
        - [ ] notifications 1: get notifications alerting
        - [x] fetch ALL employees from comp2000
        - [x] list employees searched for; filteredList
        - [ ] EmployeeList2: when an id is searched for, allow to remove the id and thus the app stops searching for it
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

    // Employee search functionality variables
    private EditText searchEmployeeInput;

    private com.example.staffsyncapp.EmployeeAdapter employeeAdapter;

    // employee list collapse functionality variables
    private boolean isEmployeeListExpanded = true;
    private ImageButton collapseButton;
    private View employeeListContent;
    private static final String EMPLOYEE_LIST_EXPANDED_KEY = "employee_list_expanded"; // toggle-state key

    // preferences
    private SharedPreferences sharedPreferences;
    
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
    private void fetchAndShowEmployees() {
        ApiDataService.getAllEmployees(new ApiDataService.EmployeeFetchListener() {
            @Override
            public void onEmployeesFetched(List<Employee> employees) {
                if (getContext() == null) return; 

                // hide loading indicator
                binding.progressBar.setVisibility(View.GONE);

                if (employees != null && !employees.isEmpty()) {
                    // 1- create and set adapter with employee data
                    employeeAdapter = new com.example.staffsyncapp.EmployeeAdapter(employees);
                    binding.recyclerViewEmployees.setAdapter(employeeAdapter);

                    // 2- show list and update employee count
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

    private void setupSearchFunctionality() {
        EditText searchInput = binding.searchEmployee; // 1- bind to respective .xml elements (search box and checkbox)
        CheckBox searchByIdCheckbox = binding.searchByIdCheckbox;
        // 2- set up text change listener
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {} // required empty method for TextWatcher interface

            @Override // 3- define and execute search functionality
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().trim();

                if (searchByIdCheckbox.isChecked()) {
                    // 4- search by ID only if text is not empty
                    if (!searchText.isEmpty()) {
                        try { // try to parse the input as an integer(employee ID)
                            int employeeId = Integer.parseInt(searchText);
                            searchEmployeeById(employeeId);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(),
                                    "Please enter a valid ID number",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    // search all employees by name, email, or department
                    if (employeeAdapter != null) {
                        employeeAdapter.filter(searchText);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

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

    private boolean validateInputs(EditText... inputs) {
        // 1- basic empty check for each field
        for (EditText input : inputs) {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
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

    private void searchEmployeeById(int id) {
        // Show loading indicator
        binding.progressBar.setVisibility(View.VISIBLE);

        // Create interface to handle the response
        ApiDataService.EmployeeFetchListener listener = new ApiDataService.EmployeeFetchListener() {
            @Override
            public void onEmployeesFetched(List<Employee> employees) {
                //if (getContext() == null) return;

                binding.progressBar.setVisibility(View.GONE);
                if (employees != null && !employees.isEmpty()) {
                    employeeAdapter = new com.example.staffsyncapp.EmployeeAdapter(employees);
                    binding.recyclerViewEmployees.setAdapter(employeeAdapter);
                    binding.totalEmployeesCount.setText(String.valueOf(employees.size()));
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

    private void setupClickListeners() {
        // click listener to get all employees and display them
        binding.totalEmployeesCard.setOnClickListener(v -> fetchAndShowEmployees());

        binding.addEmployeeBtn.setOnClickListener(v -> { // form to add new employee to the comp2000-api database
            Log.d(TAG, "Add employee button clicked");
            showAddEmployeeDialog();
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

