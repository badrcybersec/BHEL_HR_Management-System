package common.models;

import java.io.Serializable;

public class LeaveBalance implements Serializable {
    private static final long serialVersionUID = 1L;

    private int balanceId;
    private int employeeId;
    private String leaveType;  // ANNUAL, MEDICAL, EMERGENCY
    private int totalDays;
    private int usedDays;
    private int year;

    public LeaveBalance() {}

    public LeaveBalance(int balanceId, int employeeId, String leaveType, int totalDays, int usedDays, int year) {
        this.balanceId = balanceId;
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.totalDays = totalDays;
        this.usedDays = usedDays;
        this.year = year;
    }

    public int getRemainingDays() { return totalDays - usedDays; }

    public String toCSV() {
        return balanceId + "," + employeeId + "," + leaveType + "," + totalDays + "," + usedDays + "," + year;
    }

    public static LeaveBalance fromCSV(String line) {
        String[] p = Employee.parseCSV(line);
        if (p.length < 6) return null;
        return new LeaveBalance(
            Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()),
            p[2].trim(), Integer.parseInt(p[3].trim()),
            Integer.parseInt(p[4].trim()), Integer.parseInt(p[5].trim())
        );
    }

    // Getters and Setters
    public int getBalanceId() { return balanceId; }
    public void setBalanceId(int balanceId) { this.balanceId = balanceId; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public int getUsedDays() { return usedDays; }
    public void setUsedDays(int usedDays) { this.usedDays = usedDays; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    @Override
    public String toString() {
        return leaveType + ": " + getRemainingDays() + "/" + totalDays + " remaining";
    }
}
