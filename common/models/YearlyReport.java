package common.models;

import java.io.Serializable;
import java.util.List;

/**
 * Container for the yearly employee report.
 * Holds employee profile, family details, and leave history for a given year.
 */
public class YearlyReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private Employee employee;
    private List<FamilyMember> familyMembers;
    private List<LeaveBalance> leaveBalances;
    private List<LeaveApplication> leaveApplications;
    private int year;
    private String generatedAt;

    public YearlyReport() {}

    public YearlyReport(Employee employee, List<FamilyMember> familyMembers,
                        List<LeaveBalance> leaveBalances, List<LeaveApplication> leaveApplications,
                        int year, String generatedAt) {
        this.employee = employee;
        this.familyMembers = familyMembers;
        this.leaveBalances = leaveBalances;
        this.leaveApplications = leaveApplications;
        this.year = year;
        this.generatedAt = generatedAt;
    }

    // Getters and Setters
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public List<FamilyMember> getFamilyMembers() { return familyMembers; }
    public void setFamilyMembers(List<FamilyMember> familyMembers) { this.familyMembers = familyMembers; }
    public List<LeaveBalance> getLeaveBalances() { return leaveBalances; }
    public void setLeaveBalances(List<LeaveBalance> leaveBalances) { this.leaveBalances = leaveBalances; }
    public List<LeaveApplication> getLeaveApplications() { return leaveApplications; }
    public void setLeaveApplications(List<LeaveApplication> leaveApplications) { this.leaveApplications = leaveApplications; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
}
