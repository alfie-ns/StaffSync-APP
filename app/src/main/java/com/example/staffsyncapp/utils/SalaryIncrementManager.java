package com.example.staffsyncapp.utils;

import android.icu.text.SimpleDateFormat;
import android.util.Log;

import com.example.staffsyncapp.api.ApiDataService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.example.staffsyncapp.models.Employee;

public class SalaryIncrementManager {
    private static final String TAG = "SalaryIncrementManager";

    public static void showSalaryIncrementStatus() {
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

    public static void processIncrements(List<IncrementStatus> statusList, IncrementStatusListener listener) {
        try {
            // handle increment logic
            listener.onSuccess(statusList);
        } catch (Exception e) {
            Log.e(TAG, "Error processing increments: " + e.getMessage());
            listener.onError("Failed to process increments");
        }
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