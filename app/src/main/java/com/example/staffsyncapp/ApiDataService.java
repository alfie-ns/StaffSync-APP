package com.example.staffsyncapp;

// Android libraries for logging and context usage testing
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.util.Log;

// Volley libraries for making API requests
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.staffsyncapp.models.Employee;

// JSON handling libraries for parsing and creating JSON objects
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * API requests for employee management using Volley; this file essentially
 * makes the API requests to different comp2000-server endpoints; GET, POST, PUT, DELETE.
 * using hr lines to separate out the different requests into their own sections
 
 - [X] Get All Employees: GET /employees:
 - [X] Get Employee by ID: GET /employees/get/<int:id>
 - [X] Add a New Employee: POST /employees/add
 - [X] Update an Employee’s Details: PUT /employees/edit/<int:id>
 - [X] Delete an Employee: DELETE /employees/delete/<int:id>
 - [X] Health Check: GET /health

 ---

 - [X] Salary Increment

 */

public class ApiDataService {
    private static final String TAG = "ApiDataService"; // log tag
    private static final String BASE_URL = "http://10.224.41.11/comp2000"; // api's base url

    private static RequestQueue queue; 
    private Context context; 

    // set context and initialise Volley request queue
    public ApiDataService(Context context) {
        this.context = context;
        queue = Volley.newRequestQueue(context); // access Volley request queue
    }

    // listener interfaces for API requests to handle success and error responses ---
    public interface EmployeeDeleteListener { 
        void onSuccess(String message);
        void onError(String error);
    }

    public interface HealthCallback { 
        void onResponse(String response);
    }

    public interface IncrementStatusListener { 
        void onSuccess(List<IncrementStatus> statusList);
        void onError(String error);
    }

    public interface EmployeeAddListener { 
        void onSuccess(String message);
        void onError(String error);
    }

    public interface EmployeeFetchListener { 
        void onEmployeesFetched(List<Employee> employees); // success callback
        void onError(String error); // failure callback
    }

    public interface EmployeeUpdateListener { 
        void onSuccess(String message);
        void onError(String error);
    }
// --------------------------------------------------------------------------------
    // IncrementStatus class to store employee data for salary increment
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
// --------------------------------------------------------------------------------
    /** [X]
     * GET request to fetch ALL employees
     * Endpoint: /employees
     */
    public static void getAllEmployees(EmployeeFetchListener listener) {
        String url = BASE_URL + "/employees";
        Log.d(TAG, "Attempting to fetch employees from: " + url);
    
        JsonArrayRequest request = new JsonArrayRequest( // fetch all employees
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try { // parse response; s
                            List<Employee> employees = new ArrayList<>(); // create list of employees
                            Log.d(TAG, "Received response. Employee count: " + response.length());
    
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject employeeObj = response.getJSONObject(i);
                                
                                Employee employee = new Employee( // create employee object with each data field
                                    employeeObj.optInt("id", -1),
                                    employeeObj.optString("firstname", "N/A"),
                                    employeeObj.optString("lastname", "N/A"),
                                    employeeObj.optString("email", "N/A"),
                                    employeeObj.optString("department", "N/A"),
                                    employeeObj.optDouble("salary", 0.0),
                                    employeeObj.optString("joiningdate", "N/A")
                                );
                                employees.add(employee); // add employee to list
                            }
    
                            Log.d(TAG, "Successfully parsed " + employees.size() + " employees");
                            listener.onEmployeesFetched(employees); // send parsed employee data back to AdminDashboardFragmen
    
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing employee data: " + e.getMessage());
                            listener.onError("Error parsing data: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMsg;
                        if (error.networkResponse != null) {
                            errorMsg = String.format("Network Error (Code %d): %s", 
                                error.networkResponse.statusCode,
                                new String(error.networkResponse.data));
                        } else {
                            errorMsg = "Network error: " + error.getMessage();
                        }
                        Log.e(TAG, errorMsg);
                        listener.onError(errorMsg);
                    }
                }
        );
    
        request.setShouldCache(false);
        queue.add(request);
    }
// --------------------------------------------------------------------------------
    /** [X]
     * GET request to fetch a particular employee by the respective ID
     * Endpoint: /employees/get/<int:id>
     */
    public void getEmployeeById(int id, EmployeeFetchListener listener) {
        String url = BASE_URL + "/employees/get/" + id;
        Log.d(TAG, "Attempting to fetch employee " + id);
    
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        // Create a list with single employee
                        List<Employee> employeeList = new ArrayList<>();
                        
                        // Parse the response
                        Employee employee = new Employee(
                            response.optInt("id", -1),
                            response.optString("firstname", "N/A"),
                            response.optString("lastname", "N/A"),
                            response.optString("email", "N/A"),
                            response.optString("department", "N/A"),
                            response.optDouble("salary", 0.0),
                            response.optString("joiningdate", "N/A")
                        );
                        
                        employeeList.add(employee);
                        listener.onEmployeesFetched(employeeList);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing employee data: " + e.getMessage());
                        listener.onError("Error parsing employee data");
                    }
                },
                error -> {
                    String errorMsg;
                    if (error.networkResponse != null) {
                        errorMsg = String.format("Network Error (Code %d): %s",
                                error.networkResponse.statusCode,
                                new String(error.networkResponse.data));
                    } else {
                        errorMsg = "Error fetching employee data";
                    }
                    listener.onError(errorMsg);
                }
        );
    
        request.setShouldCache(false);
        queue.add(request);
    }
// ---------------------------------------------------------------------------------
    /** [X]
     * POST request to add a new employee
     * Endpoint: /employees/add
     */
    public void addEmployee(String firstname, String lastname, String email, 
                        String department, double salary, String joiningdate, 
                        final EmployeeAddListener listener) {
        String url = BASE_URL + "/employees/add";
        
        // create JSON payload
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("firstname", firstname);
            jsonBody.put("lastname", lastname);
            jsonBody.put("email", email);
            jsonBody.put("department", department);
            jsonBody.put("salary", salary);
            jsonBody.put("joiningdate", joiningdate);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON body: " + e.getMessage());
            listener.onError("Error creating request");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest( // POST request to add employee
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    Log.d(TAG, "Employee added successfully");
                    listener.onSuccess("Employee added successfully");
                },
                error -> {
                    String errorMsg = error.networkResponse != null ?
                            String.format("Network Error (Code %d)", error.networkResponse.statusCode) :
                            "Error adding employee";
                    Log.e(TAG, errorMsg);
                    listener.onError(errorMsg);
                }
        );

        request.setShouldCache(false);
        queue.add(request);
    }
// ---------------------------------------------------------------------------------
    /** [X]
     * PUT request to update an employee's details
     * Endpoint: /employees/edit/<int:id>
     */
    public void updateEmployee(int id, String firstname, String lastname, String email,
                               String department, double salary, String joiningdate,
                               EmployeeUpdateListener listener) {
        String url = BASE_URL + "/employees/edit/" + id;

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("firstname", firstname);
            jsonBody.put("lastname", lastname);
            jsonBody.put("email", email);
            jsonBody.put("department", department);
            jsonBody.put("salary", salary);

            // format date from EditText's timestamp (EEE, dd MMM yyyy) to API required format (YYYY-MM-DD)
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.UK);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
            try {
                Date date = inputFormat.parse(joiningdate);
                String formattedDate = outputFormat.format(date);
                jsonBody.put("joiningdate", formattedDate);
            } catch (ParseException e) {
                // If parsing fails, try to use the original date assuming it's already YYYY-MM-DD
                jsonBody.put("joiningdate", joiningdate);
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    url,
                    jsonBody,
                    response -> {
                        Log.d(TAG, "Update successful: " + response.toString());
                        listener.onSuccess("Employee updated successfully");
                    },
                    error -> {
                        String errorMessage = "";
                        if (error.networkResponse != null) {
                            errorMessage = new String(error.networkResponse.data);
                            Log.e(TAG, "Error updating employee: " + errorMessage);
                        }
                        listener.onError("Error: " + errorMessage);
                    }
            );

            Log.d(TAG, "Sending update request with body: " + jsonBody.toString());
            request.setShouldCache(false);
            queue.add(request);

        } catch (JSONException e) {
            listener.onError("Error creating request: " + e.getMessage());
        }
    }
// ---------------------------------------------------------------------------------
    /** [X]
     * DELETE request to delete an employee by ID
     * Endpoint: /employees/delete/<int:id>
     */
    public void deleteEmployee(int employeeId, EmployeeDeleteListener listener) {
        String url = BASE_URL + "/employees/delete/" + employeeId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    Log.d(TAG, "Employee deleted successfully");
                    listener.onSuccess("Employee deleted successfully");
                },
                error -> {
                    String errorMsg = error.networkResponse != null ?
                            String.format("Network Error (Code %d)", error.networkResponse.statusCode) :
                            "Error deleting employee";
                    Log.e(TAG, errorMsg);
                    listener.onError(errorMsg);
                }
        );

        request.setShouldCache(false);
        queue.add(request);
    }
// ---------------------------------------------------------------------------------
    /** [X]
     * - GET request to test the API is working
     * - Endpoint: /health
     */
    public void checkHealth(HealthCallback callback) {
        String url = BASE_URL + "/health";
        Log.d(TAG, "Testing API health");
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> callback.onResponse("API is working"),
                error -> callback.onResponse(error.getMessage() != null ?
                        error.getMessage() : "Cannot connect to COMP2000")
        );
        request.setShouldCache(false);
        queue.add(request);
    }
// --------------------------------------------------------------------------------
    // HELPER FUNCTIONS
    public static void getIncrementStatus(IncrementStatusListener listener) {
    String url = BASE_URL + "/employees";
    JsonArrayRequest request = new JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    List<IncrementStatus> statusList = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject emp = response.getJSONObject(i);

                        // get the basic info I need to check it's working
                        String name = emp.optString("firstname", "") + " " + emp.optString("lastname", "");
                        double salary = emp.optDouble("salary", 0.0);
                        String joiningDate = emp.optString("joiningdate", "");

                        // calculate days since joining
                        long daysSince = calculateDaysSince(joiningDate);

                        // new increment status object containing employee name, salary, and days since joining;
                        // this info used to calculate who is due for their annual salary increase
                        statusList.add(new IncrementStatus(name.trim(), salary, daysSince));
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
