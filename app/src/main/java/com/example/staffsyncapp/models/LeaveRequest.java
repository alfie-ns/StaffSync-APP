package com.example.staffsyncapp.models;

public class LeaveRequest {
    private final int id;
    private final int employeeId;
    private final String employeeName;
    private final String startDate;
    private final String endDate;
    private final String reason;
    private final int daysRequested;

    private String status = "pending";
    private String adminResponse;
    private String createdAt;

    /**
     * Constructor for the LeaveRequest class.
     * Creates a LeaveRequest object with the specified parameters.
     *
     * @param id           the unique identifier for the leave request
     * @param employeeId   the unique identifier for the employee
     * @param employeeName the name of the employee
     * @param startDate    the start date of the leave
     * @param endDate      the end date of the leave
     * @param reason       the reason for the leave
     * @param daysRequested the number of days requested for the leave
     */

    public LeaveRequest(int id, int employeeId, String employeeName,
                        String startDate, String endDate, String reason,
                        int daysRequested) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.daysRequested = daysRequested;
    }

    // Getters
    public int getId() { return id; }
    public int getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public int getDaysRequested() { return daysRequested; }

    public String getStatus() { return status; }
    public String getAdminResponse() { return adminResponse; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}