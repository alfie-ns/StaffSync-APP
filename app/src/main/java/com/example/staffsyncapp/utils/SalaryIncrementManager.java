package com.example.staffsyncapp.utils;

import android.icu.text.SimpleDateFormat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.example.staffsyncapp.ApiDataService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SalaryIncrementManager {
    private static final String TAG = "SalaryIncrementManager";

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

    public static void getIncrementStatus(ApiDataService.IncrementStatusListener listener) {
        String url = BASE_URL + "/employees";
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<ApiDataService.IncrementStatus> statusList = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject emp = response.getJSONObject(i);

                            // get the basic info I need to check it's working
                            String name = emp.optString("firstname", "") + " " + emp.optString("lastname", "");
                            double salary = emp.optDouble("salary", 0.0);
                            String joiningDate = emp.optString("joiningdate", "");

                            // calculate days since joining
                            long daysSince = calculateDaysSince(joiningDate);

                            // new increment status object containing employee name, salary, and days since joining;
                            // this info then used to calculate who is due for their annual salary increase
                            statusList.add(new ApiDataService.IncrementStatus(name.trim(), salary, daysSince));
                        }
                        listener.onSuccess(statusList);
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking increments: " + e.getMessage());
                        listener.onError("Failed to check increments");
                    }
                },
                error -> listener.onError("Network error")
        );
        request.setShouldCache(false);
        queue.add(request);
    }

    private static long calculateDaysSince(String date) {
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