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

import java.util.List;

import android.content.ContentValues;


/**
 * TODO
 *  [ ] book leave
 *  [ ] pending requests
 *  [ ] view leave history
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
        Cursor cursor = db.query(
                "employee_details",
                null,
                "employee_id = ?",
                new String[]{String.valueOf(employeeId)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
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
        }
        db.close();
    }

    private void setupPeriodicSync(LocalDataService dbHelper, int employeeId) {
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                // sync with API; call get Employee By ID API method
                apiService.getEmployeeById(employeeId, new ApiDataService.EmployeeFetchListener() {
                    @Override
                    public void onEmployeesFetched(List<Employee> employees) {
                        if (employees != null && !employees.isEmpty()) {
                            Employee apiEmployee = employees.get(0);

                            // update UI if data changed and update local DB
                            if (currentEmployee == null || !currentEmployee.equals(apiEmployee)) {
                                currentEmployee = apiEmployee;
                                updateUIWithEmployeeData(currentEmployee);
                            }

                            ContentValues values = new ContentValues();
                            values.put("employee_id", employeeId);
                            values.put("full_name", apiEmployee.getFirstname() + " " + apiEmployee.getLastname());
                            values.put("department", apiEmployee.getDepartment());
                            values.put("salary", apiEmployee.getSalary());

                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            db.insertWithOnConflict("employee_details", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                            db.close();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("EmployeeSync", "API sync failed: " + error);
                    }
                });

                // schedule next sync
                syncHandler.postDelayed(this, SYNC_INTERVAL);
            }
        };

        // start periodic sync
        syncHandler.post(syncRunnable);
    }

    private void updateUIWithEmployeeData(Employee employee) {
        binding.employeeId.setText("My Employee ID: #" + employee.getId());
        binding.department.setText("Department: " + employee.getDepartment());

        LocalDate joinDate = LocalDate.parse(employee.getJoiningDate());
        Period period = Period.between(joinDate, LocalDate.now());
        String yearsOfService = period.getYears() + " years, " + period.getMonths() + " months";
        binding.yearsOfService.setText("Years of Service: " + yearsOfService);

        LocalDate nextReview = joinDate.plusYears(Period.between(joinDate, LocalDate.now()).getYears() + 1);
        Period timeToReview = Period.between(LocalDate.now(), nextReview);
        binding.nextReview.setText("Next Salary Review: " + timeToReview.getMonths() + " months");

        SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
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