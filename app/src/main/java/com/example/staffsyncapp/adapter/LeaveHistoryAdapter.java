package com.example.staffsyncapp.adapter;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.staffsyncapp.R;
import com.example.staffsyncapp.models.LeaveRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
* Leave History Adapter
* Adapter for displaying leave history in a RecyclerView.
*/


public class LeaveHistoryAdapter extends RecyclerView.Adapter<LeaveHistoryAdapter.ViewHolder> {
    private List<LeaveRequest> leaveRequests;
    private final Context context;

    /**
     * Constructor for LeaveHistoryAdapter; initialises context and leaveRequests
     * @param context: the context in which the adapter is being used.
     */
    public LeaveHistoryAdapter(Context context) {
        this.context = context;
        this.leaveRequests = new ArrayList<>();
    }


    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     * @param parent: the ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType: the view type of the new View.
     * @return a new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.leave_history_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * @param holder: the ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position: the position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (leaveRequests == null || position >= leaveRequests.size()) return;

        LeaveRequest request = leaveRequests.get(position);

        // format dates nicely
        String dateRange = String.format("%s to %s",
                formatDate(request.getStartDate()),
                formatDate(request.getEndDate()));

        holder.leaveDates.setText(dateRange);

        // show days requested and reason
        String leaveDetails = String.format(Locale.getDefault(), "Days: %d - %s",
                request.getDaysRequested(),
                request.getReason());

        holder.leaveType.setText(leaveDetails);

        // capitalise status and set color
        String status = request.getStatus() != null ?
                request.getStatus().toUpperCase() : "PENDING";
        holder.leaveStatus.setText("Status: " + status);
        holder.leaveStatus.setTextColor(getStatusColor(status));

        // show created date
        holder.decisionDate.setText("Requested: " + formatDate(request.getCreatedAt()));

        // only show admin response if one exists
        if (request.getAdminResponse() != null && !request.getAdminResponse().isEmpty()) {
            holder.adminResponse.setVisibility(View.VISIBLE);
            holder.adminResponse.setText("Admin Response: " + request.getAdminResponse());
        } else {
            holder.adminResponse.setVisibility(View.GONE);
        }

        holder.removeIcon.setOnClickListener(v -> {
            leaveRequests.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, leaveRequests.size());
        });
    }

    /**
     * Maps a status to a respective colour using a try-switch pattern.
     * @param status: the status to map ("APPROVED", "DENIED", or other).
     * @return the colour corresponding to the status.
     */
    private int getStatusColor(String status) {
        switch(status) {
            case "APPROVED":
                return context.getColor(android.R.color.holo_green_dark);
            case "DENIED":
                return context.getColor(android.R.color.holo_red_dark);
            default:
                return context.getColor(android.R.color.darker_gray); // pending
        }
    }

    /**
     * Formats the given date string from "yyyy-MM-dd" to "dd MMM yyyy".
     * @param dateStr: the date string to format.
     * @return the formatted date string, or the original string if parsing fails.
     */
    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr; // return original if parsing fails
        }
    }

    @Override
    public int getItemCount() {
        return leaveRequests.size();
    }

    /**
     * Updates the RecyclerView with a new list of leave requests and refreshes the data view.
     * @param requests: the new list of leave requests to display.
     */
    public void setLeaveRequests(List<LeaveRequest> requests) {
        Log.d(TAG, "Setting leave requests: " + requests.size()); // log size of leave requests
        this.leaveRequests = requests;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder:
     * Holds references to the views for a single item in the RecyclerView.
     */

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView leaveDates;
        final TextView leaveType;
        final TextView leaveStatus;
        final TextView decisionDate;
        final TextView adminResponse;
        final ImageView removeIcon;

        ViewHolder(View view) {
            super(view);
            leaveDates = view.findViewById(R.id.leave_dates);
            leaveType = view.findViewById(R.id.leave_type);
            leaveStatus = view.findViewById(R.id.leave_status);
            decisionDate = view.findViewById(R.id.decision_date);
            adminResponse = view.findViewById(R.id.admin_response);
            removeIcon = view.findViewById(R.id.close_icon);
        }
    }
}