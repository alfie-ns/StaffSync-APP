package com.example.staffsyncapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import org.json.JSONObject;

import com.example.staffsyncapp.api.ApiDataService;


public class OfflineSyncManager {
    private static final String TAG = "OfflineSyncManager";
    private static final int MAX_RETRIES = 3;

    private final Context context;
    private final SQLiteDatabase db;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isNetworkAvailable = new MutableLiveData<>();
    private final ApiDataService apiService;

    public OfflineSyncManager(Context context, SQLiteDatabase db, ApiDataService apiService) {
        this.context = context;
        this.db = db;
        this.apiService = apiService;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        startNetworkMonitoring();
    }

    /**
     * Start monitoring network connectivity:
     * - Registers a NetworkCallback to listen for connectivity changes.
     * - Posts updates to LiveData (`isNetworkAvailable`) to notify observers (e.g., UI components).
     * - Automatically triggers processing of the offline queue when the network becomes available.
     *
     * @see com.example.staffsyncapp.MainActivity
     * - This activity observes LiveData to reflect real-time connectivity status in the UI.
     */
    private void startNetworkMonitoring() {
        NetworkRequest request = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(request, new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                isNetworkAvailable.postValue(true);
                processQueue();
            }

            @Override
            public void onLost(Network network) {
                isNetworkAvailable.postValue(false);
            }
        });
    }

    /**
     * Add a new task to the offline processing queue:
     * - Task data is stored in a SQLite database for durability and persistence across app restarts.
     * - Data includes the task payload (JSON) and metadata such as retry attempts.
     * - If the network is currently available, triggers immediate processing of the queue.
     *
     * @param data The task payload, represented as a JSON object.
     */
    public void enqueueTask(JSONObject data) {
        ContentValues values = new ContentValues();
        values.put("request_data", data.toString());
        values.put("attempts", 0);

        long id = db.insert("offline_request_queue", null, values);
        Log.d(TAG, "Task queued: ID=" + id + ", Data=" + data.toString());

        if (isNetworkConnected()) {
            processQueue();
        }
    }

    /**
     * Process tasks stored in the offline request queue:
     * - Retrieves pending tasks from the database, prioritised by their creation time.
     * - Utilises the ApiService to handle each task (e.g., sending it to a server).
     * - Handles transient failures by incrementing retry attempts and retaining the task.
     * - Removes tasks that exceed the maximum retry limit (`MAX_RETRIES`).

     * Error Handling:
     * - Logs detailed errors for debugging.
     * - Updates the database to reflect task state (e.g., incremented retries or task removal).
     */
    public void processQueue() {
        Log.d(TAG, "Processing offline request queue...");

        Cursor cursor = db.query(
                "offline_request_queue",
                null,
                "attempts < ?",
                new String[]{String.valueOf(MAX_RETRIES)},
                null,
                null,
                "created_at ASC"
        );

        while (cursor.moveToNext()) {
            String dataStr = cursor.getString(cursor.getColumnIndex("request_data"));
            int id = cursor.getInt(cursor.getColumnIndex("id"));

            try {
                JSONObject data = new JSONObject(dataStr);
                apiService.processQueuedTask(data);

                db.delete("offline_request_queue", "id = ?",
                        new String[]{String.valueOf(id)});
                Log.d(TAG, "Task processed successfully: ID=" + id);

            } catch (Exception e) {
                Log.e(TAG, "Error processing task: " + e.getMessage());

                // Increment attempts and update in queue
                int attempts = cursor.getInt(cursor.getColumnIndex("attempts"));
                if (attempts < MAX_RETRIES) {
                    updateAttempts(id, attempts);
                } else {
                    removeFromQueue(id);
                }
            }
        }
        cursor.close();
    }

    /**
     * Check if network is connected
     * @return boolean (true if connected, false otherwise)
     */
    private boolean isNetworkConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Increment the retry attempts for a task:
     * - Updates the `attempts` column in the SQLite database for a specified task.
     *
     * @param id The unique identifier of the task in the database.
     * @param attempts The current number of retry attempts.
     */
    private void updateAttempts(int id, int attempts) {
        ContentValues values = new ContentValues();
        values.put("attempts", attempts + 1);
        db.update("offline_request_queue", values, "id = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Remove a task from the offline queue:
     * - Deletes the specified task from the SQLite database; free resources.
     *
     * @param id The unique identifier of the task in the database.
     */
    private void removeFromQueue(int id) {
        db.delete("offline_request_queue", "id = ?", new String[]{String.valueOf(id)});
    }
}