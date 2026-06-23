package common.models;

import java.io.Serializable;

public class AuditLogEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private int logId;
    private int userId;
    private String username;
    private String role;
    private String action;
    private String targetTable;
    private int targetId;
    private String details;
    private String timestamp;

    public AuditLogEntry() {}

    public AuditLogEntry(int logId, int userId, String username, String role, String action,
                         String targetTable, int targetId, String details, String timestamp) {
        this.logId = logId;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.action = action;
        this.targetTable = targetTable;
        this.targetId = targetId;
        this.details = details;
        this.timestamp = timestamp;
    }

    public String toCSV() {
        return String.join(",",
            String.valueOf(logId), String.valueOf(userId),
            escapeCsv(username), escapeCsv(role), escapeCsv(action),
            escapeCsv(targetTable != null ? targetTable : ""),
            String.valueOf(targetId),
            escapeCsv(details != null ? details : ""),
            escapeCsv(timestamp != null ? timestamp : "")
        );
    }

    public static AuditLogEntry fromCSV(String line) {
        String[] p = Employee.parseCSV(line);
        if (p.length < 9) return null;
        return new AuditLogEntry(
            Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()),
            p[2].trim(), p[3].trim(), p[4].trim(),
            p[5].trim().isEmpty() ? null : p[5].trim(),
            p[6].trim().isEmpty() ? 0 : Integer.parseInt(p[6].trim()),
            p[7].trim().isEmpty() ? null : p[7].trim(),
            p[8].trim().isEmpty() ? null : p[8].trim()
        );
    }

    private static String escapeCsv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    // Getters and Setters
    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + username + " (" + role + "): " + action + " - " + details;
    }
}
