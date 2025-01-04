package com.example.staffsyncapp.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.navigation.NavDeepLinkBuilder;

import com.example.staffsyncapp.R;
import com.example.staffsyncapp.models.LeaveRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Database service handling local data storage and authentication for StaffSync.
 * Manages employee accounts, employee records, leave requests and notifications using SQLite.
 *
 * features:
 * - Employee authentication and session management
 * - Employee details and salary tracking
 * - Leave request processing
 * - Notification handling
 * - Automated salary increment management
 *
 * TODO:
 * - [X] leave request processing
 * - [X] notification handling from employee and admin
 * - [X] automated salary increment management
 * - [X] employee account management
 *
 */

public class LocalDataService extends SQLiteOpenHelper {

    // 1. Constants and Variables ----------------------------------------------
    private static final String DATABASE_NAME = "staffsync.db";
    private static final int DATABASE_VERSION = 2; // starting db version
    private static final String TAG = "DatabaseHelper";

    private static Boolean isLoggedIn = false;  // tracks login state

    private static final String PREFS_NAME = "StaffSyncPrefs";
    private static final String KEY_ADMIN_LOGGED_IN = "admin_logged_in";

    private final Context context;
    private final SQLiteDatabase db;  // thread safety

    private static final String KEY_EMPLOYEE_LOGGED_IN = "employee_logged_in";

    private static final String TEMP_PASSWORD_PREFIX = "EMP";

    public void deleteLeaveRequest(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("leave_requests", "id = ?", new String[]{String.valueOf(id)});
    }

    public interface LeaveRequestCallback {
        void onComplete(List<LeaveRequest> requests, String error);
    }

    public interface StatusUpdateCallback {
        void onSuccess(boolean success);
    }

    // 2. Constructor and Database Creation ------------------------------------
    public LocalDataService(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION); // call to super class constructor
        this.context = context; // store context
        db = this.getWritableDatabase();

        // ensure admin exists
        if (!adminExists()) {
            addDefaultAdmin();
        }
    }

    @Override // SQLite DATABASE CREATION
    public void onCreate(SQLiteDatabase db) {
        // 1- Create employees table; core authentication; role management
        db.execSQL("CREATE TABLE employees (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "email TEXT UNIQUE," +
                "password TEXT," +
                "is_admin INTEGER DEFAULT 0," + // 0 = employee, 1 = admin
                "is_locked INTEGER DEFAULT 0," +
                "last_login DATETIME," +
                "first_login INTEGER DEFAULT 1," + // 1 = not logged in yet TODO impelemtn first-time login password creation
                "employee_id INTEGER," + // store employee id i.e. link to api
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // 2- Employee details; personal and employment information
        db.execSQL("CREATE TABLE employee_details (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "employee_id INTEGER UNIQUE," +
                "full_name TEXT," +
                "department TEXT," +
                "salary DECIMAL(10,2)," +  // allows for large salaries with 2 decimal places
                "hire_date DATE," +         // for calculating salary increments
                "last_increment_date DATE," + // track when 5% increases occurred
                "annual_leave_allowance INTEGER DEFAULT 30," + // standard 30 days
                "used_leave INTEGER DEFAULT 0," +             // track used days
                "FOREIGN KEY(employee_id) REFERENCES employees(id))");

        // 3- Leave requests; manage holiday bookings
        db.execSQL("CREATE TABLE leave_requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "employee_id INTEGER," +
                "employee_name TEXT," +
                "start_date DATE," +
                "end_date DATE," +
                "days_requested INTEGER," +
                "reason TEXT," +
                "status TEXT DEFAULT 'pending'," + // pending, approved, denied
                "admin_response TEXT," +           // optional feedback from admin
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(employee_id) REFERENCES employees(id))");

        // 4- Notification preferences; manage employee notification settings
        db.execSQL("CREATE TABLE notification_preferences (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "employee_id INTEGER UNIQUE," +
                "leave_notifications INTEGER DEFAULT 1," + // boolean-like integers
                "salary_notifications INTEGER DEFAULT 1," +
                "email_notifications INTEGER DEFAULT 1," +
                "FOREIGN KEY(employee_id) REFERENCES employees(id))");

        // 5- Notification log; store all notifications sent to employees
        db.execSQL("CREATE TABLE notification_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "employee_id INTEGER," +
                "title TEXT," +
                "message TEXT," +
                "type TEXT," +  // i.e. leave_request, salary_update, security_alert, etc.
                "is_read INTEGER DEFAULT 0," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(employee_id) REFERENCES employees(id))");

        // 6- Pending notifications; store notifications to be sent on next login
        db.execSQL("CREATE TABLE pending_notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "employee_id INTEGER," +
                "title TEXT," +
                "message TEXT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "is_read INTEGER DEFAULT 0)");


        Log.d("StaffDataService", "Database tables created successfully");


        // insert default admin account
        try {
            ContentValues adminValues = new ContentValues();
            adminValues.put("email", "admin@staffsync.com");
            adminValues.put("password", hashPassword("admin123"));
            adminValues.put("is_admin", 1);
            long adminInsertResult = db.insertOrThrow("employees", null, adminValues);
            Log.d("StaffDataService", "Default admin account created with ID: " + adminInsertResult);
        } catch (Exception e) {
            Log.e("", "Error creating default admin: " + e.getMessage());
        }
        long employeeInsertResult = -1; // set as invalid initially to ensure it's recognised as such if not activated
        // add test employees with salary info
        try {
            ContentValues employeeDetails = new ContentValues();
            employeeDetails.put("employee_id", employeeInsertResult);
            employeeDetails.put("full_name", "Test Employee");
            employeeDetails.put("department", "IT");
            employeeDetails.put("salary", 45000.00);
            employeeDetails.put("hire_date", "2023-06-15");
            employeeDetails.put("last_increment_date", "2023-06-15");
            db.insertOrThrow("employee_details", null, employeeDetails);

            ContentValues employee2 = new ContentValues();
            employee2.put("full_name", "Emma Wilson");
            employee2.put("department", "HR");
            employee2.put("salary", 52000.00);
            employee2.put("hire_date", "2023-01-20");
            employee2.put("last_increment_date", "2023-01-20");
            db.insertOrThrow("employee_details", null, employee2);

            Log.d("StaffDataService", "Test employees added successfully");
        } catch (Exception e) {
            Log.e("StaffDataService", "Error adding test employees: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { // upgrade scheme by adding new column to satisfy SQLiteOpenHelper's abstract class requirements, otherwise it won't compile
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE employees ADD COLUMN first_login INTEGER DEFAULT 1");
        }
    }

    // ADMIN-SIDE ---

    public void addDefaultAdmin() {
        try {
            ContentValues values = new ContentValues();
            values.put("email", "admin@staffsync.com");
            values.put("password", hashPassword("admin123"));
            values.put("is_admin", 1);
            db.insertOrThrow("employees", null, values);
            Log.d(TAG, "default admin added successfully");
        } catch (Exception e) {
            Log.e(TAG, "failed to add default admin", e);
        }
    }

    public boolean verifyAdminLogin(String email, String password) {
        Cursor cursor = db.query( // create admin login cursor
                "employees", // table name
                new String[]{"password", "is_admin"}, // columns to get
                "email = ?", // where clause TODO: make emails unique
                new String[]{email},
                null, null, null // no grouping, having or sorting
        );

        try { // try to verify login
            if (cursor.moveToFirst()) {
                String storedPass = cursor.getString(0);
                int isAdmin = cursor.getInt(1);
                boolean loginSuccess = isAdmin == 1 && hashPassword(password).equals(storedPass); // verify admin status and match password

                // set login status if successful
                if (loginSuccess) {
                    setAdminLoggedIn(true);
                }

                return loginSuccess;
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    public void storeAdminNotification(String employeeName, String startDate, String endDate, String reason) { // store admin notifications in database with respective employee details and leave request details
        ContentValues values = new ContentValues();
        String title = "New Leave Request";
        String message = String.format("%s requests leave from %s to %s\nReason: %s",
                employeeName, startDate, endDate, reason);

        values.put("employee_id", 0); // mark as admin notification
        values.put("title", title);
        values.put("message", message);
        values.put("is_read", 0);
        values.put("employee_id", 0); // 0 = admin notification
        values.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(new Date()));

        db.insert("pending_notifications", null, values);
    }

    public void getPendingLeaveRequests(LeaveRequestCallback callback) {
        List<LeaveRequest> requests = new ArrayList<>();
        Cursor cursor = db.query(
                "leave_requests",
                null,
                "status = ?",
                new String[]{"pending"},
                null, null,
                "created_at DESC"
        );

        try {
            while (cursor.moveToNext()) {
                requests.add(new LeaveRequest(
                        cursor.getInt(cursor.getColumnIndex("id")),
                        cursor.getInt(cursor.getColumnIndex("employee_id")),
                        cursor.getString(cursor.getColumnIndex("employee_name")),
                        cursor.getString(cursor.getColumnIndex("start_date")),
                        cursor.getString(cursor.getColumnIndex("end_date")),
                        cursor.getString(cursor.getColumnIndex("reason")),
                        cursor.getInt(cursor.getColumnIndex("days_requested"))
                ));
            }
            callback.onComplete(requests, null);
        } catch (Exception e) {
            callback.onComplete(null, e.getMessage());
        } finally {
            cursor.close();
        }
    }

    public void logoutAdmin() {
        // clear admin session from shared preferences
        setAdminLoggedIn(false);

        // clear any stored credentials
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Log.d(TAG, "admin logged out and session cleared");
    }

    public boolean isAdminLoggedIn() { // i.e. check if admin is logged in
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ADMIN_LOGGED_IN, false);
    }

    public void setAdminLoggedIn(boolean status) { // set admin login status
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ADMIN_LOGGED_IN, status)
                .apply();
        Log.d(TAG, "admin login status updated: " + status);
    }

    public void storeBroadcastNotification(String title, String message) { // store broadcast notifications in database
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("is_read", 0);
        // no need to set employeeId for general broadcast
        values.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(new Date()));

        getWritableDatabase().insert("pending_notifications", null, values);
    }

    public void checkPendingNotifications(Context context) {
        Log.d(TAG, "Ensure pending notification is called...");
        NotificationService notificationService = new NotificationService(context);

        if (isAdminLoggedIn()) {
            // Only get admin notifications (where employee_id = 0)
            Cursor cursor = getReadableDatabase().query(
                    "pending_notifications",
                    null,
                    "employee_id = 0 AND is_read = 0", // Only admin notifications
                    null,
                    null, null,
                    "created_at DESC"
            );

            Log.d(TAG, "Found " + cursor.getCount() + " pending admin notifications");
            notificationService.showNotificationsFromCursor(cursor, "admin_channel");
        } else {
            // Employee notifications
            SharedPreferences prefs = context.getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
            int employeeId = prefs.getInt("logged_in_employee_id", -1);
            Log.d(TAG, "Current logged in employee ID: " + employeeId);

            if (employeeId != -1) {
                Cursor cursor = getReadableDatabase().query(
                        "pending_notifications",
                        null,
                        "employee_id = ? AND is_read = 0",
                        new String[]{String.valueOf(employeeId)},
                        null, null,
                        "created_at DESC"
                );

                Log.d(TAG, "Found " + cursor.getCount() + " pending notifications for employee: " + employeeId);
                notificationService.showNotificationsFromCursor(cursor, "holiday_channel");
            }
        }
    }

    private boolean adminExists() {
        Cursor cursor = db.query("Employees", null,
                "is_admin = 1", null, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // [ ]
    public boolean unlockEmployeeAccount(String email) { // account management methods
        ContentValues values = new ContentValues();
        values.put("is_locked", 0);
        return db.update("employees", values, "email = ?", new String[]{email}) > 0;
    }
    // [ ]
    public boolean lockEmployeeAccount(String email) { // method to lock Employee account
        ContentValues values = new ContentValues();
        values.put("is_locked", 1); // set is_locked to 1(success)
        return db.update("employees", values, "email = ?", new String[]{email}) > 0;
    }

    public void createEmployeeAccount(int employeeId, String email) {
        String tempPassword = generateTempPassword(employeeId);
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("password", hashPassword(tempPassword));
        values.put("is_admin", 0);
        values.put("first_login", 1);
        values.put("employee_id", employeeId); // store employee ID for linking

        try {
            db.insertOrThrow("Employees", null, values);
            Log.d(TAG, "Created Employee account for employee " + employeeId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create employee account: " + e.getMessage());
        }
    }


    // EMPLOYEE-SIDE ---

    public void checkEmployeeNotifications(Context context, int employeeId) {
        NotificationService notificationService = new NotificationService(context);
        Cursor cursor = db.query(
                "pending_notifications",
                null,
                "employee_id = ? AND is_read = 0",
                new String[]{String.valueOf(employeeId)},
                null, null, null
        );

        try {
            while (cursor != null && cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndex("title"));
                String message = cursor.getString(cursor.getColumnIndex("message"));
                int notificationId = cursor.getInt(cursor.getColumnIndex("id"));

                // Create notification intent
                PendingIntent pendingIntent = new NavDeepLinkBuilder(context)
                        .setGraph(R.navigation.nav_graph)
                        .setDestination(R.id.employee_navigation_home)
                        .createPendingIntent();

                // Build and show notification
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "holiday_channel")
                        .setSmallIcon(R.drawable.bell_icon)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notificationId, builder.build());

                // mark as read
                ContentValues values = new ContentValues();
                values.put("is_read", 1);
                db.update("pending_notifications", values,
                        "id = ?", new String[]{String.valueOf(notificationId)});
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void storeEmployeeNotification(int employeeId, boolean isApproved, String adminMessage) {
        Log.d(TAG, "Storing notification for employee: " + employeeId);

        ContentValues values = new ContentValues();
        String title = "Leave Request " + (isApproved ? "Approved" : "Denied");
        String message = String.format("Your leave request has been %s. %s",
                isApproved ? "approved" : "denied",
                adminMessage != null ? "\nMessage: " + adminMessage : "");

        values.put("employee_id", employeeId);
        values.put("title", title);
        values.put("message", message);
        values.put("is_read", 0);
        values.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(new Date()));

        long result = db.insert("pending_notifications", null, values); // testing
        Log.d(TAG, "Notification stored with result: " + result);
    }

    public Cursor getBroadcastNotifications() {
        return getReadableDatabase().query(
                "pending_notifications",
                null,
                "employee_id IS NULL AND is_read = 0",  // broadcast messages have no specific employee_id
                null,
                null,
                null,
                "created_at DESC"
        );
    }

    public long submitLeaveRequest(int employeeId, String startDate, String endDate, String reason) {
        int daysRequested = calculateDaysRequested(startDate, endDate);
        int remainingDays = getRemainingLeaveDays(employeeId);

        if (daysRequested > remainingDays) {
            return -2; // code for insufficient days
        }

        ContentValues values = new ContentValues();
        values.put("employee_id", employeeId);
        values.put("start_date", startDate);
        values.put("end_date", endDate);
        values.put("reason", reason);
        values.put("status", "pending");
        values.put("created_at", getCurrentDate());
        values.put("days_requested", daysRequested);

        // get employee name
        String employeeName = getEmployeeNameById(employeeId);
        if (employeeName != null) {
            values.put("employee_name", employeeName);
        }

        try {
            return db.insert("leave_requests", null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error submitting leave request: " + e.getMessage());
            return -1;
        }
    }

    public void logoutEmployee() {
        setEmployeeLoggedIn(false);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_EMPLOYEE_LOGGED_IN)
                .apply();
        Log.d(TAG, "Employee logged out and session cleared");
    }

    public void setEmployeeLoggedIn(boolean status) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_EMPLOYEE_LOGGED_IN, status)
                .apply();
        Log.d(TAG, "Employee login status updated: " + status);
    }

    public int verifyEmployeeLogin(String email, String password) {
        Log.d(TAG, "Attempting employee login for: " + email);

        Cursor cursor = db.query(
                "employees",
                new String[]{"password", "is_admin", "employee_id", "first_login"},
                "email = ?",
                new String[]{email},
                null, null, null
        );

        try {
            if (cursor.moveToFirst()) {
                String storedPass = cursor.getString(0);
                int isAdmin = cursor.getInt(1);
                int empId = cursor.getInt(2);
                int firstLogin = cursor.getInt(3);

                String hashedAttempt = hashPassword(password);
                boolean passwordMatch = hashedAttempt.equals(storedPass);

                if (passwordMatch && isAdmin == 0) {
                    // Store employee ID
                    SharedPreferences prefs = context.getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
                    prefs.edit().putInt("logged_in_employee_id", empId).apply();

                    // Check notifications when login successful
                    checkEmployeeNotifications(context, empId);

                    return firstLogin == 1 ? 1 : 2;
                }
            }
            return 0; // failure
        } finally {
            cursor.close();
        }
    }

    // HELPER METHODS ---

    private String getEmployeeNameById(int employeeId) {
        Cursor cursor = db.query(
                "employee_details",
                new String[]{"full_name"},
                "employee_id = ?",
                new String[]{String.valueOf(employeeId)},
                null, null, null
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public Cursor CursorLeaveRequests (int employeeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM leave_requests WHERE employee_id = ?";
        String[] selectionArgs = {String.valueOf(employeeId)};
        return db.rawQuery(query, selectionArgs);
    }

    public int getRemainingLeaveDays(int employeeId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // get used and pending days
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(days_requested), 0) as pending_days " +
                        "FROM leave_requests " +
                        "WHERE employee_id = ? AND status = 'pending'",
                new String[]{String.valueOf(employeeId)}
        );

        int pendingDays = 0;
        if (cursor != null && cursor.moveToFirst()) {
            pendingDays = cursor.getInt(0);
            cursor.close();
        }

        // get approved days
        cursor = db.query(
                "employee_details",
                new String[]{"annual_leave_allowance", "used_leave"},
                "employee_id = ?",
                new String[]{String.valueOf(employeeId)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int allowance = cursor.getInt(cursor.getColumnIndex("annual_leave_allowance"));
            int usedDays = cursor.getInt(cursor.getColumnIndex("used_leave"));
            cursor.close();
            return allowance - usedDays - pendingDays;
        }
        return 0;
    }

    public String generateTempPassword(int employeeId) {
        return TEMP_PASSWORD_PREFIX + employeeId;
    }

    public String hashPassword(String password) { // public password hashing method
        return Base64.encodeToString(password.getBytes(), Base64.DEFAULT);
    }

    // SALARY INCREMENT METHODS ---

    public void checkAndApplySalaryIncrements() {
        db.beginTransaction();
        try {
            // only get employees who haven't had an increment in the last year
            Cursor cursor = db.rawQuery(
                    "SELECT id, salary, full_name FROM employee_details " +
                            "WHERE CAST(JULIANDAY('now') - JULIANDAY(COALESCE(last_increment_date, hire_date)) AS INTEGER) >= 365 " +
                            "AND (last_increment_date IS NULL OR JULIANDAY(last_increment_date) < JULIANDAY('now') - 365)",
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    double currentSalary = cursor.getDouble(1);
                    double newSalary = currentSalary * 1.05; // 5% increase

                    ContentValues values = new ContentValues();
                    values.put("salary", newSalary);
                    values.put("last_increment_date", getCurrentDate());

                    db.update("employee_details", values, "id = ?",
                            new String[]{String.valueOf(id)});

                } while (cursor.moveToNext());
                cursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int getEmployeeUsedLeave(int employeeId) {
        int usedLeave = 0;
        Cursor cursor = db.query(
                "employee_details",
                new String[]{"used_leave"},
                "employee_id = ?",
                new String[]{String.valueOf(employeeId)},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            usedLeave = cursor.getInt(0);
        }
        cursor.close();
        return usedLeave;
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(new Date());
    }

    // LEAVE-REQUEST MANAGEMENT METHODS

    private int calculateDaysRequested(String startDate, String endDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
            Date start = format.parse(startDate);
            Date end = format.parse(endDate);
            return (int) ((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating days: " + e.getMessage());
            return 0;
        }
    }

    public void updateLeaveRequestStatus(int requestId, String status, String response, StatusUpdateCallback callback) {
        db.beginTransaction();
        try {
            // Get request details
            Cursor request = db.query(
                    "leave_requests",
                    new String[]{"employee_id", "days_requested", "status"},
                    "id = ?",
                    new String[]{String.valueOf(requestId)},
                    null, null, null
            );

            if (!request.moveToFirst()) {
                request.close();
                callback.onSuccess(false);
                return;
            }

            int employeeId = request.getInt(0);
            int daysRequested = request.getInt(1);
            String currentStatus = request.getString(2);
            request.close();

            // only update used_leave if request is being approved
            if (status.equals("approved") && !currentStatus.equals("approved")) {
                Cursor employee = db.query(
                        "employee_details",
                        new String[]{"used_leave"},
                        "employee_id = ?",
                        new String[]{String.valueOf(employeeId)},
                        null, null, null
                );

                if (employee.moveToFirst()) {
                    int currentUsedLeave = employee.getInt(0);
                    ContentValues employeeValues = new ContentValues();
                    employeeValues.put("used_leave", currentUsedLeave + daysRequested);
                    db.update("employee_details", employeeValues,
                            "employee_id = ?", new String[]{String.valueOf(employeeId)});
                }
                employee.close();
            }

            // Update request status
            ContentValues values = new ContentValues();
            values.put("status", status);
            values.put("admin_response", response);
            values.put("updated_at", getCurrentDate());

            int updated = db.update("leave_requests", values,
                    "id = ?", new String[]{String.valueOf(requestId)});

            db.setTransactionSuccessful();
            callback.onSuccess(updated > 0);

        } catch (Exception e) {
            Log.e(TAG, "Error updating leave request: " + e.getMessage());
            callback.onSuccess(false);
        } finally {
            db.endTransaction();
        }
    }

    // TESTING METHODS: OFF ---

    public void testSalaryIncrement(int employeeId) {
        // Get current salary
        Cursor beforeCursor = db.query(
                "employee_details",
                new String[]{"salary", "hire_date", "last_increment_date"},
                "employee_id = ?",
                new String[]{String.valueOf(employeeId)},
                null, null, null
        );

        if (beforeCursor.moveToFirst()) {
            double oldSalary = beforeCursor.getDouble(0);
            Log.d("SalaryTest", "Before increment: £" + oldSalary);

            // Run increment
            checkAndApplySalaryIncrements();

            // Check new salary
            Cursor afterCursor = db.query(
                    "employee_details",
                    new String[]{"salary", "last_increment_date"},
                    "employee_id = ?",
                    new String[]{String.valueOf(employeeId)},
                    null, null, null
            );

            if (afterCursor.moveToFirst()) {
                double newSalary = afterCursor.getDouble(0);
                Log.d("SalaryTest", "After increment: £" + newSalary);
                Log.d("SalaryTest", "Increase: £" + (newSalary - oldSalary));
            }
            afterCursor.close();
        }
        beforeCursor.close();
    }
}