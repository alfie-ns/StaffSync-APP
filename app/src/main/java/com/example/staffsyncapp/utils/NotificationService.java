package com.example.staffsyncapp.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

/*
 * NotificationService class is responsible for sending notifications to the employee;
 * it creates notification channels for holiday requests and system updates.
 * 
 * - Holiday notifications are high priority and have a blue light color.
 * - System notifications are default priority, non-urgent notifications.
 */

public class NotificationService {
    private static final String TAG = "NotificationService";
    private final Context context;
    private final NotificationManager notificationManager;
    
    private static final String HOLIDAY_CHANNEL = "holiday_channel";
    private static final String SYSTEM_CHANNEL = "system_channel";
    private static final int NOTIFICATION_ID = 1;

    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }
    // Create notification channels
    private void createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel holidayChannel = new NotificationChannel(
                    HOLIDAY_CHANNEL,
                    "Holiday Requests",
                    NotificationManager.IMPORTANCE_HIGH
            );
            holidayChannel.setDescription("Holiday request notifications");
            holidayChannel.enableVibration(true);
            holidayChannel.setLightColor(Color.BLUE);

            NotificationChannel systemChannel = new NotificationChannel(
                    SYSTEM_CHANNEL,
                    "System Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            systemChannel.setDescription("System notifications");

            notificationManager.createNotificationChannel(holidayChannel);
            notificationManager.createNotificationChannel(systemChannel);
        }
    }
    // Send holiday notification
    public void sendHolidayNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, HOLIDAY_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        try {
            if (ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send holiday notification", e);
        }
    }
    // Send system notification i.e. for emails, non-urgent messages
    public void sendSystemNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SYSTEM_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        try {
            if (ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send system notification", e);
        }
    }
}

// TODO: yet to also implement user-NotificationService thus BaseNotificationService [ ]