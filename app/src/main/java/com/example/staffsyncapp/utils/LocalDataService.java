    package com.example.staffsyncapp.utils;

    import android.content.ContentValues;
    import android.content.Context;
    import android.database.Cursor;
    import android.database.sqlite.SQLiteDatabase;
    import android.database.sqlite.SQLiteOpenHelper;
    import android.util.Base64;
    import android.util.Log;

    import java.text.SimpleDateFormat;
    import java.util.Date;
    import java.util.Locale;

    /**
     * Database service handling local data storage and authentication for StaffSync.
     * Manages user accounts, employee records, leave requests and notifications using SQLite.
     * 
     * features:
     * - User authentication and session management
     * - Employee details and salary tracking
     * - Leave request processing
     * - Notification handling
     * - Automated salary increment management
     *
     * todo:
     * - [ ] leave request processing
     * - [ ] notification handling from user and admin
     * - [ ] automated salary increment management
     * - [ ] user account management
     * 
    */

    public class LocalDataService extends SQLiteOpenHelper {

        // 1. Constants and Variables ----------------------------------------------
        private static final String DATABASE_NAME = "staffsync.db";
        private static final int DATABASE_VERSION = 1; // starting db version
        private static final String TAG = "DatabaseHelper";

        private static Boolean isLoggedIn = false;  // tracks login state

        private static final String PREFS_NAME = "StaffSyncPrefs";
        private static final String KEY_ADMIN_LOGGED_IN = "admin_logged_in";

        private final Context context;
        private final SQLiteDatabase db;  // thread safety

        private static final String KEY_USER_LOGGED_IN = "user_logged_in";

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

        @Override // SQL DATABASE CREATION
        public void onCreate(SQLiteDatabase db) {
            // 1- Create Users table; core authentication; role management
            db.execSQL("CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "email TEXT UNIQUE," +
                    "password TEXT," +
                    "is_admin INTEGER DEFAULT 0," +  // 0 = employee, 1 = admin
                    "is_locked INTEGER DEFAULT 0," +  // for security lockouts
                    "last_login DATETIME," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

            // 2- Employee details; personal and employment information
            db.execSQL("CREATE TABLE employee_details (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER UNIQUE," +
                    "full_name TEXT," +
                    "department TEXT," +
                    "salary DECIMAL(10,2)," +  // allows for large salaries with 2 decimal places
                    "hire_date DATE," +         // for calculating salary increments
                    "last_increment_date DATE," + // track when 5% increases occurred
                    "annual_leave_allowance INTEGER DEFAULT 30," + // standard 30 days
                    "used_leave INTEGER DEFAULT 0," +             // track used days
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            // 3- Leave requests; manage holiday bookings
            db.execSQL("CREATE TABLE leave_requests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "employee_id INTEGER," +
                    "start_date DATE," +
                    "end_date DATE," +
                    "days_requested INTEGER," +
                    "reason TEXT," +
                    "status TEXT DEFAULT 'pending'," + // pending, approved, denied
                    "admin_response TEXT," +           // optional feedback from admin
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(employee_id) REFERENCES users(id))");

            // 4- Notification preferences; manage user notification settings
            db.execSQL("CREATE TABLE notification_preferences (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER UNIQUE," +
                    "leave_notifications INTEGER DEFAULT 1," + // boolean-like integers
                    "salary_notifications INTEGER DEFAULT 1," +
                    "email_notifications INTEGER DEFAULT 1," +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            // 5- Notification log; store all notifications sent to users
            db.execSQL("CREATE TABLE notification_log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "title TEXT," +
                    "message TEXT," +
                    "type TEXT," +  // ie leave_request, salary_update, security_alert, etc.
                    "is_read INTEGER DEFAULT 0," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            // 6- Salary Increment Creation
            db.execSQL("ALTER TABLE employee_details ADD COLUMN last_increment_date DATE");
            db.execSQL("ALTER TABLE employee_details ADD COLUMN next_increment_date DATE");

            Log.d("StaffDataService", "Database tables created successfully");


            // insert default admin account
            try {
                ContentValues adminValues = new ContentValues();
                adminValues.put("email", "admin@staffsync.com");
                adminValues.put("password", hashPassword("admin123"));
                adminValues.put("is_admin", 1);
                long adminInsertResult = db.insertOrThrow("users", null, adminValues);
                Log.d("StaffDataService", "Default admin account created with ID: " + adminInsertResult);
            } catch (Exception e) {
                Log.e("", "Error creating default admin: " + e.getMessage());
            }
            long userInsertResult = -1; // set as invalid initially to ensure it's recognised as such if not activated
            // Add test employees with salary info
            try {
                ContentValues employeeDetails = new ContentValues();
                employeeDetails.put("user_id", userInsertResult);
                employeeDetails.put("full_name", "Test User");
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
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        private boolean adminExists() {
            Cursor cursor = db.query("users", null,
                    "is_admin = 1", null, null, null, null);
            boolean exists = cursor.getCount() > 0;
            cursor.close();
            return exists;
        }

        public boolean verifyAdminLogin(String email, String password) {
            Cursor cursor = db.query( // create admin login cursor
                    "users",
                    new String[]{"password", "is_admin"},
                    "email = ?",
                    new String[]{email},
                    null, null, null // no sorting
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

        public boolean verifyUserLogin(String email, String password) {
            Cursor cursor = db.query(
                    "users",
                    new String[]{"password", "is_admin"},
                    "email = ?",
                    new String[]{email},
                    null, null, null
            );

            try {
                if (cursor.moveToFirst()) {
                    String storedPass = cursor.getString(0);
                    int isAdmin = cursor.getInt(1);

                    // check if user is not admin and password matches
                    return isAdmin == 0 && hashPassword(password).equals(storedPass);
                }
                return false;
            } finally {
                cursor.close();
            }
        }

        public String hashPassword(String password) { // public password hashing method
            return Base64.encodeToString(password.getBytes(), Base64.DEFAULT);
        }

        public boolean isAdminLoggedIn() { // i.e. check if admin is logged in
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_ADMIN_LOGGED_IN, false);
        }

        public void logoutAdmin() {
            // clear admin session from shared preferences
            setAdminLoggedIn(false);

            // clear any stored credentials (if you're storing any)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()  // removed all stored preferences
                    .apply();

            Log.d(TAG, "admin logged out and session cleared");
        }

        public void setAdminLoggedIn(boolean status) { // set admin login status
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_ADMIN_LOGGED_IN, status)
                    .apply();
            Log.d(TAG, "admin login status updated: " + status);
        }

        public void addDefaultAdmin() {
            try {
                ContentValues values = new ContentValues();
                values.put("email", "admin@staffsync.com");
                values.put("password", hashPassword("admin123"));
                values.put("is_admin", 1);
                db.insertOrThrow("users", null, values);
                Log.d(TAG, "default admin added successfully");
            } catch (Exception e) {
                Log.e(TAG, "failed to add default admin", e);
            }
        }

        public Cursor getEmployeeSalaryInfo() {
            return db.rawQuery(
            "SELECT full_name, salary, " +
                    "CAST(JULIANDAY('now') - JULIANDAY(COALESCE(last_increment_date, hire_date)) AS INTEGER) " +
                    "AS days_since_last_increment " +
                    "FROM employee_details " +
                    "ORDER BY days_since_last_increment DESC", null);
        }

        public void checkAndApplySalaryIncrements() {
            //  start transaction for batch processing
            db.beginTransaction();
            try {
                // Get eligible employees
                Cursor cursor = db.rawQuery(
                        "SELECT id, salary, full_name FROM employee_details " +
                                "WHERE CAST(JULIANDAY('now') - JULIANDAY(COALESCE(last_increment_date, hire_date)) AS INTEGER) >= 365",
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

        private String getCurrentDate() {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(new Date());
        }

        // 3. Leave Request Management Methods -------------------------------------------

        public long submitLeaveRequest(int employeeId, String startDate, String endDate, String reason) {
            // First check if employee has enough leave balance
            if (!hasEnoughLeaveBalance(employeeId, startDate, endDate)) {
                return -1; // Insufficient leave balance
            }

            ContentValues values = new ContentValues();
            values.put("employee_id", employeeId);
            values.put("start_date", startDate);
            values.put("end_date", endDate);
            values.put("reason", reason);
            values.put("status", "pending");
            values.put("days_requested", calculateDaysRequested(startDate, endDate));

            return db.insert("leave_requests", null, values);
        }

        private boolean hasEnoughLeaveBalance(int employeeId, String startDate, String endDate) {
            Cursor cursor = db.query(
                    "employee_details",
                    new String[]{"annual_leave_allowance", "used_leave"},
                    "user_id = ?",
                    new String[]{String.valueOf(employeeId)},
                    null, null, null
            );

            if (cursor.moveToFirst()) {
                int allowance = cursor.getInt(0);
                int used = cursor.getInt(1);
                int requested = calculateDaysRequested(startDate, endDate);
                cursor.close();
                return (allowance - used) >= requested;
            }
            cursor.close();
            return false;
        }

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

        public Cursor getUserLeaveRequests(int employeeId) {
            return db.query(
                    "leave_requests",
                    null,
                    "employee_id = ?",
                    new String[]{String.valueOf(employeeId)},
                    null, null,
                    "created_at DESC"
            );
        }

        public Cursor getPendingLeaveRequests() {
            return db.query(
                    "leave_requests",
                    null,
                    "status = ?",
                    new String[]{"pending"},
                    null, null,
                    "created_at ASC"
            );
        }

        public boolean updateLeaveRequestStatus(int requestId, String status, String adminResponse) {
            db.beginTransaction();
            try {
                // Get request details first
                Cursor request = db.query(
                        "leave_requests",
                        new String[]{"employee_id", "days_requested", "status"},
                        "id = ?",
                        new String[]{String.valueOf(requestId)},
                        null, null, null
                );

                if (!request.moveToFirst()) {
                    request.close();
                    return false;
                }

                // Only update used_leave if request is being approved
                if (status.equals("approved") && !request.getString(2).equals("approved")) {
                    int employeeId = request.getInt(0);
                    int daysRequested = request.getInt(1);

                    // Update employee's used leave
                    Cursor employee = db.query(
                            "employee_details",
                            new String[]{"used_leave"},
                            "user_id = ?",
                            new String[]{String.valueOf(employeeId)},
                            null, null, null
                    );

                    if (employee.moveToFirst()) {
                        int currentUsedLeave = employee.getInt(0);
                        ContentValues employeeValues = new ContentValues();
                        employeeValues.put("used_leave", currentUsedLeave + daysRequested);
                        db.update("employee_details", employeeValues,
                                "user_id = ?", new String[]{String.valueOf(employeeId)});
                    }
                    employee.close();
                }

                // Update request status
                ContentValues values = new ContentValues();
                values.put("status", status);
                values.put("admin_response", adminResponse);
                values.put("updated_at", getCurrentDate());

                db.update("leave_requests", values,
                        "id = ?", new String[]{String.valueOf(requestId)});

                request.close();
                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        }

        public boolean isUserLoggedIn() {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_USER_LOGGED_IN, false);
        }

        public void setUserLoggedIn(boolean status) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_USER_LOGGED_IN, status)
                    .apply();
            Log.d(TAG, "User login status updated: " + status);
        }

        public void logoutUser() {
            setUserLoggedIn(false);
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(KEY_USER_LOGGED_IN)
                    .apply();
            Log.d(TAG, "User logged out and session cleared");
        }

        public boolean unlockUserAccount(String email) { // account management methods
            ContentValues values = new ContentValues();
            values.put("is_locked", 0);
            return db.update("users", values, "email = ?", new String[]{email}) > 0;
        }

        public boolean lockUserAccount(String email) { // method to lock user account
            ContentValues values = new ContentValues();
            values.put("is_locked", 1); // set is_locked to 1(success)
            return db.update("users", values, "email = ?", new String[]{email}) > 0;
        }
        // TODO: EMPLOYEE-SIDE

        public int getLoggedInEmployeeId() {
            SQLiteDatabase db = this.getReadableDatabase();
            int employeeId = -1; //  value if no employee is logged in

            try {
                String query = "SELECT employee_id FROM user_sessions WHERE is_active = 1";
                Cursor cursor = db.rawQuery(query, null);

                if (cursor != null && cursor.moveToFirst()) {
                    employeeId = cursor.getInt(cursor.getColumnIndex("employee_id"));
                }

                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e("LocalDataService", "Error getting logged in employee ID: " + e.getMessage());
            }

            return employeeId;
        }
    }
