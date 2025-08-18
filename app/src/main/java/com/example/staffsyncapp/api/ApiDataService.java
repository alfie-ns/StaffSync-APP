package com.example.staffsyncapp.api;

// Android libraries for logging and context usage testing
import android.annotation.SuppressLint;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

// Volley libraries for making API requests
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.OfflineSyncManager;

// JSON handling libraries for parsing and creating JSON objects
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Employee management API service handling 'comp2000' server requests

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

 * These functions use dedicated local worker threads to handle network requests in background;
 * returns data via EmployeeFetchListener callbacks on main thread; they also use
 * Volley's RequestQueue to handle network requests and responses.

 * Worker Threads:
 * - [X] getAllEmployees
 * - [X] getEmployeeById
 * - [X] addEmployee
 * - [X] updateEmployee
 * - [X] deleteEmployee
 * - [X] checkHealth

 * I subsequently had to make getEmployeeById static because the method is
 * called directly on the class name (ApiDataService.getEmployeeById) rather than 
 * on an instance of the class (apiService.getEmployeeById) this is because in
 * regards to Holiday requests, we need to access employee data without instantiating 
 * the ApiDataService class each time we validate or process a request
 */

public class ApiDataService {
    private static final String TAG = "ApiDataService"; // log tag
    private static final String BASE_URL = "http://10.0.2.2:8000/comp2000";

    private static ApiWorkerThread workerThread;
    private OfflineSyncManager offlineSyncManager;

    private static RequestQueue queue; 
    private Context context; 

    // set context and initialise Volley request queue
    public ApiDataService(Context context) {
        this.context = context;
        queue = Volley.newRequestQueue(context); // access Volley request queue
        offlineSyncManager = new OfflineSyncManager(context, new LocalDataService(context).getWritableDatabase(), this); // initialise offline sync manager
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
    /** [X] [X]
     * GET request to fetch ALL employees
     * Endpoint: /employees
     * Fetch the employee data from the API and parse it into a list of Employee objects
     */
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
     * Fetch a particular employee by their ID and parse the data into an Employee object
     */
    public static void getEmployeeById(int id, EmployeeFetchListener listener) {
        String url = BASE_URL + "/employees/get/" + id;
        Log.d(TAG, "Attempting to fetch employee " + id);

        if (workerThread == null || !workerThread.isAlive()) { // if worker thread is null or dead, create a new one
            workerThread = new ApiWorkerThread();
            workerThread.start();
        }

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
     */
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

                JsonObjectRequest request = new JsonObjectRequest( // POST request with JSON body passing raw json for new employee
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
     */
    public void updateEmployee(int id, String firstname, String lastname, String email,
                               String department, double salary, String joiningdate,
                               EmployeeUpdateListener listener) {
        String url = BASE_URL + "/employees/edit/" + id;
        Log.d(TAG, "Attempting to update employee " + id);

        workerThread.queueTask(() -> {
            Log.d(TAG, "updateEmployee: Worker thread executing: " + Thread.currentThread().getName());

            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("action", "edit_employee");
                jsonBody.put("id", id);
                jsonBody.put("firstname", firstname.equals("null") ? "" : firstname);
                jsonBody.put("lastname", lastname.equals("null") ? "" : lastname);
                jsonBody.put("email", email.equals("null") ? "" : email);
                jsonBody.put("department", department.equals("null") ? "" : department);
                jsonBody.put("salary", salary);

                // Format date from EditText's timestamp to API format
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
                        error -> {
                            // Queue for offline sync first
                            offlineSyncManager.enqueueTask(jsonBody);

                            // Then notify via listener if worker thread still exists
                            if (workerThread != null) {
                                workerThread.postToMainThread(() -> {
                                    listener.onError("Update queued for later; API currently unavailable...");
                                });
                            }
                        }
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
     */
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
                        String errorMsg = error.networkResponse != null ?
                                String.format("Network Error (Code %d)", error.networkResponse.statusCode) :
                                "Error deleting employee";
                        Log.e(TAG, errorMsg);

                        try {
                            JSONObject jsonBody = new JSONObject();
                            jsonBody.put("action", "delete_employee");
                            jsonBody.put("id", employeeId);
                            offlineSyncManager.enqueueTask(jsonBody);

                            listener.onError(errorMsg + " - Delete queued for later"); // notify UI
                        } catch (JSONException e) {
                            Log.e(TAG, "Error queuing delete task", e);
                            listener.onError(errorMsg); // notify UI without queuing
                        }
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
     */
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
                                error.getMessage() : "Cannot connect to comp2000";
                        Log.e(TAG, "Health check failed: " + errorMsg);
                        callback.onResponse(errorMsg);
                    })
            );

            request.setShouldCache(false);
            queue.add(request);
        });
    }
// --------------------------------------------------------------------------------
    // OFFLINE-SYNC and HELPER FUNCTIONS

    public void processQueuedTask(JSONObject data) throws JSONException {
        try {
            String action = data.getString("action");
            switch (action) {
                case "edit_employee":
                    Log.d(TAG, "Processing queued edit task: " + data);
                    String url = BASE_URL + "/employees/edit/" + data.getInt("id");

                    // If API isn't reachable, throw exception to keep task in queue
                    if (!isApiReachable()) {
                        throw new Exception("API not reachable");
                    }

                    // Try to make direct API request first, if error(i.e. API connection unsuccesful) queue task
                    JsonObjectRequest request = new JsonObjectRequest(
                            Request.Method.PUT,
                            url,
                            data,
                            response -> {
                                Log.d(TAG, "Successfully processed queued edit task");
                            },
                            error -> {
                                // throw exception on error to keep task in queue
                                throw new RuntimeException("Failed to process task: " + error.getMessage());
                            }
                    );

                    request.setShouldCache(false);
                    queue.add(request);
                    break;
                case "delete_employee":
                    Log.d(TAG, "Processing queued delete task: " + data);
                    String deleteUrl = BASE_URL + "/employees/delete/" + data.getInt("id");

                    if (!isApiReachable()) {
                        throw new Exception("API not reachable");
                    }

                    JsonObjectRequest deleteRequest = new JsonObjectRequest(
                            Request.Method.DELETE,
                            deleteUrl,
                            data,
                            response -> {
                                Log.d(TAG, "Successfully processed queued delete task");
                            },
                            error -> {
                                // keep task on queue
                                throw new RuntimeException("Failed to process task: " + error.getMessage());
                            }
                    );
                    deleteRequest.setShouldCache(false);
                    queue.add(deleteRequest);
                    break;

                default:
                    Log.e(TAG, "Unsupported action: " + action);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing queued task: " + e.getMessage());
            throw new JSONException(e.getMessage());  // Rethrow to signal failure
        }
    }

    private boolean isApiReachable() {
        try {
            InetAddress address = InetAddress.getByName("10.224.41.11");
            return address.isReachable(1000);
        } catch (Exception e) {
            return false;
        }
    }

    public void cleanUp() { // shut down all running workerThreads; clean running threads
        if (workerThread != null) {
            workerThread.shutdown();
            workerThread = null; // dereference to free up resources
        }
    }
}
