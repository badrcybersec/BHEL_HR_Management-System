package common.models;

import java.io.Serializable;

public class LeaveApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    private int leaveId;
    private int employeeId;
    private String employeeName; // for display purposes
    private String leaveType;    // ANNUAL, MEDICAL, EMERGENCY
    private String startDate;
    private String endDate;
    private int daysRequested;
    private String reason;
    private String status;       // PENDING, APPROVED, REJECTED
    private int reviewedBy;
    private String reviewDate;
    private String appliedAt;

    public LeaveApplication() {
        this.status = "PENDING";
    }

    public LeaveApplication(int leaveId, int employeeId, String leaveType, String startDate,
                            String endDate, int daysRequested, String reason, String status,
                            int reviewedBy, String reviewDate, String appliedAt) {
        this.leaveId = leaveId;
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.daysRequested = daysRequested;
        this.reason = reason;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewDate = reviewDate;
        this.appliedAt = appliedAt;
    }

    public String toCSV() {
        return String.join(",",
            String.valueOf(leaveId),
            String.valueOf(employeeId),
            escapeCsv(leaveType),
            escapeCsv(startDate),
            escapeCsv(endDate),
            String.valueOf(daysRequested),
            escapeCsv(reason != null ? reason : ""),
            escapeCsv(status),
            String.valueOf(reviewedBy),
            escapeCsv(reviewDate != null ? reviewDate : ""),
            escapeCsv(appliedAt != null ? appliedAt : "")
        );
    }

    public static LeaveApplication fromCSV(String line) {
        String[] parts = Employee.parseCSV(line);
        if (parts.length < 11) return null;
        return new LeaveApplication(
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            parts[2].trim(),
            parts[3].trim(),
            parts[4].trim(),
            Integer.parseInt(parts[5].trim()),
            parts[6].trim().isEmpty() ? null : parts[6].trim(),
            parts[7].trim(),
            Integer.parseInt(parts[8].trim()),
            parts[9].trim().isEmpty() ? null : parts[9].trim(),
            parts[10].trim().isEmpty() ? null : parts[10].trim()
        );
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // Getters and Setters
    public int getLeaveId() { return leaveId; }
    public void setLeaveId(int leaveId) { this.leaveId = leaveId; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public int getDaysRequested() { return daysRequested; }
    public void setDaysRequested(int daysRequested) { this.daysRequested = daysRequested; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(int reviewedBy) { this.reviewedBy = reviewedBy; }
    public String getReviewDate() { return reviewDate; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }
    public String getAppliedAt() { return appliedAt; }
    public void setAppliedAt(String appliedAt) { this.appliedAt = appliedAt; }

    @Override
    public String toString() {
        return "Leave #" + leaveId + " [" + leaveType + "] " + startDate + " to " + endDate + " - " + status;
    }
}
