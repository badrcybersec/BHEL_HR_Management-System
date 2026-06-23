package common.models;

import java.io.Serializable;

public class PayrollRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private int payrollId;
    private int employeeId;
    private int month;
    private int year;
    private double basicSalary;
    private double deductions;
    private double netSalary;
    private String generatedAt;

    public PayrollRecord() {}

    public PayrollRecord(int payrollId, int employeeId, int month, int year,
                         double basicSalary, double deductions, double netSalary, String generatedAt) {
        this.payrollId = payrollId;
        this.employeeId = employeeId;
        this.month = month;
        this.year = year;
        this.basicSalary = basicSalary;
        this.deductions = deductions;
        this.netSalary = netSalary;
        this.generatedAt = generatedAt;
    }

    public String toCSV() {
        return payrollId + "," + employeeId + "," + month + "," + year + ","
             + basicSalary + "," + deductions + "," + netSalary + ","
             + (generatedAt != null ? generatedAt : "");
    }

    public static PayrollRecord fromCSV(String line) {
        String[] p = Employee.parseCSV(line);
        if (p.length < 8) return null;
        return new PayrollRecord(
            Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()),
            Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim()),
            Double.parseDouble(p[4].trim()), Double.parseDouble(p[5].trim()),
            Double.parseDouble(p[6].trim()), p[7].trim().isEmpty() ? null : p[7].trim()
        );
    }

    // Getters and Setters
    public int getPayrollId() { return payrollId; }
    public void setPayrollId(int payrollId) { this.payrollId = payrollId; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public double getBasicSalary() { return basicSalary; }
    public void setBasicSalary(double basicSalary) { this.basicSalary = basicSalary; }
    public double getDeductions() { return deductions; }
    public void setDeductions(double deductions) { this.deductions = deductions; }
    public double getNetSalary() { return netSalary; }
    public void setNetSalary(double netSalary) { this.netSalary = netSalary; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
}
