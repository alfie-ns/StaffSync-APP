package com.example.staffsyncapp;

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
import com.example.staffsyncapp.databinding.AdminHolidayRequestsFragmentBinding;
import com.example.staffsyncapp.models.LeaveRequest;
import com.example.staffsyncapp.utils.LocalDataService;
import com.example.staffsyncapp.utils.NotificationService;
//import java.util.ArrayList;
//import java.util.List;

public class AdminHolidayRequestsFragment extends Fragment implements HolidayRequestAdapter.OnRequestActionListener {
    private static final String TAG = "AdminHolidayRequests";
    private AdminHolidayRequestsFragmentBinding binding;
    private LocalDataService dbHelper;
    private NotificationService notificationService;
    private HolidayRequestAdapter adapter;

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
        adapter = new HolidayRequestAdapter(this);

        binding.backArrow.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

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

            if (requests == null || requests.isEmpty()) {
                binding.emptyState.setVisibility(View.VISIBLE);
            } else {
                binding.pendingCount.setText(String.format("Pending Requests: %d", requests.size()));
                adapter.updateRequests(requests); // update adapter with new requests
            }
        });
    }
    // TODO: combine functions into one
    @Override
    public void onApprove(LeaveRequest request) { // approve leave request
        dbHelper.updateLeaveRequestStatus(
                request.getId(),
                "approved",
                "Request approved by admin",
                success -> {
                    if (success) {
                        notifyEmployee(request.getEmployeeId(), true);
                        loadRequests();
                    }
                }
        );
    }
    @Override
    public void onDeny(LeaveRequest request) { // deny leave request
        dbHelper.updateLeaveRequestStatus(
                request.getId(),
                "denied",
                "Request denied by admin",
                success -> {
                    if (success) {
                        notifyEmployee(request.getEmployeeId(), false);
                        loadRequests();
                    }
                }
        );
    }
    private void notifyEmployee(int employeeId, boolean isApproved) { // send notification to employee with NoficationService
        String title = "Holiday Request " + (isApproved ? "Approved" : "Denied");
        String message = "Your holiday request has been " +
                (isApproved ? "approved" : "denied") +
                " by the administrator";

        notificationService.sendHolidayNotification(title, message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}