package common.interfaces;

import common.models.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for Database Service.
 * Handles all CSV file operations and data persistence.
 * Also provides replication endpoints for primary-backup fault tolerance.
 */
public interface DatabaseService extends Remote {

    // ==================== USER OPERATIONS ====================
    
    List<UserAccount> getAllUsers() throws RemoteException;
    UserAccount getUserById(int userId) throws RemoteException;
    UserAccount getUserByUsername(String username) throws RemoteException;
    int addUser(UserAccount user) throws RemoteException;
    boolean updateUser(UserAccount user) throws RemoteException;
    
    // ==================== EMPLOYEE OPERATIONS ====================
    
    Employee getEmployee(int employeeId) throws RemoteException;
    List<Employee> getAllEmployees() throws RemoteException;
    List<Employee> searchEmployees(String query) throws RemoteException;
    int addEmployee(Employee emp) throws RemoteException;
    boolean updateEmployee(Employee emp) throws RemoteException;
    
    // ==================== FAMILY MEMBER OPERATIONS ====================
    
    List<FamilyMember> getFamilyMembers(int employeeId) throws RemoteException;
    int addFamilyMember(FamilyMember member) throws RemoteException;
    boolean updateFamilyMember(FamilyMember member) throws RemoteException;
    boolean removeFamilyMember(int familyId) throws RemoteException;
    
    // ==================== LEAVE OPERATIONS ====================
    
    List<LeaveBalance> getLeaveBalances(int employeeId, int year) throws RemoteException;
    List<LeaveBalance> getAllLeaveBalances() throws RemoteException;
    int addLeaveBalance(LeaveBalance balance) throws RemoteException;
    boolean updateLeaveBalance(LeaveBalance balance) throws RemoteException;
    boolean updateLeaveBalance(int employeeId, String leaveType, int year, int additionalUsed) throws RemoteException;
    
    List<LeaveApplication> getLeaveApplications(int employeeId) throws RemoteException;
    List<LeaveApplication> getAllLeaveApplications() throws RemoteException;
    List<LeaveApplication> getLeaveApplicationsByYear(int employeeId, int year) throws RemoteException;
    LeaveApplication getLeaveApplication(int leaveId) throws RemoteException;
    List<LeaveApplication> getPendingLeaveApplications() throws RemoteException;
    int addLeaveApplication(LeaveApplication app) throws RemoteException;
    boolean updateLeaveApplication(LeaveApplication app) throws RemoteException;
    
    // ==================== PROFILE UPDATE OPERATIONS ====================
    
    List<Object> getProfileUpdateRequests() throws RemoteException;
    List<String[]> getPendingProfileUpdates() throws RemoteException;
    int addProfileUpdateRequest(int employeeId, String fieldName, String oldValue, String newValue) throws RemoteException;
    boolean approveProfileUpdate(int requestId, int reviewedBy) throws RemoteException;
    boolean rejectProfileUpdate(int requestId) throws RemoteException;
    boolean updateProfileRequest(int requestId, String status, int reviewedBy) throws RemoteException;
    
    // ==================== PAYROLL OPERATIONS ====================
    
    List<PayrollRecord> getPayrollRecords(int employeeId) throws RemoteException;
    List<PayrollRecord> getYearlyPayroll(int employeeId, int year) throws RemoteException;
    PayrollRecord getPayrollRecord(int employeeId, int month, int year) throws RemoteException;
    List<PayrollRecord> getAllPayrollRecords() throws RemoteException;
    int addPayrollRecord(PayrollRecord record) throws RemoteException;
    boolean updatePayrollRecord(PayrollRecord record) throws RemoteException;
    
    // ==================== USER OPERATIONS ====================
    
    boolean removeUser(int userId) throws RemoteException;
    
    // ==================== AUDIT OPERATIONS ====================
    
    List<AuditLogEntry> getAuditLog() throws RemoteException;
    List<AuditLogEntry> getAuditLogForUser(int userId) throws RemoteException;
    List<AuditLogEntry> getAuditLogForTable(String tableName) throws RemoteException;
    void logAudit(int userId, String username, String role, String action, String targetTable, 
                  int targetId, String details) throws RemoteException;

    // ==================== REPLICATION (Fault Tolerance) ====================

    /** Health check — returns true if this database server is alive. */
    boolean ping() throws RemoteException;

    /** Receive a single appended CSV line from the primary. */
    void replicateAppend(String fileName, String csvLine) throws RemoteException;

    /** Receive a full CSV file rewrite from the primary. */
    void replicateWrite(String fileName, String header, List<String> dataLines) throws RemoteException;
}
