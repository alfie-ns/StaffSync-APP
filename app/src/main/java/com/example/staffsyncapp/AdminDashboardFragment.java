package com.example.staffsyncapp;

import static com.example.staffsyncapp.utils.SalaryIncrementManager.calculateDaysSince;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.example.staffsyncapp.utils.NotificationService;
import com.example.staffsyncapp.utils.SalaryIncrementManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.staffsyncapp.utils.SalaryIncrementManager;
import java.util.ArrayList;

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
        - [ ] login to respective account based on employee id


    */

    // Constants
    private static final String TAG = "AdminDashboard";
    private static final String PREFS_NAME = "AdminSettings";

    // Core variables
    private AdminDashboardFragmentBinding binding;
    private LocalDataService dbHelper;
    private ApiDataService employeeDataService;
    private ProgressBar progressBar;
    private int totalEmployeeCount = 0; // pre-initialise to ensure safe state before first API response

    // Employee search functionality variables
    private EditText searchEmployeeInput;

    private EmployeeAdapter adminEmployeeAdapter;

    // Employee list collapse functionality variables
    private boolean isEmployeeListExpanded = true;
    private ImageButton collapseButton;
    private View employeeListContent;
    private static final String EMPLOYEE_LIST_EXPANDED_KEY = "employee_list_expanded"; // toggle-state key

    private static final String HOLIDAY_NOTIFICATIONS_KEY = "holiday_notifications";
    private static final String EMAIL_NOTIFICATIONS_KEY = "email_notifications";
    private NotificationService notificationService;

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

        notificationService = new NotificationService(requireContext()); // setup notification service

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) { // check for notification permissions
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }

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
        if (adminEmployeeAdapter != null) {
            adminEmployeeAdapter.filter(searchText);
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
        adminEmployeeAdapter = new EmployeeAdapter(employees);

        // Set up the delete listener; 
        adminEmployeeAdapter.setOnEmployeeDeleteListener(employee -> {
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

        adminEmployeeAdapter.setOnEmployeeUpdateListener(employee -> {
            showUpdateDialog(requireContext(), employee);
        });

        binding.recyclerViewEmployees.setAdapter(adminEmployeeAdapter);
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
                    if (adminEmployeeAdapter != null) {
                        adminEmployeeAdapter.updateDisplayList(employees); // update existing adapter
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
            if (validateInputs(firstNameInput, lastNameInput, emailInput,
                    departmentInput, salaryInput, joiningDateInput)) {

                double salary = Double.parseDouble(salaryInput.getText().toString());
                String email = emailInput.getText().toString().trim();

                employeeDataService.addEmployee(
                        firstNameInput.getText().toString(),
                        lastNameInput.getText().toString(),
                        email,
                        departmentInput.getText().toString(),
                        salary,
                        joiningDateInput.getText().toString(),
                        new ApiDataService.EmployeeAddListener() {
                            @Override
                            public void onSuccess(String message, int employeeId, String email) {
                                LocalDataService dbHelper = new LocalDataService(requireContext());
                                dbHelper.createEmployeeAccount(employeeId, email);

                                dialog.dismiss();
                                Toast.makeText(requireContext(),
                                        "Employee added successfully",
                                        Toast.LENGTH_SHORT).show();
                                fetchAndShowEmployees();
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

    private void showUpdateDialog(Context context, Employee employee) {
        Log.d(TAG, "Original date from employee: " + employee.getJoiningdate());
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = getLayoutInflater().inflate(R.layout.admin_update_employee_dialog, null);

        // get input fields
        EditText firstNameInput = dialogView.findViewById(R.id.firstNameInput);
        EditText lastNameInput = dialogView.findViewById(R.id.lastNameInput);
        EditText emailInput = dialogView.findViewById(R.id.emailInput);
        EditText departmentInput = dialogView.findViewById(R.id.departmentInput);
        EditText salaryInput = dialogView.findViewById(R.id.salaryInput);
        EditText joiningDateInput = dialogView.findViewById(R.id.joiningDateInput);

        // pre-populate fields with current values
        firstNameInput.setText(employee.getFirstname());
        lastNameInput.setText(employee.getLastname());
        emailInput.setText(employee.getEmail());
        departmentInput.setText(employee.getDepartment());
        salaryInput.setText(String.valueOf(employee.getSalary()));
        joiningDateInput.setText(employee.getJoiningdate());
        Log.d(TAG, "Date after EditText: " + joiningDateInput.getText().toString());
        AlertDialog dialog = builder.setView(dialogView).create();

        dialogView.findViewById(R.id.updateButton).setOnClickListener(v -> {
            // get current values and keep original if unchanged
            String newFirstName = firstNameInput.getText().toString().trim();
            String newLastName = lastNameInput.getText().toString().trim();
            String newEmail = emailInput.getText().toString().trim();
            String newDepartment = departmentInput.getText().toString().trim();
            String newJoiningDate = joiningDateInput.getText().toString().trim();

            // keep original value if empty or unchanged
            if (newFirstName.isEmpty()) newFirstName = employee.getFirstname();
            if (newLastName.isEmpty()) newLastName = employee.getLastname();
            if (newEmail.isEmpty()) newEmail = employee.getEmail();
            if (newDepartment.isEmpty()) newDepartment = employee.getDepartment();
            if (newJoiningDate.isEmpty()) newJoiningDate = employee.getJoiningdate();

            // parse salary, keep original if empty or invalid
            double newSalary = employee.getSalary();
            String salaryStr = salaryInput.getText().toString().trim();
            if (!salaryStr.isEmpty()) {
                try {
                    newSalary = Double.parseDouble(salaryStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Please enter a valid salary", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            employeeDataService.updateEmployee(
                    employee.getId(),
                    newFirstName,
                    newLastName,
                    newEmail,
                    newDepartment,
                    newSalary,
                    newJoiningDate,
                    new ApiDataService.EmployeeUpdateListener() {
                        @Override
                        public void onSuccess(String message) {
                            dialog.dismiss();
                            fetchAndShowEmployees();
                            Toast.makeText(context, "Employee updated successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(context, "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        dialog.show();
    }

    private void showEditEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.admin_edit_employee_id_dialog, null);
        builder.setView(dialogView);

        // get reference to input field
        EditText employeeIdInput = dialogView.findViewById(R.id.employeeIdInput);

        AlertDialog dialog = builder.create();

        // handle find button click
        dialogView.findViewById(R.id.findButton).setOnClickListener(v -> {
            String idText = employeeIdInput.getText().toString().trim();
            if (!idText.isEmpty()) {
                try {
                    int employeeId = Integer.parseInt(idText);
                    employeeDataService.getEmployeeById(
                            employeeId,
                            new ApiDataService.EmployeeFetchListener() {
                                @Override
                                public void onEmployeesFetched(List<Employee> employees) {
                                    if (employees != null && !employees.isEmpty()) {
                                        dialog.dismiss();
                                        showUpdateDialog(requireContext(), employees.get(0));
                                    } else {
                                        employeeIdInput.setError("Employee not found");
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(requireContext(),
                                            "Error: " + error,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                } catch (NumberFormatException e) {
                    employeeIdInput.setError("Please enter a valid ID");
                }
            } else {
                employeeIdInput.setError("ID is required");
            }
        });

        dialog.show();
    }

    private void showDeleteEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.admin_delete_employee_dialog, null);
        builder.setView(dialogView);

        // get reference to input field
        EditText employeeIdInput = dialogView.findViewById(R.id.employeeIdInput);

        AlertDialog dialog = builder.create();

        // set positive and negative buttons after creating dialog
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Delete", (dialogInterface, i) -> {
            String idText = employeeIdInput.getText().toString().trim();
            if (!idText.isEmpty()) {
                try {
                    int employeeId = Integer.parseInt(idText);
                    employeeDataService.deleteEmployee(
                            employeeId,
                            new ApiDataService.EmployeeDeleteListener() {
                                @Override
                                public void onSuccess(String message) {
                                    dialog.dismiss();
                                    fetchAndShowEmployees();  // Refresh list
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
                } catch (NumberFormatException e) {
                    employeeIdInput.setError("Please enter a valid ID");
                }
            } else {
                employeeIdInput.setError("ID is required");
            }
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> dialog.dismiss());

        // add dialog title
        dialog.setTitle("Delete Employee");

        dialog.show();
    }
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
        /**
         * Set all click listeners for each admin functionality
         **/

        // go to holiday requests fragment
        binding.manageLeaveBtn.setOnClickListener(v -> {
            Navigation.findNavController(v)
                    .navigate(R.id.action_AdminDashboardFragment_to_AdminHolidayRequestsFragment);
        });

        // edit employee dialog via pencil icon
        binding.pencilIcon.setOnClickListener(v -> {
            showEditEmployeeDialog();
        });

        // refresh employee list
        binding.refreshEmployeesBtn.setOnClickListener(v -> {
            Log.d(TAG, "refresh employees list requested");
            binding.progressBar.setVisibility(View.VISIBLE);  // show loading whilst fetching
            fetchAndShowEmployees(); // effectively refreshes/synchronises the RecyclerView with the API data
        });

        // etch and display all employees
        binding.totalEmployeesCard.setOnClickListener(v -> fetchAndShowEmployees());

        // add new employee dialog
        binding.addEmployeeBtn.setOnClickListener(v -> { // form to add new employee to the comp2000-api database
            Log.d(TAG, "add employee button clicked...");
            showAddEmployeeDialog();
        });

        // delete employee dialog
        binding.deleteEmployeeBtn.setOnClickListener(v -> {
            Log.d(TAG, "delete employee button clicked...");
            showDeleteEmployeeDialog();
        });

        // check salary increments
        binding.checkIncrementsBtn.setOnClickListener(v -> {
            Log.d(TAG, "checking salary increments...");
            SalaryIncrementManager.showSalaryIncrementStatus();
        });

        // notification controls setup
        binding.notificationSwitch.setChecked(
                sharedPreferences.getBoolean(HOLIDAY_NOTIFICATIONS_KEY, true));

        binding.emailSwitch.setChecked(
                sharedPreferences.getBoolean(EMAIL_NOTIFICATIONS_KEY, true));

        // holiday notifications toggle
        binding.notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                sharedPreferences.edit()
                        .putBoolean(HOLIDAY_NOTIFICATIONS_KEY, isChecked)
                        .apply();

                if (isChecked) {
                    notificationService.sendHolidayNotification(
                            "holiday notifications enabled",
                            "you will now receive holiday request notifications"
                    );
                }
                Log.d(TAG, "holiday notifications " + (isChecked ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.e(TAG, "failed to save holiday notification setting", e);
            }
        });

        // email notifications toggle
        binding.emailSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                sharedPreferences.edit()
                        .putBoolean(EMAIL_NOTIFICATIONS_KEY, isChecked)
                        .apply();

                if (isChecked) {
                    notificationService.sendSystemNotification(
                            "email notifications enabled",
                            "you will now receive email notifications"
                    );
                }
                Log.d(TAG, "email notifications " + (isChecked ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.e(TAG, "failed to save email notification setting", e);
            }
        });

        // logout functionality
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
                        "logged out successfully",
                        Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "logout failed", e);
                Toast.makeText(requireContext(),
                        "logout failed; please try again",
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

    public static void getIncrementStatus(SalaryIncrementManager.IncrementStatusListener listener) {
        ApiDataService.getAllEmployees(new ApiDataService.EmployeeFetchListener() {
            @Override
            public void onEmployeesFetched(List<Employee> employees) {
                try {
                    List<SalaryIncrementManager.IncrementStatus> statusList = new ArrayList<>();
                    for (Employee emp : employees) {
                        String name = emp.getFirstname() + " " + emp.getLastname();
                        double salary = emp.getSalary();
                        long daysSince = calculateDaysSince(emp.getJoiningdate());

                        statusList.add(new SalaryIncrementManager.IncrementStatus(name.trim(), salary, daysSince));
                    }
                    listener.onSuccess(statusList);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing increments: " + e.getMessage());
                    listener.onError("Failed to process increments");
                }
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
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
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (employeeDataService != null) {
            employeeDataService.cleanup();  // ensure all worker threads are terminated to prevent memory leak
            Log.d(TAG, "Worker thread cleaned up");
        }
        binding = null;
    }
}

