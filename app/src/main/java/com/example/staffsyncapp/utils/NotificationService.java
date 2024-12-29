package com.example.staffsyncapp.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
 * - System channel: Default priority for general updates

 * @since 1.0
 * @see android.app.NotificationManager
 * @see androidx.core.app.NotificationCompat
 */

public class NotificationService {
    private static final String TAG = "NotificationService";
    private final Context context;
    private final NotificationManager notificationManager;
    
    private static final String HOLIDAY_CHANNEL = "holiday_channel";
    private static final String SYSTEM_CHANNEL = "system_channel";
    private static final int ADMIN_NOTIFICATION_ID = 1000;
    private static final String ADMIN_CHANNEL = "admin_channel";
    private static final int HOLIDAY_NOTIFICATION_ID = 2000;
    private static final int NOTIFICATION_ID = 3000;
    private static final int BASE_NOTIFICATION_ID = 4000;

    /**
     * Creates notification service and initialises channels
     * 
     * @param context Application context for notification manager
     */
    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
    }/**
     
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

        // only show if admin is logged in
        if (dbHelper.isAdminLoggedIn()) {
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
        PendingIntent pendingIntent = PendingIntent.getActivity(context, // create pending intent to open app on notification click
                0, 
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT // immutable flag for security
        );

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
    
    // Send holiday notification; i.e. for leave request updates ---
    public void sendHolidayNotification(int employeeId, String title, String message) {
        // offset notification ID with employeeId to handle multiple active notifications
        int notificationId = HOLIDAY_NOTIFICATION_ID + employeeId;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, HOLIDAY_CHANNEL)
                .setSmallIcon(R.drawable.bell_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            if (ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                Log.d(TAG, "Granting leave request notifications...");
            } else {
                Log.e(TAG, "Notification permission not granted.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send holiday notification.", e);
        }
    }
    
    // Send system notification i.e. for emails, non-urgent messages ---
    public void sendSystemNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SYSTEM_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        try { // check if phone has permission to send notifications
            if (ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send system notification", e);
        }
    }

    // Admin decision -> Employee ---
    public void sendRequestUpdateToEmployee(int employeeId, boolean isApproved, String adminMessage) {
        // store notification first
        LocalDataService dbHelper = new LocalDataService(context);
        dbHelper.storeEmployeeNotification(employeeId, isApproved, adminMessage);

        // only show if this employee is logged in
        SharedPreferences prefs = context.getSharedPreferences("employee_prefs", Context.MODE_PRIVATE);
        int loggedInEmployeeId = prefs.getInt("logged_in_employee_id", -1);

        boolean isAdmin = dbHelper.isAdminLoggedIn();

        if (employeeId == loggedInEmployeeId && !isAdmin) {
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    employeeId,
                    new Intent(context, MainActivity.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            String title = "Leave Request " + (isApproved ? "Approved" : "Denied");
            String message = String.format("Your leave request has been %s. %s",
                    isApproved ? "approved" : "denied",
                    adminMessage != null ? "\nMessage: " + adminMessage : "");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, HOLIDAY_CHANNEL)
                    .setSmallIcon(R.drawable.bell_icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            notificationManager.notify(HOLIDAY_NOTIFICATION_ID + employeeId, builder.build());
        }
    }

    // Send notification; utilised in showBroadcastNotification()
    private void sendNotification(int notificationId, android.app.Notification notification) {
        try {
            if (ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, notification);
                Log.d(TAG, "Sent notification: " + notificationId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send notification", e);
        }
    }

    // Admin General Broadcast ---

    public void sendAdminBroadcastMessage(String title, String message) {
        // Store notification in database
        LocalDataService dbHelper = new LocalDataService(context);
        dbHelper.storeBroadcastNotification(title, message);

        // show notification immediately
        showBroadcastNotification(title, message);
    }

    public void showBroadcastNotification(String title, String message) { // Create and display a broadcast notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ADMIN_CHANNEL)
                .setSmallIcon(R.drawable.bell_icon) // fetch bell_icon from drawable resources
                .setContentTitle("Admin Broadcast: " + title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        sendNotification(BASE_NOTIFICATION_ID + 3000, builder.build());
    }
}