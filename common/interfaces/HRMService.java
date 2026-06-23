package common.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import common.models.*;

/**
 * Remote interface for the Human Resource Management System.
 * Provides methods for both Employee self-service and HR staff operations.
 */
public interface HRMService extends Remote {

    // ==================== EMPLOYEE SELF-SERVICE ====================

    /** Get employee profile by employee ID */
    Employee getProfile(int employeeId, String sessionToken) throws RemoteException;

    /** Request a profile field update (requires HR approval) */
    boolean requestProfileUpdate(int employeeId, String fieldName, String oldValue, String newValue, String sessionToken) throws RemoteException;

    /** Get family members for an employee */
    List<FamilyMember> getFamilyMembers(int employeeId, String sessionToken) throws RemoteException;

    /** Add a new family member */
    boolean addFamilyMember(FamilyMember member, String sessionToken) throws RemoteException;

    /** Update an existing family member */
    boolean updateFamilyMember(FamilyMember member, String sessionToken) throws RemoteException;

    /** Remove a family member */
    boolean removeFamilyMember(int familyId, int employeeId, String sessionToken) throws RemoteException;

    /** Get leave balance for an employee for a specific year */
    List<LeaveBalance> getLeaveBalances(int employeeId, int year, String sessionToken) throws RemoteException;

    /** Submit a leave application */
    boolean applyForLeave(LeaveApplication application, String sessionToken) throws RemoteException;

    /** Get all leave applications for an employee */
    List<LeaveApplication> getMyLeaveApplications(int employeeId, String sessionToken) throws RemoteException;

    // ==================== HR STAFF OPERATIONS ====================

    /** Register a new employee (HR only) */
    int registerEmployee(Employee emp, String sessionToken) throws RemoteException;

    /** Search employees by name or IC (HR only) */
    List<Employee> searchEmployees(String query, String sessionToken) throws RemoteException;

    /** Get all employees (HR only) */
    List<Employee> getAllEmployees(String sessionToken) throws RemoteException;

    /** Get pending profile update requests (HR only) */
    List<String[]> getPendingProfileUpdates(String sessionToken) throws RemoteException;

    /** Approve a profile update request (HR only) */
    boolean approveProfileUpdate(int requestId, String sessionToken) throws RemoteException;

    /** Reject a profile update request (HR only) */
    boolean rejectProfileUpdate(int requestId, String sessionToken) throws RemoteException;

    /** Get all pending leave applications (HR only) */
    List<LeaveApplication> getPendingLeaveApplications(String sessionToken) throws RemoteException;

    /** Approve a leave application (HR only) */
    boolean approveLeave(int leaveId, String sessionToken) throws RemoteException;

    /** Reject a leave application (HR only) */
    boolean rejectLeave(int leaveId, String sessionToken) throws RemoteException;

    /** Generate yearly report for an employee (HR only) */
    YearlyReport generateYearlyReport(int employeeId, int year, String sessionToken) throws RemoteException;

    // ==================== ADMIN OPERATIONS ====================

    /** Get all user accounts (Admin only) */
    List<UserAccount> getAllUsers(String sessionToken) throws RemoteException;

    /** Add a new user account (Admin only) */
    boolean addUser(String username, String password, String role, int employeeId, String sessionToken) throws RemoteException;

    /** Update a user account (Admin only) */
    boolean updateUser(int userId, String username, String role, boolean isActive, String sessionToken) throws RemoteException;

    /** Remove a user account (Admin only) */
    boolean removeUser(int userId, String sessionToken) throws RemoteException;

    /** Get audit log entries (Admin only) */
    List<AuditLogEntry> getAuditLog(String sessionToken) throws RemoteException;
}
