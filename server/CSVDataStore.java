package server;

import common.interfaces.DatabaseService;
import common.models.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized CSV data storage manager.
 * All methods are synchronized to prevent concurrent write conflicts.
 * CSV files are stored in the /data directory.
 *
 * FAULT TOLERANCE: When a replicator (backup DatabaseService) is configured,
 * every write operation is automatically forwarded to the backup.
 * Replication is fire-and-forget — if backup is unreachable, primary continues.
 */
public class CSVDataStore {

    private final String dataDir;

    /** Remote reference to backup's DatabaseService for replication. Null if standalone. */
    private volatile DatabaseService replicator;

    // CSV file names
    private static final String USERS_FILE = "users.csv";
    private static final String EMPLOYEES_FILE = "employees.csv";
    private static final String FAMILY_FILE = "family_members.csv";
    private static final String LEAVE_BALANCE_FILE = "leave_balances.csv";
    private static final String LEAVE_APPS_FILE = "leave_applications.csv";
    private static final String PROFILE_UPDATES_FILE = "profile_updates.csv";
    private static final String AUDIT_LOG_FILE = "audit_log.csv";
    private static final String PAYROLL_FILE = "payroll_records.csv";

    public CSVDataStore(String dataDir) {
        this.dataDir = dataDir;
        initializeFiles();
    }

    // ==================== REPLICATION ====================

    public void setReplicator(DatabaseService replicator) {
        this.replicator = replicator;
        System.out.println("[CSVDataStore] Replication " + (replicator != null ? "ENABLED" : "DISABLED"));
    }

    public boolean isReplicating() {
        return replicator != null;
    }

    // ==================== INITIALIZATION ====================

    private void initializeFiles() {
        try {
            Files.createDirectories(Paths.get(dataDir));
            createIfNotExists(USERS_FILE, "user_id,username,password_hash,role,employee_id,is_active");
            createIfNotExists(EMPLOYEES_FILE, "employee_id,first_name,last_name,ic_passport,email,phone,department,position,date_joined,is_active");
            createIfNotExists(FAMILY_FILE, "family_id,employee_id,name,relationship,ic_passport,date_of_birth");
            createIfNotExists(LEAVE_BALANCE_FILE, "balance_id,employee_id,leave_type,total_days,used_days,year");
            createIfNotExists(LEAVE_APPS_FILE, "leave_id,employee_id,leave_type,start_date,end_date,days_requested,reason,status,reviewed_by,review_date,applied_at");
            createIfNotExists(PROFILE_UPDATES_FILE, "request_id,employee_id,field_name,old_value,new_value,status,requested_at,reviewed_by,review_date");
            createIfNotExists(AUDIT_LOG_FILE, "log_id,user_id,username,role,action,target_table,target_id,details,timestamp");
            createIfNotExists(PAYROLL_FILE, "payroll_id,employee_id,month,year,basic_salary,deductions,net_salary,generated_at");
            System.out.println("[CSVDataStore] Data files initialized in: " + dataDir);
        } catch (IOException e) {
            System.err.println("[CSVDataStore] Error initializing files: " + e.getMessage());
        }
    }

    private void createIfNotExists(String fileName, String header) throws IOException {
        Path path = Paths.get(dataDir, fileName);
        if (!Files.exists(path)) {
            Files.writeString(path, header + "\n");
        }
    }

    // ==================== GENERIC CSV HELPERS ====================

    private synchronized List<String> readLines(String fileName) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(dataDir, fileName));
            if (lines.size() > 0) lines.remove(0);
            return lines;
        } catch (IOException e) {
            System.err.println("[CSVDataStore] Error reading " + fileName + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private synchronized void writeLines(String fileName, String header, List<String> dataLines) {
        try {
            List<String> allLines = new ArrayList<>();
            allLines.add(header);
            allLines.addAll(dataLines);
            Files.write(Paths.get(dataDir, fileName), allLines);
        } catch (IOException e) {
            System.err.println("[CSVDataStore] Error writing " + fileName + ": " + e.getMessage());
        }
        // Replicate full file to backup
        DatabaseService rep = this.replicator;
        if (rep != null) {
            try { rep.replicateWrite(fileName, header, dataLines); }
            catch (Exception e) { System.err.println("[REPLICATION] Backup unreachable (WRITE " + fileName + ")"); }
        }
    }

    private synchronized void appendLine(String fileName, String line) {
        try {
            Files.writeString(Paths.get(dataDir, fileName), line + "\n", StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[CSVDataStore] Error appending to " + fileName + ": " + e.getMessage());
        }
        // Replicate append to backup
        DatabaseService rep = this.replicator;
        if (rep != null) {
            try { rep.replicateAppend(fileName, line); }
            catch (Exception e) { System.err.println("[REPLICATION] Backup unreachable (APPEND " + fileName + ")"); }
        }
    }

    private int getNextId(String fileName) {
        List<String> lines = readLines(fileName);
        int maxId = 0;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            try {
                String[] parts = Employee.parseCSV(line);
                int id = Integer.parseInt(parts[0].trim());
                if (id > maxId) maxId = id;
            } catch (Exception e) { /* skip bad lines */ }
        }
        return maxId + 1;
    }

    // ==================== USER OPERATIONS ====================

    public synchronized UserAccount getUserByUsername(String username) {
        for (String line : readLines(USERS_FILE)) {
            if (line.trim().isEmpty()) continue;
            UserAccount user = UserAccount.fromCSV(line);
            if (user != null && user.getUsername().equals(username)) return user;
        }
        return null;
    }

    public synchronized UserAccount getUserById(int userId) {
        for (String line : readLines(USERS_FILE)) {
            if (line.trim().isEmpty()) continue;
            UserAccount user = UserAccount.fromCSV(line);
            if (user != null && user.getUserId() == userId) return user;
        }
        return null;
    }

    public synchronized List<UserAccount> getAllUsers() {
        return readLines(USERS_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(UserAccount::fromCSV)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public synchronized int addUser(UserAccount user) {
        int id = getNextId(USERS_FILE);
        user.setUserId(id);
        appendLine(USERS_FILE, user.toCSV());
        return id;
    }

    public synchronized boolean updateUser(UserAccount updated) {
        List<String> lines = readLines(USERS_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            UserAccount user = UserAccount.fromCSV(line);
            if (user != null && user.getUserId() == updated.getUserId()) {
                newLines.add(updated.toCSV());
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(USERS_FILE, "user_id,username,password_hash,role,employee_id,is_active", newLines);
        }
        return found;
    }

    public synchronized boolean removeUser(int userId) {
        List<String> lines = readLines(USERS_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            UserAccount user = UserAccount.fromCSV(line);
            if (user != null && user.getUserId() == userId) {
                user.setActive(false);
                newLines.add(user.toCSV());
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(USERS_FILE, "user_id,username,password_hash,role,employee_id,is_active", newLines);
        }
        return found;
    }

    // ==================== EMPLOYEE OPERATIONS ====================

    public synchronized Employee getEmployee(int employeeId) {
        for (String line : readLines(EMPLOYEES_FILE)) {
            if (line.trim().isEmpty()) continue;
            Employee emp = Employee.fromCSV(line);
            if (emp != null && emp.getEmployeeId() == employeeId) return emp;
        }
        return null;
    }

    public synchronized Employee getEmployeeByIC(String ic) {
        for (String line : readLines(EMPLOYEES_FILE)) {
            if (line.trim().isEmpty()) continue;
            Employee emp = Employee.fromCSV(line);
            if (emp != null && emp.getIcPassport().equals(ic)) return emp;
        }
        return null;
    }

    public synchronized List<Employee> getAllEmployees() {
        return readLines(EMPLOYEES_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(Employee::fromCSV)
            .filter(Objects::nonNull)
            .filter(Employee::isActive)
            .collect(Collectors.toList());
    }

    public synchronized List<Employee> searchEmployees(String query) {
        String q = query.toLowerCase();
        return getAllEmployees().stream()
            .filter(e -> e.getFirstName().toLowerCase().contains(q)
                      || e.getLastName().toLowerCase().contains(q)
                      || e.getIcPassport().contains(q)
                      || e.getFullName().toLowerCase().contains(q))
            .collect(Collectors.toList());
    }

    public synchronized int addEmployee(Employee emp) {
        if (getEmployeeByIC(emp.getIcPassport()) != null) return -1;
        int id = getNextId(EMPLOYEES_FILE);
        emp.setEmployeeId(id);
        if (emp.getDateJoined() == null) {
            emp.setDateJoined(java.time.LocalDate.now().toString());
        }
        appendLine(EMPLOYEES_FILE, emp.toCSV());
        int year = java.time.Year.now().getValue();
        initializeLeaveBalances(id, year);
        return id;
    }

    public synchronized boolean updateEmployee(Employee updated) {
        List<String> lines = readLines(EMPLOYEES_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            Employee emp = Employee.fromCSV(line);
            if (emp != null && emp.getEmployeeId() == updated.getEmployeeId()) {
                newLines.add(updated.toCSV());
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(EMPLOYEES_FILE,
                "employee_id,first_name,last_name,ic_passport,email,phone,department,position,date_joined,is_active",
                newLines);
        }
        return found;
    }

    // ==================== FAMILY MEMBER OPERATIONS ====================

    public synchronized List<FamilyMember> getFamilyMembers(int employeeId) {
        return readLines(FAMILY_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(FamilyMember::fromCSV)
            .filter(Objects::nonNull)
            .filter(f -> f.getEmployeeId() == employeeId)
            .collect(Collectors.toList());
    }

    public synchronized int addFamilyMember(FamilyMember member) {
        int id = getNextId(FAMILY_FILE);
        member.setFamilyId(id);
        appendLine(FAMILY_FILE, member.toCSV());
        return id;
    }

    public synchronized boolean updateFamilyMember(FamilyMember updated) {
        List<String> lines = readLines(FAMILY_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            FamilyMember fm = FamilyMember.fromCSV(line);
            if (fm != null && fm.getFamilyId() == updated.getFamilyId()) {
                newLines.add(updated.toCSV());
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(FAMILY_FILE, "family_id,employee_id,name,relationship,ic_passport,date_of_birth", newLines);
        }
        return found;
    }

    public synchronized boolean removeFamilyMember(int familyId, int employeeId) {
        List<String> lines = readLines(FAMILY_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            FamilyMember fm = FamilyMember.fromCSV(line);
            if (fm != null && fm.getFamilyId() == familyId && fm.getEmployeeId() == employeeId) {
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(FAMILY_FILE, "family_id,employee_id,name,relationship,ic_passport,date_of_birth", newLines);
        }
        return found;
    }

    public synchronized boolean removeFamilyMember(int familyId) {
        List<String> lines = readLines(FAMILY_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            FamilyMember fm = FamilyMember.fromCSV(line);
            if (fm != null && fm.getFamilyId() == familyId) {
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(FAMILY_FILE, "family_id,employee_id,name,relationship,ic_passport,date_of_birth", newLines);
        }
        return found;
    }

    // ==================== LEAVE BALANCE OPERATIONS ====================

    public synchronized void initializeLeaveBalances(int employeeId, int year) {
        String[] types = {"ANNUAL", "MEDICAL", "EMERGENCY"};
        int[] defaults = {14, 14, 5};
        for (int i = 0; i < types.length; i++) {
            int id = getNextId(LEAVE_BALANCE_FILE);
            LeaveBalance lb = new LeaveBalance(id, employeeId, types[i], defaults[i], 0, year);
            appendLine(LEAVE_BALANCE_FILE, lb.toCSV());
        }
    }

    public synchronized List<LeaveBalance> getLeaveBalances(int employeeId, int year) {
        return readLines(LEAVE_BALANCE_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(LeaveBalance::fromCSV)
            .filter(Objects::nonNull)
            .filter(lb -> lb.getEmployeeId() == employeeId && lb.getYear() == year)
            .collect(Collectors.toList());
    }

    public synchronized boolean updateLeaveBalance(int employeeId, String leaveType, int year, int additionalUsed) {
        List<String> lines = readLines(LEAVE_BALANCE_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            LeaveBalance lb = LeaveBalance.fromCSV(line);
            if (lb != null && lb.getEmployeeId() == employeeId
                && lb.getLeaveType().equals(leaveType) && lb.getYear() == year) {
                lb.setUsedDays(lb.getUsedDays() + additionalUsed);
                newLines.add(lb.toCSV());
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(LEAVE_BALANCE_FILE,
                "balance_id,employee_id,leave_type,total_days,used_days,year", newLines);
        }
        return found;
    }

    public synchronized List<LeaveBalance> getAllLeaveBalances() {
        return readLines(LEAVE_BALANCE_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(LeaveBalance::fromCSV)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public synchronized int addLeaveBalance(LeaveBalance balance) {
        int id = getNextId(LEAVE_BALANCE_FILE);
        balance.setBalanceId(id);
        appendLine(LEAVE_BALANCE_FILE, balance.toCSV());
        return id;
    }

    public synchronized boolean updateLeaveBalance(LeaveBalance balance) {
        List<String> lines = readLines(LEAVE_BALANCE_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            LeaveBalance lb = LeaveBalance.fromCSV(line);
            if (lb != null && lb.getBalanceId() == balance.getBalanceId()) {
                newLines.add(balance.toCSV());
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(LEAVE_BALANCE_FILE,
                "balance_id,employee_id,leave_type,total_days,used_days,year", newLines);
        }
        return found;
    }

    public synchronized List<LeaveApplication> getLeaveApplications(int employeeId) {
        return getLeaveApplicationsByEmployee(employeeId);
    }

    public synchronized List<LeaveApplication> getAllLeaveApplications() {
        return readLines(LEAVE_APPS_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(LeaveApplication::fromCSV)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // ==================== LEAVE APPLICATION OPERATIONS ====================

    public synchronized int addLeaveApplication(LeaveApplication app) {
        int id = getNextId(LEAVE_APPS_FILE);
        app.setLeaveId(id);
        if (app.getAppliedAt() == null) {
            app.setAppliedAt(java.time.LocalDateTime.now().toString());
        }
        appendLine(LEAVE_APPS_FILE, app.toCSV());
        return id;
    }

    public synchronized List<LeaveApplication> getLeaveApplicationsByEmployee(int employeeId) {
        return readLines(LEAVE_APPS_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(LeaveApplication::fromCSV)
            .filter(Objects::nonNull)
            .filter(la -> la.getEmployeeId() == employeeId)
            .collect(Collectors.toList());
    }

    public synchronized List<LeaveApplication> getLeaveApplicationsByYear(int employeeId, int year) {
        String yearStr = String.valueOf(year);
        return getLeaveApplicationsByEmployee(employeeId).stream()
            .filter(la -> la.getStartDate().startsWith(yearStr))
            .collect(Collectors.toList());
    }

    public synchronized List<LeaveApplication> getPendingLeaveApplications() {
        return readLines(LEAVE_APPS_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(LeaveApplication::fromCSV)
            .filter(Objects::nonNull)
            .filter(la -> "PENDING".equals(la.getStatus()))
            .collect(Collectors.toList());
    }

    public synchronized LeaveApplication getLeaveApplication(int leaveId) {
        for (String line : readLines(LEAVE_APPS_FILE)) {
            if (line.trim().isEmpty()) continue;
            LeaveApplication la = LeaveApplication.fromCSV(line);
            if (la != null && la.getLeaveId() == leaveId) return la;
        }
        return null;
    }

    public synchronized boolean updateLeaveApplication(LeaveApplication updated) {
        List<String> lines = readLines(LEAVE_APPS_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            LeaveApplication la = LeaveApplication.fromCSV(line);
            if (la != null && la.getLeaveId() == updated.getLeaveId()) {
                newLines.add(updated.toCSV());
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(LEAVE_APPS_FILE,
                "leave_id,employee_id,leave_type,start_date,end_date,days_requested,reason,status,reviewed_by,review_date,applied_at",
                newLines);
        }
        return found;
    }

    // ==================== PROFILE UPDATE REQUEST OPERATIONS ====================

    public synchronized int addProfileUpdateRequest(int employeeId, String fieldName, String oldValue, String newValue) {
        int id = getNextId(PROFILE_UPDATES_FILE);
        String timestamp = java.time.LocalDateTime.now().toString();
        String line = id + "," + employeeId + "," + escapeCsv(fieldName) + ","
                     + escapeCsv(oldValue != null ? oldValue : "") + ","
                     + escapeCsv(newValue) + ",PENDING," + timestamp + ",0,";
        appendLine(PROFILE_UPDATES_FILE, line);
        return id;
    }

    public synchronized List<String[]> getPendingProfileUpdates() {
        List<String[]> results = new ArrayList<>();
        for (String line : readLines(PROFILE_UPDATES_FILE)) {
            if (line.trim().isEmpty()) continue;
            String[] parts = Employee.parseCSV(line);
            if (parts.length >= 9 && "PENDING".equals(parts[5].trim())) {
                results.add(parts);
            }
        }
        return results;
    }

    public synchronized String[] getProfileUpdateRequest(int requestId) {
        for (String line : readLines(PROFILE_UPDATES_FILE)) {
            if (line.trim().isEmpty()) continue;
            String[] parts = Employee.parseCSV(line);
            if (parts.length >= 9 && Integer.parseInt(parts[0].trim()) == requestId) {
                return parts;
            }
        }
        return null;
    }

    public synchronized boolean updateProfileRequest(int requestId, String status, int reviewedBy) {
        List<String> lines = readLines(PROFILE_UPDATES_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = Employee.parseCSV(line);
            if (parts.length >= 9 && Integer.parseInt(parts[0].trim()) == requestId) {
                parts[5] = status;
                parts[7] = String.valueOf(reviewedBy);
                parts[8] = java.time.LocalDateTime.now().toString();
                newLines.add(String.join(",", parts));
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(PROFILE_UPDATES_FILE,
                "request_id,employee_id,field_name,old_value,new_value,status,requested_at,reviewed_by,review_date",
                newLines);
        }
        return found;
    }

    // ==================== AUDIT LOG OPERATIONS ====================

    public synchronized void addAuditLog(int userId, String username, String role,
                                          String action, String targetTable, int targetId, String details) {
        int id = getNextId(AUDIT_LOG_FILE);
        AuditLogEntry entry = new AuditLogEntry(id, userId, username, role, action,
            targetTable, targetId, details, java.time.LocalDateTime.now().toString());
        appendLine(AUDIT_LOG_FILE, entry.toCSV());
    }

    public synchronized List<AuditLogEntry> getAuditLog() {
        return readLines(AUDIT_LOG_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(AuditLogEntry::fromCSV)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public synchronized List<AuditLogEntry> getAuditLogForUser(int userId) {
        return readLines(AUDIT_LOG_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(AuditLogEntry::fromCSV)
            .filter(Objects::nonNull)
            .filter(entry -> entry.getUserId() == userId)
            .collect(Collectors.toList());
    }

    public synchronized List<AuditLogEntry> getAuditLogForTable(String tableName) {
        return readLines(AUDIT_LOG_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(AuditLogEntry::fromCSV)
            .filter(Objects::nonNull)
            .filter(entry -> tableName != null && tableName.equals(entry.getTargetTable()))
            .collect(Collectors.toList());
    }

    // ==================== PAYROLL OPERATIONS ====================

    public synchronized int addPayrollRecord(PayrollRecord record) {
        int id = getNextId(PAYROLL_FILE);
        record.setPayrollId(id);
        if (record.getGeneratedAt() == null) {
            record.setGeneratedAt(java.time.LocalDateTime.now().toString());
        }
        appendLine(PAYROLL_FILE, record.toCSV());
        return id;
    }

    public synchronized PayrollRecord getPayrollRecord(int employeeId, int month, int year) {
        for (String line : readLines(PAYROLL_FILE)) {
            if (line.trim().isEmpty()) continue;
            PayrollRecord pr = PayrollRecord.fromCSV(line);
            if (pr != null && pr.getEmployeeId() == employeeId
                && pr.getMonth() == month && pr.getYear() == year) {
                return pr;
            }
        }
        return null;
    }

    public synchronized List<PayrollRecord> getYearlyPayroll(int employeeId, int year) {
        return readLines(PAYROLL_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(PayrollRecord::fromCSV)
            .filter(Objects::nonNull)
            .filter(pr -> pr.getEmployeeId() == employeeId && pr.getYear() == year)
            .collect(Collectors.toList());
    }

    public synchronized List<PayrollRecord> getPayrollRecords(int employeeId) {
        return readLines(PAYROLL_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(PayrollRecord::fromCSV)
            .filter(Objects::nonNull)
            .filter(pr -> pr.getEmployeeId() == employeeId)
            .collect(Collectors.toList());
    }

    public synchronized List<PayrollRecord> getAllPayrollRecords() {
        return readLines(PAYROLL_FILE).stream()
            .filter(l -> !l.trim().isEmpty())
            .map(PayrollRecord::fromCSV)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public synchronized boolean updatePayrollRecord(PayrollRecord record) {
        List<String> lines = readLines(PAYROLL_FILE);
        List<String> newLines = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            PayrollRecord pr = PayrollRecord.fromCSV(line);
            if (pr != null && pr.getPayrollId() == record.getPayrollId()) {
                newLines.add(record.toCSV());
                found = true;
            } else {
                newLines.add(line);
            }
        }
        if (found) {
            writeLines(PAYROLL_FILE,
                "payroll_id,employee_id,month,year,basic_salary,deductions,net_salary,generated_at",
                newLines);
        }
        return found;
    }

    public synchronized List<Object> getProfileUpdateRequests() {
        return new ArrayList<>(getPendingProfileUpdates());
    }

    public synchronized boolean approveProfileUpdate(int requestId, int reviewedBy) {
        return updateProfileRequest(requestId, "APPROVED", reviewedBy);
    }

    public synchronized boolean rejectProfileUpdate(int requestId) {
        return updateProfileRequest(requestId, "REJECTED", 0);
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
