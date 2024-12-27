package com.example.staffsyncapp.utils;

import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.example.staffsyncapp.api.ApiDataService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import android.content.Context;

import com.example.staffsyncapp.models.Employee;

public class SalaryIncrementManager {
    private static final String TAG = "SalaryIncrementManager";

    public static void showSalaryIncrementStatus(Context context) {
        LocalDataService dbHelper = new LocalDataService(context);

        // get all employees who were due an increment
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT full_name, salary FROM employee_details " +
                        "WHERE CAST(JULIANDAY('now') - JULIANDAY(COALESCE(last_increment_date, hire_date)) AS INTEGER) >= 365",
                null
        );

        StringBuilder message = new StringBuilder();
        int eligibleCount = 0;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                double oldSalary = cursor.getDouble(1);
                double newSalary = oldSalary * 1.05;

                message.append(name)
                        .append(": £").append(String.format("%.2f", oldSalary))
                        .append(" → £").append(String.format("%.2f", newSalary))
                        .append("\n");
                eligibleCount++;
            } while (cursor.moveToNext());
            cursor.close();
        }

        // apply increments
        dbHelper.checkAndApplySalaryIncrements();

        // show dialog with results
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Salary Increment Status");

        if (eligibleCount > 0) {
            builder.setMessage("The following employees received a 5% increase:\n\n" + message.toString());
        } else { // else-if no employees eligible
            builder.setMessage("No employees are eligible for salary increments at this time.");
        }

        builder.setPositiveButton("OK", null)
                .show();
    }

    public static class IncrementStatus {
        public final String name;
        public final double salary;
        public final long daysSince;

        public IncrementStatus(String name, double salary, long daysSince) {
            this.name = name;
            this.salary = salary;
            this.daysSince = daysSince;
        }
    }

    public interface IncrementStatusListener {
        void onSuccess(List<IncrementStatus> statusList);
        void onError(String error);
    }


    public static void getIncrementStatus(IncrementStatusListener listener) {
        ApiDataService.getAllEmployees(new ApiDataService.EmployeeFetchListener() {
            @Override
            public void onEmployeesFetched(List<Employee> employees) {
                try {
                    List<IncrementStatus> statusList = new ArrayList<>();
                    for (Employee emp : employees) {
                        String name = emp.getFirstname() + " " + emp.getLastname();
                        double salary = emp.getSalary();
                        long daysSince = calculateDaysSince(emp.getJoiningdate());

                        statusList.add(new IncrementStatus(name.trim(), salary, daysSince));
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
    public static long calculateDaysSince(String date) {
        try { // get days - joiningDate
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
            Date joinDate = sdf.parse(date);
            Date now = new Date();
            return TimeUnit.DAYS.convert(now.getTime() - joinDate.getTime(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return 0;
        }
    }

}