package com.example.staffsyncapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.staffsyncapp.R;
import com.example.staffsyncapp.models.LeaveRequest;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for managing leave/holiday requests in the admin dashboard's RecyclerView.

 * Core functionalities:
 * - displays employee leave requests with details like name, dates, reason
 * - provides approve/deny actions that trigger callbacks to AdminHolidayRequestFragment
 * - manages list of pending requests via updateRequests() method

 * Main responsibilities:
 *  * - Binds leave request data to the list item views
 *  * - Handles approve/deny button clicks and triggers callbacks
 *  * - Updates list when new data is provided

 * Implementation:
 * - uses ViewHolder pattern for efficient view recycling 
 * - binds LeaveRequest data to admin_holiday_request_item.xml layout
 * - communicates request actions via OnRequestActionListener interface

 * Typical usage in AdminHolidayRequestFragment:
 * - created with fragment as listener
 * - updated when new requests are fetched from SQLite
 * - triggers onApprove/onDeny callbacks when admin takes action

 * @see AdminHolidayRequestFragment for the fragment using this adapter
 * @see LeaveRequest for the data model class
 * @see LocalDataService for the SQlite database operations
 **/

/**
 * ---------------------------------------------------------------------------------------------------------
 **/

public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.ViewHolder> {
    private final List<LeaveRequest> requests = new ArrayList<>();
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onApprove(LeaveRequest request);
        void onDeny(LeaveRequest request);
    }

    public LeaveRequestAdapter(OnRequestActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override 
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_holiday_request_item, parent, false);
        return new ViewHolder(view);
    }

    @Override // override to bind LeaveRequest data to ViewHolder views at given position
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) { // bind to input data
        LeaveRequest request = requests.get(position);

        holder.employeeName.setText(request.getEmployeeName());
        holder.employeeId.setText(String.format("(ID: %d)", request.getEmployeeId()));
        holder.dateRange.setText(String.format("%s to %s",
                request.getStartDate(), request.getEndDate()));
        holder.daysRequested.setText(String.format("%d days", request.getDaysRequested()));
        holder.reason.setText(request.getReason());

        holder.approveButton.setOnClickListener(v -> listener.onApprove(request));
        holder.denyButton.setOnClickListener(v -> listener.onDeny(request));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void updateRequests(List<LeaveRequest> newRequests) {
        requests.clear();
        requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder { // Cache views from layout to avoid repeated findViewById() calls
        final TextView employeeName;
        final TextView employeeId;
        final TextView dateRange;
        final TextView daysRequested;
        final TextView reason;
        final MaterialButton approveButton;
        final MaterialButton denyButton;
        // initialise view references from layout XML using findViewById
        ViewHolder(View view) {
            super(view);
            employeeName = view.findViewById(R.id.employee_name);
            employeeId = view.findViewById(R.id.employee_id);
            dateRange = view.findViewById(R.id.date_range);
            daysRequested = view.findViewById(R.id.days_requested);
            reason = view.findViewById(R.id.reason);
            approveButton = view.findViewById(R.id.approve_button);
            denyButton = view.findViewById(R.id.deny_button);
        }
    }
}