package com.example.staffsyncapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ApiWorkerThread extends Thread {
    private static final String TAG = ApiWorkerThread.class.getSimpleName();
    private final Handler mainHandler;
    private final BlockingQueue<Runnable> taskQueue;
    private volatile boolean isRunning;

    /**
     * Background thread implementation for processing COMP2000 API requests,
     * using BlockingQueue for task management and Handler for main thread callbacks.
     *
     * @property mainHandler: Posts results back to UI thread
     * @property taskQueue: Stores pending API requests
     * @property isRunning: Controls thread lifecycle
     *
     * @method postToMainThread: Posts callback to UI thread
     * @method queueTask: Adds new API request to queue
     * @method run: Executes queued tasks in background
     * @method shutdown: Safely terminates thread
     *
     * @see ApiDataService: Uses this class for API operations
     **/

    public ApiWorkerThread() {
        mainHandler = new Handler(Looper.getMainLooper());
        taskQueue = new LinkedBlockingQueue<>();
        isRunning = true;
    }

    public void postToMainThread(Runnable task) {
        mainHandler.post(task);
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                Runnable task = taskQueue.take();
                if (task != null) {
                    task.run();
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Worker thread interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        // clean up remaining tasks
        taskQueue.clear();
    }

    public boolean queueTask(Runnable task) { // add a task to the queue if the worker thread is running
        if (task == null || !isRunning) {
            return false;
        }
        return taskQueue.offer(task);
    }

    public void shutdown() { // safely shutdown the worker thread
        isRunning = false;
        interrupt();
    }
}