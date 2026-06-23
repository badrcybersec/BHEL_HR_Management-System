package common.models;

import java.io.Serializable;

public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String passwordHash;
    private String role;       // EMPLOYEE, HR, ADMIN
    private int employeeId;    // linked employee (0 if admin-only)
    private boolean isActive;

    public UserAccount() { this.isActive = true; }

    public UserAccount(int userId, String username, String passwordHash, String role, int employeeId, boolean isActive) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.employeeId = employeeId;
        this.isActive = isActive;
    }

    public String toCSV() {
        return userId + "," + escapeCsv(username) + "," + escapeCsv(passwordHash) + "," + role + "," + employeeId + "," + (isActive ? "1" : "0");
    }

    public static UserAccount fromCSV(String line) {
        String[] p = Employee.parseCSV(line);
        if (p.length < 6) return null;
        return new UserAccount(
            Integer.parseInt(p[0].trim()), p[1].trim(), p[2].trim(),
            p[3].trim(), Integer.parseInt(p[4].trim()), p[5].trim().equals("1")
        );
    }

    private static String escapeCsv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"")) return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return username + " [" + role + "]";
    }
}
