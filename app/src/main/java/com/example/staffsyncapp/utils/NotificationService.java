package com.example.staffsyncapp.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.navigation.NavDeepLinkBuilder;
import android.content.Intent;

import com.example.staffsyncapp.MainActivity;
import com.example.staffsyncapp.R;

/**
 * Handles push notification logic for the StaffSync employee management app.

 * Manages three notification channels:
 * - Admin channel: High priority, red LED for leave requests
 * - Holiday channel: High priority, blue LED for request updates

 * @since 1.0
 * @see android.app.NotificationManager
 * @see androidx.core.app.NotificationCompat
 */

public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final String SYSTEM_CHANNEL = "system_channel";
    private final Context context;
    private final NotificationManager notificationManager;
    private LocalDataService dbHelper;
    
    private static final String HOLIDAY_CHANNEL = "holiday_channel";
    private static final String ADMIN_CHANNEL = "admin_channel";
    private static final String NOTIFICATION_PREFS = "notification_preferences";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";


    private static final int ADMIN_NOTIFICATION_ID = 1000;
    private static final int HOLIDAY_NOTIFICATION_ID = 2000;
    // was thinking I was going to use more IDs for different notifications, but I didn't end up needing them

    // destinations if notification is clicked
    private static final int DESTINATION_ADMIN_REQUESTS = R.id.admin_leave_requests_fragment;
    private static final int DESTINATION_EMPLOYEE_HISTORY = R.id.employee_navigation_home;

    private PendingIntent createPendingIntent(int destination, boolean isAdmin) {
        NavDeepLinkBuilder builder = new NavDeepLinkBuilder(context)
                .setGraph(R.navigation.nav_graph);
        
        if (isAdmin) {
            builder.setDestination(DESTINATION_ADMIN_REQUESTS);
        } else {
            builder.setDestination(DESTINATION_EMPLOYEE_HISTORY);
        }
        
        return builder.createPendingIntent();
    }


    /**
     * Creates notification service and initialises channels
     * 
     * @param context Application context for notification manager
     */
    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.dbHelper = new LocalDataService(context);
        createNotificationChannels();
    }

    /**
     * Creates notification channels for admin and employee notifications
     *
     * @throws IllegalStateException: if SDK version is below Android O (API 26)
     */
    private void createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Admin channel for receiving leave requests
            NotificationChannel adminChannel = new NotificationChannel(
                    ADMIN_CHANNEL,
                    "Leave Requests",
                    NotificationManager.IMPORTANCE_HIGH
            );
            adminChannel.setDescription("Notifications for new leave requests");
            adminChannel.enableVibration(true);
            adminChannel.setLightColor(Color.RED);

            // Employee channel for request updates
            NotificationChannel holidayChannel = new NotificationChannel(
                    HOLIDAY_CHANNEL,
                    "Leave Request Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            holidayChannel.setDescription("Updates on your leave requests");
            holidayChannel.enableVibration(true);
            holidayChannel.setLightColor(Color.CYAN);

            // create channels
            notificationManager.createNotificationChannel(adminChannel);
            notificationManager.createNotificationChannel(holidayChannel);
        }
    }

    /**
    * Sends leave request notification to admin
    * 
    * @param employeeName Name of requesting employee
    * @param startDate Start date of leave
    * @param endDate End date of leave  
    * @param reason Leave request reason
    *
    * Store notification in database; fetch and show if admin logs in
    */
    public void sendLeaveRequestToAdmin(String employeeName, String startDate, String endDate, String reason) {
        // store notification in DB first
        LocalDataService dbHelper = new LocalDataService(context);
        dbHelper.storeAdminNotification(employeeName, startDate, endDate, reason);

        // get current logged in employee ID to make sure sender doesn't get notification
        SharedPreferences prefs = context.getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int loggedInEmployeeId = prefs.getInt("logged_in_employee_id", -1);

        // only show if admin is logged in
        if (dbHelper.isAdminLoggedIn() && !employeeName.contains(String.valueOf(loggedInEmployeeId))) {
            showAdminNotification(employeeName, startDate, endDate, reason);
        }
    }

    /**
     * Show admin notification for leave request
     * @param employeeName
     * @param startDate
     * @param endDate
     * @param reason
     */
    private void showAdminNotification(String employeeName, String startDate, String endDate, String reason) {
        PendingIntent pendingIntent = createPendingIntent(DESTINATION_ADMIN_REQUESTS, true);

        String title = "New Leave Request";
        String message = String.format("%s requests leave from %s to %s\nReason: %s",
                employeeName, startDate, endDate, reason);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ADMIN_CHANNEL) // use admin channel thus high priority; red LED, bell_icon
                .setSmallIcon(R.drawable.bell_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(ADMIN_NOTIFICATION_ID, builder.build()); // notify admin
    }

    /**
     * Check if notifications are enabled for employee
     * @param employeeId
     * @return preference value
     */
    public boolean isNotificationsEnabled(int employeeId) {
        SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED + "_" + employeeId, true);
    }

    /**
     * Set notifications enabled/disabled for employee
     * @param employeeId
     * @param enabled
     */
    public void setNotificationsEnabled(int employeeId, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED + "_" + employeeId, enabled)
                .apply();
    }
    
    /**
     * Send holiday notification to employee if notifications enabled
     * @param employeeId
     * @param title
     * @param message
     */
    public void sendHolidayNotification(int employeeId, String title, String message) {

        // get current logged in user type
        LocalDataService dbHelper = new LocalDataService(context);
        boolean isAdminLoggedIn = dbHelper.isAdminLoggedIn();

        // get current logged in employee ID
        SharedPreferences prefs = context.getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int loggedInEmployeeId = prefs.getInt("logged_in_employee_id", -1);

        /* Only send notification if:
           1- Current user is NOT admin AND 2-
           2- Logged in employee matches target employee
           3- If notifications is enabled
        */
        if (!dbHelper.isAdminLoggedIn() && loggedInEmployeeId == employeeId && isNotificationsEnabled(employeeId)) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, HOLIDAY_CHANNEL)
                    .setSmallIcon(R.drawable.bell_icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            try {
                if (ActivityCompat.checkSelfPermission(context,
                        android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    int notificationId = HOLIDAY_NOTIFICATION_ID + employeeId;
                    notificationManager.notify(notificationId, builder.build());
                    Log.d(TAG, "Holiday notification sent to employee: " + employeeId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to send holiday notification: " + e.getMessage());
            }
        }
    }

    /**
     * Send broadcast message from admin to all employees
     * @param title
     * @param message
     */
    public void sendAdminBroadcastMessage(String title, String message) { // General broadcast
        Log.d(TAG, "Attempting to store broadcast message: " + title);
        // store notification in DB first
        dbHelper.storeBroadcastNotification(title, message);
        Log.d(TAG, "Broadcast message stored successfully");

        // if user(someone that's not an admin) is logged in, show notification
        if (!dbHelper.isAdminLoggedIn()) {
            Log.d(TAG, "Non-admin logged in, showing broadcast");
            showNotificationsFromCursor(dbHelper.getBroadcastNotifications(), SYSTEM_CHANNEL);
        } else {
            Log.d(TAG, "Admin logged in, not showing broadcast");
        }

        
    }

    /**
     * Show system notifications from cursor
     * @param cursor: database cursor data: title, message and ids
     * @param channel: channel i.e. Holiday
     */
    public void showNotificationsFromCursor(Cursor cursor, String channel) {
        Log.d(TAG, "Showing notifications from cursor. Count: " + cursor.getCount());

        try { // iterate through cursor and show notifications
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndex("title"));
                String message = cursor.getString(cursor.getColumnIndex("message"));
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                int employeeId = cursor.getInt(cursor.getColumnIndex("employee_id"));

                if (!isNotificationsEnabled(employeeId)) {
                    Log.d(TAG, "Notifications disabled for employee: " + employeeId);
                    continue;
                }

                Log.d(TAG, "Processing notification: " + id + " for employee: " + employeeId);

                boolean isAdminNotification = employeeId == 0;
                    PendingIntent pendingIntent = createPendingIntent(
                        isAdminNotification ? DESTINATION_ADMIN_REQUESTS : DESTINATION_EMPLOYEE_HISTORY,
                        isAdminNotification
                );
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, HOLIDAY_CHANNEL)
                        .setSmallIcon(R.drawable.bell_icon)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                try {
                    if (ActivityCompat.checkSelfPermission(context,
                            android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        notificationManager.notify(id, builder.build());
                        Log.d(TAG, "Notification displayed: " + id);
                    } else {
                        Log.e(TAG, "Notification permission not granted");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to show notification: " + e.getMessage());
                }

                // mark as read
                ContentValues values = new ContentValues();
                values.put("is_read", 1);
                dbHelper.getWritableDatabase().update("pending_notifications",
                        values, "id = ?", new String[]{String.valueOf(id)});
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notifications: " + e.getMessage());
        } finally {
            cursor.close();
        }
    }

    /**
     * Show notification; i.e. leave request updates
     * @param employeeId: to send respective notification
     * @param title: notification title
     * @param message: notification message
     * @param notificationId: unique notification id
     */
    public void showNotification(int employeeId, String title, String message, int notificationId) {
        if (!isNotificationsEnabled(employeeId)) { // early-exit/kill function
            Log.d(TAG, "Notifications disabled for employee: " + employeeId);
            return;
        }
        PendingIntent pendingIntent = createPendingIntent(DESTINATION_EMPLOYEE_HISTORY, false);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, HOLIDAY_CHANNEL) // build nnofication with high priority; holiday channel
                .setSmallIcon(R.drawable.bell_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        if (ActivityCompat.checkSelfPermission(context, // ensure notifications permission have been granted
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) { // check if app has permission to post notifications
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification displayed for employee: " + employeeId);
        }
    }
}