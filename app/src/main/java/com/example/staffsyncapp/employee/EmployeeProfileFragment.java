package com.example.staffsyncapp.employee;

import static android.content.ContentValues.TAG;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.database.Cursor;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.staffsyncapp.adapter.EmployeeAdapter;
import com.example.staffsyncapp.api.ApiDataService;
import com.example.staffsyncapp.R;
import com.example.staffsyncapp.databinding.EmployeeProfileFragmentBinding;
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NavigationManager;
import com.example.staffsyncapp.api.ApiWorkerThread;

import java.util.List;
import java.util.Locale;

// Employee profile fragment class with methods to update employee details
public class EmployeeProfileFragment extends Fragment {
    private EmployeeProfileFragmentBinding binding;
    private ApiDataService apiService;
    private Employee currentEmployee;
    NavigationManager navigationManager;

    private ApiWorkerThread workerThread;

    private EmployeeAdapter.EmployeeViewModel employeeViewModel;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = EmployeeProfileFragmentBinding.inflate(inflater, container, false);
        apiService = new ApiDataService(requireContext());
        employeeViewModel = new EmployeeAdapter.EmployeeViewModel();

        this.workerThread = new ApiWorkerThread();
        workerThread.start();
        return binding.getRoot();
    }

    // Initialise UI and load employee data
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navigationManager = new NavigationManager(this, binding.bottomNavigation);
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_profile);

        setupValidation();
        setupUI();
        loadEmployeeData();
        setupClickListeners();
    }

    // Update UI with employee data continuously
    @Override
    public void onResume() {
        super.onResume();
        Employee employee = employeeViewModel.getEmployeeLiveData().getValue();
        if (employee != null) {
            updateUIWithEmployeeData(employee);
        } else {
            loadEmployeeData();
        }
    }

    private void setupUI() {
        // set initial UI state; show current values as hints
        // null check to prevent NPE
        if (currentEmployee != null) {
            binding.currentEmail.setText(currentEmployee.getEmail());
            binding.editEmail.setHint(currentEmployee.getEmail());
            binding.currentName.setText(currentEmployee.getFirstname() + " " + currentEmployee.getLastname());
            binding.editName.setHint(currentEmployee.getFirstname() + " " + currentEmployee.getLastname());
        }
    }

    // Validate email format
    private void setupValidation() {
        // email validation: the unused parameters are necessary for the method signature(previously explained somewhere)
        binding.editEmail.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }

            public void afterTextChanged(Editable s) {}
        });

        // confirm email validation
        binding.confirmEditEmail.addTextChangedListener(new TextWatcher() {

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}


            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmailMatch();
            }


            public void afterTextChanged(Editable s) {}
        });
    }

    // Validate email format
    private boolean validateEmail() { 
        String email = binding.editEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() && !email.isEmpty()) { // if email doesn't match pattern
            binding.emailFormatError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.emailFormatError.setVisibility(View.GONE);
        return true;
    }

    // Validate email match
    private boolean validateEmailMatch() { 
        String email = binding.editEmail.getText().toString().trim();
        String confirmEmail = binding.confirmEditEmail.getText().toString().trim();
        if (!email.equals(confirmEmail) && !confirmEmail.isEmpty()) {
            binding.emailMatchError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.emailMatchError.setVisibility(View.GONE);
        return true;
    }

    // Load employee data from API or local DB
    private void loadEmployeeData() {
        LocalDataService dbHelper = new LocalDataService(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int employeeId = prefs.getInt("logged_in_employee_id", -1);

        if (employeeId != -1) {
            // Try API first
            apiService.getEmployeeById(employeeId, new ApiDataService.EmployeeFetchListener() {
                @Override
                public void onEmployeesFetched(List<Employee> employees) {
                    if (employees != null && !employees.isEmpty()) {
                        currentEmployee = employees.get(0);
                        updateUIWithEmployeeData(currentEmployee);

                        // Store API data in local DB
                        ContentValues values = new ContentValues();
                        values.put("employee_id", employeeId);
                        values.put("full_name", currentEmployee.getFirstname() + " " + currentEmployee.getLastname());
                        values.put("department", currentEmployee.getDepartment());
                        values.put("salary", currentEmployee.getSalary());

                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.insertWithOnConflict("employee_details", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                        db.close();
                    }
                }

                @Override
                public void onError(String error) {
                    // On API error, use local DB
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    Cursor cursor = db.query(
                            "employee_details",
                            null,
                            "id = ?",
                            new String[]{String.valueOf(employeeId)},
                            null, null, null
                    );

                    if (cursor != null && cursor.moveToFirst()) {
                        // If find data in local DB, use it
                        String fullName = cursor.getString(cursor.getColumnIndex("full_name"));
                        String[] names = fullName.split(" ");
                        String firstName = names[0];
                        String lastName = names.length > 1 ? names[1] : "";
                        String department = cursor.getString(cursor.getColumnIndex("department"));
                        double salary = cursor.getDouble(cursor.getColumnIndex("salary"));

                        currentEmployee = new Employee(
                                employeeId,
                                firstName,
                                lastName,
                                firstName.toLowerCase() + "." + lastName.toLowerCase() + "@staffsync.com",
                                department,
                                salary,
                                "2023-01-01"
                        );
                        updateUIWithEmployeeData(currentEmployee); // Update UI with local DB data
                        cursor.close();
                    } else {
                        // if no data in local DB either
                        if (getContext() != null) {
                            Toast.makeText(requireContext(),
                                    "Could not load employee data from API or local storage",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    db.close();
                }
            });
        }
    }

    // Update UI with employee data
    private void updateUIWithEmployeeData(Employee employee) {
        if (binding == null || employee == null) return;
        // ^ ensure both aren't null to prevent crash
        binding.employeeId.setText(String.valueOf(employee.getId()));
        binding.currentName.setText(employee.getFirstname() + " " + employee.getLastname());
        binding.currentEmail.setText(employee.getEmail());
        binding.currentSalary.setText(String.format(Locale.getDefault(), "£%.2f", employee.getSalary()));
    }

    // Setup click listeners for save and back buttons
    private void setupClickListeners() {
        binding.saveProfile.setOnClickListener(v -> {
            if (validateInput()) {
                updateEmployeeDetails();
            }
        });

        binding.backArrow.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });
    }

    // Validate employee input
    private boolean validateInput() {
        String newName = binding.editName.getText().toString().trim();
        if (newName.isEmpty()) {
            binding.nameFormatError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.nameFormatError.setVisibility(View.GONE);

        return validateEmail() && validateEmailMatch();
    }

    /**
     * Update employee details in API and local DB
     * - Update employee name and email
     * - if invalid input kill method
     * - if valid input, proceed with using workerThread to queue a task to update employee details in API and local DB
     * - call apiService.updateEmployee() to update employee details in API
     * - if API change successful, update local DB with new data
     * @return void
     */
    private void updateEmployeeDetails() {
        String newName = binding.editName.getText().toString().trim();
        String newEmail = binding.editEmail.getText().toString().trim();

        if (!validateInput()) {
            return;
        }

        String[] names = newName.split(" ", 2);
        String firstName = names[0];
        String lastName = names.length > 1 ? names[1] : "";

        workerThread.queueTask (() -> { // workerThread profile customisation
            apiService.updateEmployee(
                    currentEmployee.getId(),
                    firstName,
                    lastName,
                    newEmail,
                    currentEmployee.getDepartment(), // get existing
                    currentEmployee.getSalary(), // get existing
                    currentEmployee.getJoiningDate(), // get existing
                    // build off existing data
                    new ApiDataService.EmployeeUpdateListener() {
                        @Override
                        public void onSuccess(String message) {
                            workerThread.postToMainThread(() -> {
                                LocalDataService dbHelper = new LocalDataService(requireContext());
                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                db.beginTransaction();
                                try {
                                    // Update employee details
                                    ContentValues values = new ContentValues();
                                    values.put("full_name", newName);
                                    db.update("employee_details", values,
                                            "employee_id = ?",
                                            new String[]{String.valueOf(currentEmployee.getId())});

                                    // Update login email
                                    ContentValues loginValues = new ContentValues();
                                    loginValues.put("email", newEmail);
                                    int rows = db.update("employees", loginValues,
                                            "employee_id = ?",
                                            new String[]{String.valueOf(currentEmployee.getId())});
                                    Log.d("ProfileUpdate", "Updated " + rows + " rows in employees table");

                                    db.setTransactionSuccessful();
                                } finally {
                                    db.endTransaction();
                                }

                                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                loadEmployeeData();
                                Navigation.findNavController(requireView()).navigateUp();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            workerThread.postToMainThread(() -> {
                                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
            );
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (workerThread != null) { // workerThread shutdown, clear binding
            workerThread.shutdown();
        }
        binding = null;
    }
}