package com.example.staffsyncapp;

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

import com.example.staffsyncapp.databinding.EmployeeProfileFragmentBinding;
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NavigationManager;

import java.util.List;
import java.util.Locale;


/** TODO
* [ ] implement functionality to edit own details
* [ ] test that the details change by looking on adminDashboard fragment
**/

public class EmployeeProfileFragment extends Fragment {
    private EmployeeProfileFragmentBinding binding;
    private ApiDataService apiService;
    private Employee currentEmployee;
    NavigationManager navigationManager;

    private EmployeeAdapter.EmployeeViewModel employeeViewModel;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = EmployeeProfileFragmentBinding.inflate(inflater, container, false);
        apiService = new ApiDataService(requireContext());
        employeeViewModel = new EmployeeAdapter.EmployeeViewModel();
        return binding.getRoot();
    }

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
        // set initial UI state
        binding.employeeId.setText("Loading...");
        binding.currentName.setText("Loading...");
        binding.currentEmail.setText("Loading...");
        binding.currentSalary.setText("Loading...");
    }

    private void setupValidation() {
        // email validation
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

    private boolean validateEmail() { // validate email format
        String email = binding.editEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() && !email.isEmpty()) {
            binding.emailFormatError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.emailFormatError.setVisibility(View.GONE);
        return true;
    }

    private boolean validateEmailMatch() { // validate email match
        String email = binding.editEmail.getText().toString().trim();
        String confirmEmail = binding.confirmEditEmail.getText().toString().trim();
        if (!email.equals(confirmEmail) && !confirmEmail.isEmpty()) {
            binding.emailMatchError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.emailMatchError.setVisibility(View.GONE);
        return true;
    }

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
                            "employee_id = ?",
                            new String[]{String.valueOf(employeeId)},
                            null, null, null
                    );

                    if (cursor != null && cursor.moveToFirst()) {
                        // if find data in local DB, use it
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
                                firstName.toLowerCase() + "." + lastName.toLowerCase() + "@company.com",
                                department,
                                salary,
                                "2023-01-01"
                        );
                        updateUIWithEmployeeData(currentEmployee);
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

    private void updateUIWithEmployeeData(Employee employee) {
        binding.employeeId.setText(String.valueOf(employee.getId()));
        binding.currentName.setText(employee.getFirstname() + " " + employee.getLastname());
        binding.currentEmail.setText(employee.getEmail());
        binding.currentSalary.setText(String.format(Locale.getDefault(), "£%.2f", employee.getSalary()));
    }

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

    private boolean validateInput() { // validate employee input
        String newName = binding.editName.getText().toString().trim();
        if (newName.isEmpty()) {
            binding.nameFormatError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.nameFormatError.setVisibility(View.GONE);

        return validateEmail() && validateEmailMatch();
    }

    private void updateEmployeeDetails() { // update employee details with JSON employee fields
        String newName = binding.editName.getText().toString().trim();
        String newEmail = binding.editEmail.getText().toString().trim();

        String[] names = newName.split(" ", 2);
        String firstName = names[0];
        String lastName = names.length > 1 ? names[1] : "";

        apiService.updateEmployee(
                currentEmployee.getId(),
                firstName,
                lastName,
                newEmail,
                currentEmployee.getDepartment(),
                currentEmployee.getSalary(),
                currentEmployee.getJoiningdate(),
                new ApiDataService.EmployeeUpdateListener() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}