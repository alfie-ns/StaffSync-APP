package com.example.staffsyncapp;

// Android libraries for logging and context usage testing
import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

/**
 * API requests for employee management using Volley; this file essentially
 * makes the API requests to different comp2000-server endpoints; GET, POST, PUT, DELETE.
 * using hr lines to separate out the different requests into their own sections
 
 - [X] Get All Employees: GET /employees:
 - [X] Get Employee by ID: GET /employees/get/<int:id>
 - [ ] Add a New Employee: POST /employees/add
 - [ ] Update an Employee’s Details: PUT /employees/edit/<int:id>
 - [ ] Delete an Employee: DELETE /employees/delete/<int:id>
 - [X] Health Check: GET /health
 */

public class ApiDataService {
    private static final String TAG = "ApiDataService"; // log tag
    private static final String BASE_URL = "http://10.224.41.11/comp2000"; // api's base url

    private static RequestQueue queue; 
    private Context context; 

    // set context and initialise Volley request queue
    public ApiDataService(Context context) {
        this.context = context;
        this.queue = Volley.newRequestQueue(context); 
    }

    public interface HealthCallback { // interface for handling comp2000 health checking responses
        void onResponse(String response);
    }

    // Interface for handling employee fetch operations and responses
    public interface EmployeeFetchListener {
        void onEmployeesFetched(List<Employee> employees); // success callback
        void onError(String error); // failure callback
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

    // ...

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
}
