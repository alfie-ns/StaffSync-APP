package com.example.staffsyncapp.leave;

// Android core
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

// AndroidX
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

// Project-specific
import com.example.staffsyncapp.api.ApiDataService;
import com.example.staffsyncapp.databinding.EmployeeLeaveRequestFragmentBinding;
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NotificationService;

// Java
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Fragment for employees to submit holiday/leave requests.
 * Key functionalities:
 * - displays employee's remaining leave balance
 * - allows selecting start and end dates with validation
 *   - min 7 days in advance, max 14 consecutive days
 * - Submits request to local DB and notifies admin
 * - Navigates back to main screen on successful submission
 */

public class LeaveRequestFragment extends Fragment {
    private static final String TAG = "HolidayRequestFragment";
    private EmployeeLeaveRequestFragmentBinding binding;
    private LocalDataService dbHelper;
    private NotificationService notificationService;

    private Calendar startDate = Calendar.getInstance();
    private Calendar endDate = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);

    private static final int MIN_ADVANCE_DAYS = 7;
    private static final int MAX_CONSECUTIVE_DAYS = 14;
    private static final int TOTAL_ANNUAL_LEAVE = 30;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = EmployeeLeaveRequestFragmentBinding.inflate(inflater, container, false);
        dbHelper = new LocalDataService(requireContext());
        notificationService = new NotificationService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();
        loadEmployeeLeaveBalance();
    }

    private void setupClickListeners() {
        binding.backArrow.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        binding.startDateInput.setOnClickListener(v -> showStartDatePicker());
        binding.endDateInput.setOnClickListener(v -> showEndDatePicker());

        binding.submitRequestBtn.setOnClickListener(v -> validateAndSubmitRequest());
    }

    private void loadEmployeeLeaveBalance() {
        SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int employeeId = prefs.getInt("logged_in_employee_id", -1);

        if (employeeId != -1) {
            int remainingDays = dbHelper.getRemainingLeaveDays(employeeId);
            binding.remainingDays.setText(String.format(Locale.UK,
                    "Holiday Allowance: %d/%d days remaining", remainingDays, TOTAL_ANNUAL_LEAVE));
        }
    }

    private void showStartDatePicker() { // show date picker; set min date; show dialog
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.DAY_OF_MONTH, MIN_ADVANCE_DAYS);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    startDate.set(year, month, dayOfMonth);
                    binding.startDateInput.setText(dateFormatter.format(startDate.getTime()));
                    updateDaysRequested();
                },
                startDate.get(Calendar.YEAR),
                startDate.get(Calendar.MONTH),
                startDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        datePickerDialog.show();
    }

    private void showEndDatePicker() { // show date picker; set min and max date; dialog
        if (binding.startDateInput.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please select a start date first", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar maxDate = (Calendar) startDate.clone();
        maxDate.add(Calendar.DAY_OF_MONTH, MAX_CONSECUTIVE_DAYS);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    endDate.set(year, month, dayOfMonth);
                    binding.endDateInput.setText(dateFormatter.format(endDate.getTime()));
                    updateDaysRequested();
                },
                endDate.get(Calendar.YEAR),
                endDate.get(Calendar.MONTH),
                endDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(startDate.getTimeInMillis());
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        datePickerDialog.show();
    }

    private void updateDaysRequested() {
        if (!binding.startDateInput.getText().toString().isEmpty() &&
                !binding.endDateInput.getText().toString().isEmpty()) {

            long diffInMillies = endDate.getTimeInMillis() - startDate.getTimeInMillis();
            long daysRequested = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;

            binding.daysRequested.setText(String.format(Locale.UK,
                    "Days Requested: %d", daysRequested));

            SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
            int employeeId = prefs.getInt("logged_in_employee_id", -1);

            if (employeeId != -1) {
                int remainingDays = dbHelper.getRemainingLeaveDays(employeeId);
                // if requested days are less than or equal to remaining days proceed with submission
                if (daysRequested <= remainingDays) {
                    binding.submitRequestBtn.setEnabled(true);
                    binding.errorMessage.setVisibility(View.GONE);
                } else { // if exceed remaining days, disable submission and show errorMessage
                    binding.submitRequestBtn.setEnabled(false);
                    binding.errorMessage.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * Validates the leave request before submission:
     * 1- start and end dates must be selected
     * 2- reason must be provided
     * 3- request must be at least 7 days in advance
     * 4- maximum of 14 consecutive days allowed
     * 5- employee must have sufficient leave balance
     * @return true if all validations pass, false otherwise
     */
    private boolean validateRequest() {
        String startDateStr = binding.startDateInput.getText().toString();
        String endDateStr = binding.endDateInput.getText().toString();
        String reason = binding.reasonInput.getText().toString().trim();

        if (startDateStr.isEmpty() || endDateStr.isEmpty()) { // catch error if ANY is empty
            showError("Please select both start and end dates");
            return false;
        }

        if (reason.isEmpty()) { // if no reason provided
            showError("Please provide a reason for your leave/holiday request");
            return false;
        }

        Calendar today = Calendar.getInstance();
        long daysUntilStart = TimeUnit.DAYS.convert(
                startDate.getTimeInMillis() - today.getTimeInMillis(),
                TimeUnit.MILLISECONDS
        );

        if (daysUntilStart < MIN_ADVANCE_DAYS) {
            showError("Requests must be submitted at least 1 week in advance");
            return false;
        }

        long requestedDays = TimeUnit.DAYS.convert(
                endDate.getTimeInMillis() - startDate.getTimeInMillis(),
                TimeUnit.MILLISECONDS
        ) + 1;

        if (requestedDays > MAX_CONSECUTIVE_DAYS) {
            showError("Maximum consecutive days allowed is 14");
            return false;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int employeeId = prefs.getInt("logged_in_employee_id", -1);

        if (employeeId != -1) {
            int usedDays = dbHelper.getEmployeeUsedLeave(employeeId);
            if (usedDays + requestedDays > TOTAL_ANNUAL_LEAVE) {
                showError("Insufficient leave balance");
                return false;
            }
        }

        return true;
    }

    /**
     * If request is valid, submits it to local DB and notifies admin;
     * on success, navigates back to main employee screen;
     * displays error message if submission fails.
     */
    public void validateAndSubmitRequest() {
        if (!validateRequest()) {
            return;
        }

        hideError();

        SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int employeeId = prefs.getInt("logged_in_employee_id", -1);

        if (employeeId == -1) { // if error maintain status quo
            showError("Unable to identify employee");
            return;
        }

        String reason = binding.reasonInput.getText().toString().trim();
        if (reason.length() < 10) {
            showError("Please provide a more detailed reason");
            return;
        }

        long requestResult = dbHelper.submitLeaveRequest(
                employeeId,
                binding.startDateInput.getText().toString(),
                binding.endDateInput.getText().toString(),
                reason
        );

        Log.d(TAG, "Submit leave request result: " + requestResult);

        if (requestResult == -2) { // implies ok-error; validly rejected
            // -0 success; -1 failure; -2 ok-error
            showError("Insufficient leave days remaining");
        } else if (requestResult != -1) {
            notifyAdmin(employeeId);
            Toast.makeText(requireContext(), "Holiday request submitted successfully", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
        } else {
            showError("Failed to submit request. Please try again.");
            Log.e(TAG, "Failed to submit leave request");
        }
    }

    private void notifyAdmin(int employeeId) {
        if (notificationService == null) {
            notificationService = new NotificationService(requireContext());
        }

        // store values before async call
        final String startDate = binding.startDateInput.getText().toString();
        final String endDate = binding.endDateInput.getText().toString();
        final String reason = binding.reasonInput.getText().toString();

        // send notification immediately with available info
        notificationService.sendLeaveRequestToAdmin(
                "Employee #" + employeeId,  // Use ID if name fetch fails
                startDate,
                endDate,
                reason
        );

        // try to get employee name asynchronously
        ApiDataService.getEmployeeById(employeeId, new ApiDataService.EmployeeFetchListener() {
            @Override
            public void onEmployeesFetched(List<Employee> employees) {
                if (employees != null && !employees.isEmpty()) {
                    Employee employee = employees.get(0);
                    // send another notification with full name if fetch succeeds
                    notificationService.sendLeaveRequestToAdmin(
                            employee.getFirstname() + " " + employee.getLastname(),
                            startDate,
                            endDate,
                            reason
                    );
                }
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching employee details: " + error);
            }
        });
    }

    private void showError(String message) {
        binding.errorMessage.setText(message);
        binding.errorMessage.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.errorMessage.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // clear binding reference
    }
}