package com.example.staffsyncapp.employee;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.staffsyncapp.adapter.EmployeeAdapter;
import com.example.staffsyncapp.api.ApiDataService;
import com.example.staffsyncapp.R;
import com.example.staffsyncapp.databinding.EmployeeMainBodyFragmentBinding;
import com.example.staffsyncapp.leave.LeaveHistoryDialog;
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NavigationManager;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.Period;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import android.content.ContentValues;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * TODO
 *  [X] book leave
 *  [X] pending requests
 *  [X] view leave history
 */
public class EmployeeMainBodyFragment extends Fragment {
    private EmployeeMainBodyFragmentBinding binding;
    private ApiDataService apiService;
    private Employee currentEmployee;
    private NavigationManager navigationManager;

    private Handler syncHandler = new Handler(Looper.getMainLooper());
    private static final long SYNC_INTERVAL = 60 * 1000; // refresh every 1 minute; in milliseconds
    private Runnable syncRunnable;

    private LocalDataService dbHelper;
    private EmployeeAdapter.EmployeeViewModel employeeViewModel;

    // TODO [X]: get employee ID from local database
    //LocalDataService dbHelper = new LocalDataService(requireContext());
    //int employeeId = dbHelper.getLoggedInEmployeeId();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = EmployeeMainBodyFragmentBinding.inflate(inflater, container, false);
        apiService = new ApiDataService(requireContext());
        dbHelper = new LocalDataService(requireContext());
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
        setupUI(); // refresh UI to show updated leave balance
        Employee employee = employeeViewModel.getEmployeeLiveData().getValue();
        if (employee != null) {
            updateUIWithEmployeeData(employee);
        } else {
            loadEmployeeData();
        }
    }

    private void setupUI() {
        SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int employeeId = prefs.getInt("logged_in_employee_id", -1);

        if (employeeId != -1) {
            int remainingDays = dbHelper.getRemainingLeaveDays(employeeId);
            binding.daysRemaining.setText(String.format("Days Remaining: %d/30", remainingDays));
        } else {
            binding.daysRemaining.setText("Days Remaining: --/30");
        }
        binding.employeeId.setText("Loading...");
        binding.department.setText("Loading...");
        binding.yearsOfService.setText("Loading...");
        binding.nextReview.setText("Loading...");
        binding.notificationStatus.setText("Notifications: Enabled");
    }

    private void loadEmployeeData() {
        LocalDataService dbHelper = new LocalDataService(requireContext());
        SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int employeeId = prefs.getInt("logged_in_employee_id", -1);

        // First load from local DB
        loadFromLocalDb(dbHelper, employeeId);

        // Setup periodic API sync
        setupPeriodicSync(dbHelper, employeeId);
    }


    private void loadFromLocalDb(LocalDataService dbHelper, int employeeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM employee_details WHERE employee_id = ?",
                new String[]{String.valueOf(employeeId)});

        if (cursor != null && cursor.moveToFirst()) {
            try {
                String fullName = cursor.getString(cursor.getColumnIndex("full_name"));
                String[] names = fullName.split(" ");
                String firstName = names[0];
                String lastName = names.length > 1 ? names[1] : "";
                String department = cursor.getString(cursor.getColumnIndex("department"));
                double salary = cursor.getDouble(cursor.getColumnIndex("salary"));

                // Get and log the hire date; debugging
                String hireDate = cursor.getString(cursor.getColumnIndex("hire_date"));
                Log.d("EmployeeData", "Raw hire date from DB: " + hireDate);

                currentEmployee = new Employee(
                        employeeId,
                        firstName,
                        lastName,
                        firstName.toLowerCase() + "." + lastName.toLowerCase() + "@staffsync.com",
                        department,
                        salary,
                        hireDate
                );
                updateUIWithEmployeeData(currentEmployee);
            } catch (Exception e) {
                Log.e("EmployeeData", "Error loading employee data", e);
            }
            cursor.close();
        }
        db.close();
    }

    private void setupPeriodicSync(LocalDataService dbHelper, int employeeId) {
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                apiService.getEmployeeById(employeeId, new ApiDataService.EmployeeFetchListener() {
                    @Override
                    public void onEmployeesFetched(List<Employee> employees) {
                        if (employees != null && !employees.isEmpty()) {
                            Employee apiEmployee = employees.get(0);
                            currentEmployee = apiEmployee;

                            // parses the GMT date string from API to a proper date format
                            String apiDate = apiEmployee.getJoiningDate(); // "Thu, 04 Mar 2021 00:00:00 GMT" -> "2021-03-04"
                            try {
                                SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.UK);
                                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                                Date date = inputFormat.parse(apiDate);
                                String formattedDate = outputFormat.format(date);

                                // update local DB with properly formatted date
                                ContentValues values = new ContentValues();
                                values.put("employee_id", employeeId);
                                values.put("full_name", apiEmployee.getFirstname() + " " + apiEmployee.getLastname());
                                values.put("department", apiEmployee.getDepartment());
                                values.put("salary", apiEmployee.getSalary());
                                values.put("hire_date", formattedDate); // Store the parsed date

                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                db.insertWithOnConflict("employee_details", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                                db.close();

                                // update UI with the employee data
                                if(isAdded()) {
                                    requireActivity().runOnUiThread(() -> {
                                        updateUIWithEmployeeData(currentEmployee);
                                    });
                                }
                            } catch (ParseException e) {
                                Log.e("EmployeeSync", "Error parsing date from API", e);
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("EmployeeSync", "API sync failed: " + error);
                    }
                });

                syncHandler.postDelayed(this, SYNC_INTERVAL);
            }
        };

        // start periodic sync
        syncHandler.post(syncRunnable);
    }

    private void updateUIWithEmployeeData(Employee employee) {
        if (binding == null || employee == null) return; // null check to prevent crash

        binding.employeeId.setText("My Employee ID: #" + employee.getId());
        binding.department.setText("Department: " + employee.getDepartment());

        try {
            String hireDateStr = employee.getJoiningDate();
            LocalDate hireDate;
            LocalDate now = LocalDate.now();

            if (hireDateStr == null || hireDateStr.isEmpty()) {
                Log.d("EmployeeData", "No hire date found, using default");
                hireDate = LocalDate.of(2021, 3, 4);
            } else {
                try {
                    // Try parse API format
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.UK);
                    hireDate = LocalDate.parse(hireDateStr, formatter);
                } catch (Exception e) {
                    // if fail try DB format
                    hireDate = LocalDate.parse(hireDateStr);
                }
            }

            Period serviceTime = Period.between(hireDate, now);

            Log.d("EmployeeData", "Hire date: " + hireDate);
            Log.d("EmployeeData", "Service time: " + serviceTime.getYears() + "y " + serviceTime.getMonths() + "m");

            binding.yearsOfService.setText("Years of Service: " +
                    serviceTime.getYears() + " years, " + serviceTime.getMonths() + " months");

            // Calculate next review
            LocalDate nextReview = hireDate.withYear(now.getYear());
            if (nextReview.isBefore(now)) {
                nextReview = nextReview.plusYears(1);
            }

            Log.d("DateCheck", String.format( // PROOF LOGGING
                    "Now: %s\nHire: %s\nNext: %s",
                    now.toString(),
                    hireDate.toString(),
                    nextReview.toString()
            ));

            long monthsToReview = ChronoUnit.MONTHS.between(now, nextReview);
            binding.nextReview.setText("Next Salary Review: " + monthsToReview + " months");

        } catch (Exception e) {
            Log.e("EmployeeData", "Error calculating dates", e);
            binding.yearsOfService.setText("Years of Service: Not available");
            binding.nextReview.setText("Next Salary Review: Not available");
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled_" + employee.getId(), true);
        binding.notificationStatus.setText("Notifications: " + (notificationsEnabled ? "Enabled" : "Disabled"));
    }

    private void setupClickListeners() {
        // Settings
        binding.employeeSettingsIcon.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_EmployeeMainFragment_to_EmployeeSettingsFragment);
        });

        // Book leave
        binding.bookLeaveBtn.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_EmployeeMainFragment_to_HolidayRequestFragment);
        });

        // Edit profile
        binding.employeeEditIcon.setOnClickListener(v -> { // navigate to EmployeeProfileFragment
            Navigation.findNavController(v).navigate(R.id.action_EmployeeMainFragment_to_EmployeeProfileFragment);
        });

        // View history
        binding.viewHistoryBtn.setOnClickListener(v -> {
            LeaveHistoryDialog dialog = new LeaveHistoryDialog();
            dialog.show(getParentFragmentManager(), "leave_history");
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // stop sync when view is£ destroyed
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
        binding = null;
    }
}