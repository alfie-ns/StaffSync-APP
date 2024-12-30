package com.example.staffsyncapp.leave;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.staffsyncapp.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.staffsyncapp.models.LeaveRequest;

import com.example.staffsyncapp.adapter.LeaveHistoryAdapter;
import com.example.staffsyncapp.utils.LocalDataService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LeaveHistoryDialog extends DialogFragment {
    private static final String TAG = "LeaveHistoryDialog";
    private LeaveHistoryAdapter adapter;
    private LocalDataService dbHelper;
    private RecyclerView recyclerView;
    private View progressBar;
    private View emptyView;
    private View dialogView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog);
        dbHelper = new LocalDataService(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 1- initialise dialog view first
        dialogView = inflater.inflate(R.layout.leave_history_dialog, container, false);

        // 2- initialise views second
        recyclerView = dialogView.findViewById(R.id.leave_history_recycler);
        progressBar = dialogView.findViewById(R.id.progress_bar);
        emptyView = dialogView.findViewById(R.id.empty_view);

        // 3- setup recyclerView and click listeners
        setupRecyclerView();
        setupClickListeners();
        loadLeaveHistory();

        return dialogView;
    }

    private void setupRecyclerView() {
        // 1- set layout manager and adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        // 2- divide items with a line
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                requireContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        adapter = new LeaveHistoryAdapter(requireContext());
        recyclerView.setAdapter(adapter);

        // 3- create and set layout params
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        recyclerView.setLayoutParams(params);
        recyclerView.setVisibility(View.VISIBLE);

    }

    private void setupClickListeners() {
        dialogView.findViewById(R.id.close_button).setOnClickListener(v -> dismiss());
    }

    /**
     * Load leave history from the database and display it in the RecyclerView.
     * 
     * This function is called when the dialog is created or when the user inadvertently refreshes the list.
     * TODO [ ]: refresh button to manually and purposefully refresh the list.
     * 
     * Steps:
     * 1- check for null views.
     * 2- retrieve the logged-in employee ID from SharedPreferences.
     * 3- fetch leave requests from the database.
     * 4- sort leave requests by date (newest first).
     * 5- update the adapter with the new requests and toggle views for empty or loading states.
     */
    private void loadLeaveHistory() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
        }
        if (emptyView != null) {
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }

            SharedPreferences prefs = requireContext().getSharedPreferences("employee_prefs",
                    Context.MODE_PRIVATE);
            int employeeId = prefs.getInt("logged_in_employee_id", -1); // default value is fail

            Log.d(TAG, "Loading leave history for employee: " + employeeId); // testing

            if (employeeId != -1) {
                List<LeaveRequest> requests = new ArrayList<>();
                Cursor cursor = null;

                try {
                    cursor = dbHelper.CursorLeaveRequests(employeeId);

                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            try {
                                LeaveRequest request = new LeaveRequest(
                                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                                        cursor.getInt(cursor.getColumnIndexOrThrow("employee_id")),
                                        cursor.getString(cursor.getColumnIndexOrThrow("employee_name")),
                                        cursor.getString(cursor.getColumnIndexOrThrow("start_date")),
                                        cursor.getString(cursor.getColumnIndexOrThrow("end_date")),
                                        cursor.getString(cursor.getColumnIndexOrThrow("reason")),
                                        cursor.getInt(cursor.getColumnIndexOrThrow("days_requested"))
                                );

                                request.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                                request.setAdminResponse(cursor.getString(cursor.getColumnIndexOrThrow("admin_response")));
                                request.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));

                                requests.add(request);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing request: " + e.getMessage());
                                continue;
                            }
                        } while (cursor.moveToNext());
                    }

                    // sort by date (newest first)
                    Collections.sort(requests, (r1, r2) -> {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                            return sdf.parse(r2.getCreatedAt()).compareTo(sdf.parse(r1.getCreatedAt()));
                        } catch (Exception e) {
                            return 0;
                        }
                    });

                    Log.d(TAG, "Loaded " + requests.size() + " leave requests");
                    Log.d(TAG, "Setting leave requests: " + requests.size());

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }

                            if (requests.isEmpty() && emptyView != null) {
                                emptyView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else if (recyclerView != null) {
                                recyclerView.setVisibility(View.VISIBLE);
                                emptyView.setVisibility(View.GONE);
                            }

                            if (adapter != null) {
                                adapter.setLeaveRequests(requests);
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error loading leave history", e);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Failed to load leave history",
                                        Toast.LENGTH_SHORT).show()
                        );
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }

    }

    /**
     * Releases references to free memory and prevents dialogView memory leaks
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        progressBar = null;
        emptyView = null;
        dialogView = null;
    }
}