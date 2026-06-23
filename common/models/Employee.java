package common.models;

import java.io.Serializable;

/**
 * Represents an employee in the HRM system.
 * Implements Serializable for RMI transmission.
 */
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    private int employeeId;
    private String firstName;
    private String lastName;
    private String icPassport;
    private String email;
    private String phone;
    private String department;
    private String position;
    private String dateJoined;
    private boolean isActive;

    // Default constructor
    public Employee() {
        this.isActive = true;
    }

    // Constructor for registration
    public Employee(String firstName, String lastName, String icPassport) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.icPassport = icPassport;
        this.isActive = true;
    }

    // Full constructor
    public Employee(int employeeId, String firstName, String lastName, String icPassport,
                    String email, String phone, String department, String position,
                    String dateJoined, boolean isActive) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.icPassport = icPassport;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.position = position;
        this.dateJoined = dateJoined;
        this.isActive = isActive;
    }

    // CSV conversion
    public String toCSV() {
        return String.join(",",
            String.valueOf(employeeId),
            escapeCsv(firstName),
            escapeCsv(lastName),
            escapeCsv(icPassport),
            escapeCsv(email != null ? email : ""),
            escapeCsv(phone != null ? phone : ""),
            escapeCsv(department != null ? department : ""),
            escapeCsv(position != null ? position : ""),
            escapeCsv(dateJoined != null ? dateJoined : ""),
            isActive ? "1" : "0"
        );
    }

    public static Employee fromCSV(String line) {
        String[] parts = parseCSV(line);
        if (parts.length < 10) return null;
        return new Employee(
            Integer.parseInt(parts[0].trim()),
            parts[1].trim(),
            parts[2].trim(),
            parts[3].trim(),
            parts[4].trim().isEmpty() ? null : parts[4].trim(),
            parts[5].trim().isEmpty() ? null : parts[5].trim(),
            parts[6].trim().isEmpty() ? null : parts[6].trim(),
            parts[7].trim().isEmpty() ? null : parts[7].trim(),
            parts[8].trim().isEmpty() ? null : parts[8].trim(),
            parts[9].trim().equals("1")
        );
    }

    // CSV helpers
    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static String[] parseCSV(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "Employee{id=" + employeeId + ", name=" + getFullName() + ", ic=" + icPassport + "}";
    }

    // Getters and Setters
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getIcPassport() { return icPassport; }
    public void setIcPassport(String icPassport) { this.icPassport = icPassport; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getDateJoined() { return dateJoined; }
    public void setDateJoined(String dateJoined) { this.dateJoined = dateJoined; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
