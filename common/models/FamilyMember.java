package common.models;

import java.io.Serializable;

public class FamilyMember implements Serializable {
    private static final long serialVersionUID = 1L;

    private int familyId;
    private int employeeId;
    private String name;
    private String relationship; // SPOUSE, CHILD, PARENT, SIBLING
    private String icPassport;
    private String dateOfBirth;

    public FamilyMember() {}

    public FamilyMember(int familyId, int employeeId, String name, String relationship,
                        String icPassport, String dateOfBirth) {
        this.familyId = familyId;
        this.employeeId = employeeId;
        this.name = name;
        this.relationship = relationship;
        this.icPassport = icPassport;
        this.dateOfBirth = dateOfBirth;
    }

    public String toCSV() {
        return String.join(",",
            String.valueOf(familyId),
            String.valueOf(employeeId),
            Employee.parseCSV(name) != null ? escapeCsv(name) : "",
            escapeCsv(relationship != null ? relationship : ""),
            escapeCsv(icPassport != null ? icPassport : ""),
            escapeCsv(dateOfBirth != null ? dateOfBirth : "")
        );
    }

    public static FamilyMember fromCSV(String line) {
        String[] parts = Employee.parseCSV(line);
        if (parts.length < 6) return null;
        return new FamilyMember(
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            parts[2].trim(),
            parts[3].trim(),
            parts[4].trim().isEmpty() ? null : parts[4].trim(),
            parts[5].trim().isEmpty() ? null : parts[5].trim()
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
    public int getFamilyId() { return familyId; }
    public void setFamilyId(int familyId) { this.familyId = familyId; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getIcPassport() { return icPassport; }
    public void setIcPassport(String icPassport) { this.icPassport = icPassport; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    @Override
    public String toString() {
        return name + " (" + relationship + ")";
    }
}
