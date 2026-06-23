package server;

import common.interfaces.DatabaseService;
import common.models.*;
import java.io.IOException;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Database Service.
 * Provides remote access to all CSV data operations with comprehensive auditing.
 * All write operations are synchronized for thread-safe concurrent access.
 * Also handles receiving replicated data when running as a backup server.
 */
public class DatabaseServiceImpl extends UnicastRemoteObject implements DatabaseService {

    private final CSVDataStore dataStore;
    private final String dataDir;

    public DatabaseServiceImpl(CSVDataStore dataStore, String dataDir, int port,
                              RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(port, csf, ssf);
        this.dataStore = dataStore;
        this.dataDir = dataDir;
    }

    // ==================== USER OPERATIONS ====================

    @Override
    public List<UserAccount> getAllUsers() throws RemoteException {
        return dataStore.getAllUsers();
    }

    @Override
    public UserAccount getUserById(int userId) throws RemoteException {
        return dataStore.getUserById(userId);
    }

    @Override
    public UserAccount getUserByUsername(String username) throws RemoteException {
        return dataStore.getUserByUsername(username);
    }

    @Override
    public synchronized int addUser(UserAccount user) throws RemoteException {
        return dataStore.addUser(user);
    }

    @Override
    public synchronized boolean updateUser(UserAccount user) throws RemoteException {
        return dataStore.updateUser(user);
    }

    @Override
    public synchronized boolean removeUser(int userId) throws RemoteException {
        return dataStore.removeUser(userId);
    }

    // ==================== EMPLOYEE OPERATIONS ====================

    @Override
    public Employee getEmployee(int employeeId) throws RemoteException {
        return dataStore.getEmployee(employeeId);
    }

    @Override
    public List<Employee> getAllEmployees() throws RemoteException {
        return dataStore.getAllEmployees();
    }

    @Override
    public List<Employee> searchEmployees(String query) throws RemoteException {
        return dataStore.searchEmployees(query);
    }

    @Override
    public synchronized int addEmployee(Employee emp) throws RemoteException {
        return dataStore.addEmployee(emp);
    }

    @Override
    public synchronized boolean updateEmployee(Employee emp) throws RemoteException {
        return dataStore.updateEmployee(emp);
    }

    // ==================== FAMILY MEMBER OPERATIONS ====================

    @Override
    public List<FamilyMember> getFamilyMembers(int employeeId) throws RemoteException {
        return dataStore.getFamilyMembers(employeeId);
    }

    @Override
    public synchronized int addFamilyMember(FamilyMember member) throws RemoteException {
        return dataStore.addFamilyMember(member);
    }

    @Override
    public synchronized boolean updateFamilyMember(FamilyMember member) throws RemoteException {
        return dataStore.updateFamilyMember(member);
    }

    @Override
    public synchronized boolean removeFamilyMember(int familyId) throws RemoteException {
        return dataStore.removeFamilyMember(familyId);
    }

    // ==================== LEAVE OPERATIONS ====================

    @Override
    public List<LeaveBalance> getLeaveBalances(int employeeId, int year) throws RemoteException {
        return dataStore.getLeaveBalances(employeeId, year);
    }

    @Override
    public List<LeaveBalance> getAllLeaveBalances() throws RemoteException {
        return dataStore.getAllLeaveBalances();
    }

    @Override
    public synchronized int addLeaveBalance(LeaveBalance balance) throws RemoteException {
        return dataStore.addLeaveBalance(balance);
    }

    @Override
    public synchronized boolean updateLeaveBalance(LeaveBalance balance) throws RemoteException {
        return dataStore.updateLeaveBalance(balance);
    }

    @Override
    public synchronized boolean updateLeaveBalance(int employeeId, String leaveType, int year, int additionalUsed) throws RemoteException {
        return dataStore.updateLeaveBalance(employeeId, leaveType, year, additionalUsed);
    }

    @Override
    public List<LeaveApplication> getLeaveApplications(int employeeId) throws RemoteException {
        return dataStore.getLeaveApplications(employeeId);
    }

    @Override
    public List<LeaveApplication> getAllLeaveApplications() throws RemoteException {
        return dataStore.getAllLeaveApplications();
    }

    @Override
    public List<LeaveApplication> getPendingLeaveApplications() throws RemoteException {
        return dataStore.getPendingLeaveApplications();
    }

    @Override
    public synchronized int addLeaveApplication(LeaveApplication app) throws RemoteException {
        return dataStore.addLeaveApplication(app);
    }

    @Override
    public synchronized boolean updateLeaveApplication(LeaveApplication app) throws RemoteException {
        return dataStore.updateLeaveApplication(app);
    }

    @Override
    public List<LeaveApplication> getLeaveApplicationsByYear(int employeeId, int year) throws RemoteException {
        return dataStore.getLeaveApplicationsByYear(employeeId, year);
    }

    @Override
    public LeaveApplication getLeaveApplication(int leaveId) throws RemoteException {
        return dataStore.getLeaveApplication(leaveId);
    }

    // ==================== PROFILE UPDATE OPERATIONS ====================

    @Override
    public List<Object> getProfileUpdateRequests() throws RemoteException {
        return dataStore.getProfileUpdateRequests();
    }

    @Override
    public synchronized int addProfileUpdateRequest(int employeeId, String fieldName, 
                                                      String oldValue, String newValue) throws RemoteException {
        return dataStore.addProfileUpdateRequest(employeeId, fieldName, oldValue, newValue);
    }

    @Override
    public synchronized boolean approveProfileUpdate(int requestId, int reviewedBy) throws RemoteException {
        return dataStore.approveProfileUpdate(requestId, reviewedBy);
    }

    @Override
    public synchronized boolean rejectProfileUpdate(int requestId) throws RemoteException {
        return dataStore.rejectProfileUpdate(requestId);
    }

    @Override
    public List<String[]> getPendingProfileUpdates() throws RemoteException {
        return dataStore.getPendingProfileUpdates();
    }

    @Override
    public synchronized boolean updateProfileRequest(int requestId, String status, int reviewedBy) throws RemoteException {
        return dataStore.updateProfileRequest(requestId, status, reviewedBy);
    }

    // ==================== PAYROLL OPERATIONS ====================

    @Override
    public List<PayrollRecord> getPayrollRecords(int employeeId) throws RemoteException {
        return dataStore.getPayrollRecords(employeeId);
    }

    @Override
    public List<PayrollRecord> getYearlyPayroll(int employeeId, int year) throws RemoteException {
        return dataStore.getYearlyPayroll(employeeId, year);
    }

    @Override
    public PayrollRecord getPayrollRecord(int employeeId, int month, int year) throws RemoteException {
        return dataStore.getPayrollRecord(employeeId, month, year);
    }

    @Override
    public List<PayrollRecord> getAllPayrollRecords() throws RemoteException {
        return dataStore.getAllPayrollRecords();
    }

    @Override
    public synchronized int addPayrollRecord(PayrollRecord record) throws RemoteException {
        return dataStore.addPayrollRecord(record);
    }

    @Override
    public synchronized boolean updatePayrollRecord(PayrollRecord record) throws RemoteException {
        return dataStore.updatePayrollRecord(record);
    }

    // ==================== AUDIT OPERATIONS ====================

    @Override
    public List<AuditLogEntry> getAuditLog() throws RemoteException {
        return dataStore.getAuditLog();
    }

    @Override
    public List<AuditLogEntry> getAuditLogForUser(int userId) throws RemoteException {
        return dataStore.getAuditLogForUser(userId);
    }

    @Override
    public List<AuditLogEntry> getAuditLogForTable(String tableName) throws RemoteException {
        return dataStore.getAuditLogForTable(tableName);
    }

    @Override
    public synchronized void logAudit(int userId, String username, String role, String action, 
                                       String targetTable, int targetId, String details) throws RemoteException {
        dataStore.addAuditLog(userId, username, role, action, targetTable, targetId, details);
        System.out.println("[AUDIT] " + username + " (" + role + ") - " + action + " on " + 
                          targetTable + " (#" + targetId + "): " + details);
    }

    // ==================== REPLICATION (Fault Tolerance) ====================

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    @Override
    public synchronized void replicateAppend(String fileName, String csvLine) throws RemoteException {
        try {
            Path path = Paths.get(dataDir, fileName);
            if (!Files.exists(path)) {
                System.err.println("[REPLICATION] File not found, skipping: " + fileName);
                return;
            }
            Files.writeString(path, csvLine + "\n", StandardOpenOption.APPEND);
            System.out.println("[REPLICATION] APPEND -> " + fileName);
        } catch (IOException e) {
            System.err.println("[REPLICATION] Append error: " + e.getMessage());
            throw new RemoteException("Replication append failed: " + e.getMessage());
        }
    }

    @Override
    public synchronized void replicateWrite(String fileName, String header, List<String> dataLines) throws RemoteException {
        try {
            Path path = Paths.get(dataDir, fileName);
            List<String> allLines = new ArrayList<>();
            allLines.add(header);
            allLines.addAll(dataLines);
            Files.write(path, allLines);
            System.out.println("[REPLICATION] WRITE  -> " + fileName + " (" + dataLines.size() + " records)");
        } catch (IOException e) {
            System.err.println("[REPLICATION] Write error: " + e.getMessage());
            throw new RemoteException("Replication write failed: " + e.getMessage());
        }
    }
}
