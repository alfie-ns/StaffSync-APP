package com.example.staffsyncapp.admin;

import android.content.ContentValues;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.staffsyncapp.R;
import com.example.staffsyncapp.adapter.LeaveRequestAdapter;
import com.example.staffsyncapp.databinding.AdminHolidayRequestsFragmentBinding;
import com.example.staffsyncapp.models.LeaveRequest;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NotificationService;

import java.util.Date;
import java.util.Locale;

public class AdminLeaveRequestsFragment extends Fragment implements LeaveRequestAdapter.OnRequestActionListener {
    private static final String TAG = "AdminHolidayRequests";
    private AdminHolidayRequestsFragmentBinding binding;
    private LocalDataService dbHelper;
    private NotificationService notificationService;
    private LeaveRequestAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = AdminHolidayRequestsFragmentBinding.inflate(inflater, container, false);
        dbHelper = new LocalDataService(requireContext());
        notificationService = new NotificationService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        loadRequests();
    }

    private void setupUI() {// setup RecyclerView and click listeners
        adapter = new LeaveRequestAdapter(this);

        binding.backArrow.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.AdminDashboardFragment)); // before this clicking on the notification sent the user to the entry point before

        binding.requestsRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext()));


        binding.requestsRecyclerView.setAdapter(adapter);
    }

    private void loadRequests() { // fetch pending leave requests from SQLite
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);

        dbHelper.getPendingLeaveRequests((requests, error) -> { // get all pending leave requests
            if (getContext() == null) return;

            binding.progressBar.setVisibility(View.GONE);

            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                return;
            }

            if (requests == null || requests.isEmpty()) { // if no pending requests
                binding.emptyState.setVisibility(View.VISIBLE);
            } else {
                binding.pendingCount.setText(String.format("Pending Requests: %d", requests.size()));
                adapter.updateRequests(requests); // update adapter with new requests
            }
        });
    }
    /**
     * Both functions use dbHelper to send a approved or denial
     *
     * @param request LeaveRequest object
     * @return void
     * 
     * 1- output respective message in updateLeaveRequestStatus; thus employee is notified
     * 2- notify employee of the decision
     * 3- fetch pending leave requests from SQLite
     *
     */
    // Approve leave request
    // @param request LeaveRequest object
    @Override
    public void onApprove(LeaveRequest request) { // approve leave request
        Log.d(TAG, "Approving leave request: " + request.getId());
        dbHelper.updateLeaveRequestStatus(
                request.getId(),
                "approved",
                "Request approved by admin",
                success -> {
                    if (success) {
                        Log.d(TAG, "Leave request approved successfully");
                        notifyEmployee(request.getEmployeeId(), true);
                        loadRequests();
                    }
                    else {
                        Log.e(TAG, "Failed to approve leave request");
                    }
                }
        );
    }
    /**
     * Deny leave request
     * @param request LeaveRequest object
     */
    @Override
    public void onDeny(LeaveRequest request) { // deny leave request
        Log.d(TAG, "Denying leave request: " + request.getId());
        dbHelper.updateLeaveRequestStatus(
                request.getId(),
                "denied",
                "Request denied by admin",
                success -> {
                    if (success) {
                        Log.d(TAG, "Leave request denied successfully");
                        notifyEmployee(request.getEmployeeId(), false);
                        loadRequests();
                    }
                    else {
                        Log.e(TAG, "Failed to deny leave request");
                    }
                }
        );
    }

    private void notifyEmployee(int employeeId, boolean isApproved) {
        Log.d(TAG, "Sending response notification to employee: " + employeeId);
        String title = isApproved ? "Leave Request Approved" : "Leave Request Denied";
        String message = isApproved ?
                "Your leave request has been approved." :
                "Your leave request has been denied.";

        // Store notification in pending_notifications table
        ContentValues values = new ContentValues();
        values.put("employee_id", employeeId);
        values.put("title", title);
        values.put("message", message);
        values.put("is_read", 0);
        values.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK)
                .format(new Date()));

        dbHelper.getWritableDatabase().insert("pending_notifications", null, values);

        // Send Holiday push notification
        if (notificationService != null) {
            notificationService.sendHolidayNotification(employeeId, title, message);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}