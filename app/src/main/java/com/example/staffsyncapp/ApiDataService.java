package com.example.staffsyncapp;

// Android libraries for logging and context usage testing
import android.annotation.SuppressLint;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.util.Log;

// Volley libraries for making API requests
import androidx.annotation.WorkerThread;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
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

/** Employee management API service handling 'comp2000' server requests
 * 
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

 * These functions use dedicated local worker threads to handle network requests in background;
 * returns data via EmployeeFetchListener callbacks on main thread; they also use
 * Volley's RequestQueue to handle network requests and responses.
 * 
 * Worker Threads:
 * - [X] getAllEmployees
 * - [X] getEmployeeById
 * - [X] addEmployee
 * - [X] updateEmployee
 * - [X] deleteEmployee
 * - [X] checkHealth
 *
 * I subsequently had to make getEmployeeById static because the method is
 * called directly on the class name (ApiDataService.getEmployeeById) rather than 
 * on an instance of the class (apiService.getEmployeeById) this is because in
 * regards to Holiday requests, we need to access employee data without instantiating 
 * the ApiDataService class each time we validate or process a request
 */

public class ApiDataService {
    private static final String TAG = "ApiDataService"; // log tag
    private static final String BASE_URL = "http://10.224.41.11/comp2000"; // base url

    private static ApiWorkerThread workerThread;

    private static RequestQueue queue; 
    private Context context; 

    // set context and initialise Volley request queue
    public ApiDataService(Context context) {
        this.context = context;
        queue = Volley.newRequestQueue(context); // access Volley request queue
        workerThread = new ApiWorkerThread(); // 1- initialise worker thread
        workerThread.start(); 
    }

    // various listener interfaces for API requests to handle success and error responses ---
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
        void onSuccess(String message, int employeeId, String email);
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
    /** [X] [X]
     * GET request to fetch ALL employees
     * Endpoint: /employees
     **/
    public static void getAllEmployees(EmployeeFetchListener listener) {
        String url = BASE_URL + "/employees";
        Log.d(TAG, "Attempting to fetch employees from: " + url);

        workerThread.queueTask(() -> {
            Log.d(TAG, "getAllEmployees: Worker thread executing: " + Thread.currentThread().getName());
            JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "getAllEmployees: Response received on thread: " + Thread.currentThread().getName());
                    workerThread.postToMainThread(() -> {
                        try {
                            List<Employee> employees = new ArrayList<>();
                            Log.d(TAG, "Received response. Employee count: " + response.length());
    
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject employeeObj = response.getJSONObject(i);
                                
                                Employee employee = new Employee(
                                    employeeObj.optInt("id", -1),
                                    employeeObj.optString("firstname", "N/A"),
                                    employeeObj.optString("lastname", "N/A"),
                                    employeeObj.optString("email", "N/A"),
                                    employeeObj.optString("department", "N/A"),
                                    employeeObj.optDouble("salary", 0.0),
                                    employeeObj.optString("joiningdate", "N/A")
                                );
                                employees.add(employee);
                            }
    
                            Log.d(TAG, "Successfully parsed " + employees.size() + " employees");
                            listener.onEmployeesFetched(employees);
    
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing employee data: " + e.getMessage());
                            listener.onError("Error parsing data: " + e.getMessage());
                        }
                    });
                },
                error -> workerThread.postToMainThread(() -> {
                    String errorMsg = error.networkResponse != null ?
                        String.format(Locale.UK, "Network Error (Code %d)", error.networkResponse.statusCode) :
                        "Failed to fetch employee data";
                    Log.e(TAG, errorMsg);
                    listener.onError(errorMsg);
                })
            );
    
            request.setShouldCache(false);
            queue.add(request);
        });
    }
// --------------------------------------------------------------------------------
    /** [X] [X]
     * GET request to fetch a particular employee by the respective ID
     * Endpoint: /employees/get/<int:id>
     **/
    public static void getEmployeeById(int id, EmployeeFetchListener listener) {
        String url = BASE_URL + "/employees/get/" + id;
        Log.d(TAG, "Attempting to fetch employee " + id);

        workerThread.queueTask(() -> {
            Log.d(TAG, "getEmployeeById: Worker thread executing: " + Thread.currentThread().getName());
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        workerThread.postToMainThread(() -> {
                            Log.d(TAG, "getEmployeeById: Response received on thread: " + Thread.currentThread().getName());
                            Log.d(TAG, "API response: " + response);
                            try {
                                List<Employee> employeeList = new ArrayList<>();
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
                        });
                    },
                    error -> workerThread.postToMainThread(() -> {
                        String errorMsg = error.networkResponse != null ?
                                String.format(Locale.UK, "Network Error (Code %d)", error.networkResponse.statusCode) :
                                "Failed to fetch employee data";
                        Log.e(TAG, errorMsg);
                        listener.onError(errorMsg);
                    })
            );

            request.setShouldCache(false);
            queue.add(request);
        });
    }
// ---------------------------------------------------------------------------------
    /** [X] [X]
     * POST request to add a new employee
     * Endpoint: /employees/add
     **/
    public void addEmployee(String firstname, String lastname, String email,
                                String department, double salary, String joiningdate,
                                final EmployeeAddListener listener) {
        String url = BASE_URL + "/employees/add";
        Log.d(TAG, "Attempting to add new employee: " + firstname + " " + lastname);

        workerThread.queueTask(() -> {
            Log.d(TAG, "addEmployee: Worker thread executing: " + Thread.currentThread().getName());

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("firstname", firstname);
                jsonBody.put("lastname", lastname);
                jsonBody.put("email", email);
                jsonBody.put("department", department);
                jsonBody.put("salary", salary);
                jsonBody.put("joiningdate", joiningdate);

                Log.d(TAG, "Request body: " + jsonBody.toString());

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,
                        response -> {
                            Log.d(TAG, "Employee addition successful");
                            workerThread.postToMainThread(() -> {
                                getAllEmployees(new EmployeeFetchListener() {
                                    @Override
                                    public void onEmployeesFetched(List<Employee> employees) {
                                        for (Employee emp : employees) {
                                            if (emp.getEmail().equals(email)) {
                                                listener.onSuccess("Employee added successfully",
                                                        emp.getId(), email);
                                                return;
                                            }
                                        }
                                        listener.onError("Could not verify new employee");
                                    }

                                    @Override
                                    public void onError(String error) {
                                        listener.onError("Error verifying new employee: " + error);
                                    }
                                });
                            });
                        },
                        error -> workerThread.postToMainThread(() -> {
                            String errorMsg = error.networkResponse != null ?
                                    String.format(Locale.UK, "Network Error (Code %d): %s",
                                            error.networkResponse.statusCode,
                                            new String(error.networkResponse.data)) :
                                    "Error adding employee";
                            Log.e(TAG, errorMsg);
                            listener.onError(errorMsg);
                        })
                );

                request.setShouldCache(false);
                queue.add(request);

            } catch (JSONException e) {
                workerThread.postToMainThread(() -> {
                    Log.e(TAG, "Error creating JSON body: " + e.getMessage());
                    listener.onError("Error creating request");
                });
            }
        });
    }
// ---------------------------------------------------------------------------------
    /** [X] [X]
     * PUT request to update an employee's details
     * Endpoint: /employees/edit/<int:id>
     **/
    public void updateEmployee(int id, String firstname, String lastname, String email,
                               String department, double salary, String joiningdate,
                               EmployeeUpdateListener listener) {
        String url = BASE_URL + "/employees/edit/" + id;
        Log.d(TAG, "Attempting to update employee " + id);

        workerThread.queueTask(() -> {
            Log.d(TAG, "updateEmployee: Worker thread executing: " + Thread.currentThread().getName());

            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("firstname", firstname);
                jsonBody.put("lastname", lastname);
                jsonBody.put("email", email);
                jsonBody.put("department", department);
                jsonBody.put("salary", salary);

                // format date from EditText's timestamp to API format
                SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.UK);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                try {
                    Date date = inputFormat.parse(joiningdate);
                    String formattedDate = outputFormat.format(date);
                    jsonBody.put("joiningdate", formattedDate);
                } catch (ParseException e) {
                    // if parsing fails, use original date format, presuming YYYY-MM-DD
                    jsonBody.put("joiningdate", joiningdate);
                }

                Log.d(TAG, "Update request body: " + jsonBody.toString());

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.PUT,
                        url,
                        jsonBody,
                        response -> {
                            Log.d(TAG, "Employee update successful");
                            workerThread.postToMainThread(() -> {
                                listener.onSuccess("Employee updated successfully");
                            });
                        },
                        error -> workerThread.postToMainThread(() -> {
                            String errorMsg = error.networkResponse != null ?
                                    String.format(Locale.UK, "Network Error (Code %d): %s",
                                            error.networkResponse.statusCode,
                                            new String(error.networkResponse.data)) :
                                    "Error updating employee";
                            Log.e(TAG, errorMsg);
                            listener.onError(errorMsg);
                        })
                );

                request.setShouldCache(false);
                queue.add(request);

            } catch (JSONException e) {
                workerThread.postToMainThread(() -> {
                    Log.e(TAG, "Error creating JSON body: " + e.getMessage());
                    listener.onError("Error creating request: " + e.getMessage());
                });
            }
        });
    }
// ---------------------------------------------------------------------------------
    /** [X] [X]
     * DELETE request to delete an employee by ID
     * Endpoint: /employees/delete/<int:id>
     **/
    public void deleteEmployee(int employeeId, EmployeeDeleteListener listener) {
        String url = BASE_URL + "/employees/delete/" + employeeId;
        Log.d(TAG, "Attempting to delete employee " + employeeId);

        workerThread.queueTask(() -> {
            Log.d(TAG, "deleteEmployee: Worker thread executing: " + Thread.currentThread().getName());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.DELETE,
                    url,
                    null,
                    response -> {
                        Log.d(TAG, "Employee deletion successful");
                        workerThread.postToMainThread(() -> {
                            listener.onSuccess("Employee deleted successfully");
                        });
                    },
                    error -> workerThread.postToMainThread(() -> {
                        @SuppressLint("DefaultLocale") String errorMsg = error.networkResponse != null ?
                                String.format(Locale.UK, "Network Error (Code %d)", error.networkResponse.statusCode) :
                                "Error deleting employee";
                        Log.e(TAG, errorMsg);
                        listener.onError(errorMsg);
                    })
            );

            request.setShouldCache(false);
            queue.add(request);
        });
    }
// ---------------------------------------------------------------------------------
    /** [X] [X]
     * - GET request to test the API is working
     * - Endpoint: /health
     **/
    public void checkHealth(HealthCallback callback) {
        String url = BASE_URL + "/health";
        Log.d(TAG, "Testing API health");

        workerThread.queueTask(() -> {
            Log.d(TAG, "checkHealth: Worker thread executing: " + Thread.currentThread().getName());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        Log.d(TAG, "Health check successful");
                        workerThread.postToMainThread(() -> {
                            callback.onResponse("API is working");
                        });
                    },
                    error -> workerThread.postToMainThread(() -> {
                        String errorMsg = error.getMessage() != null ?
                                error.getMessage() : "Cannot connect to COMP2000";
                        Log.e(TAG, "Health check failed: " + errorMsg);
                        callback.onResponse(errorMsg);
                    })
            );

            request.setShouldCache(false);
            queue.add(request);
        });
    }
// --------------------------------------------------------------------------------
    // HELPER FUNCTIONS
    public void cleanup() { // shut down all running workerThreads; clean running threads
        if (workerThread != null) {
            workerThread.shutdown();
            workerThread = null; // dereference to free up resources
        }
    }
}
