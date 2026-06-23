package server;

import common.interfaces.HRMService;
import common.interfaces.DatabaseService;
import common.models.*;
import utils.ValidationUtil;
import utils.PasswordHasher;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the HRM Service.
 * Delegates data operations to remote DatabaseService.
 * All authenticated operations are logged through AuditLogger.
 * Supports SSL/TLS when socket factories are provided.
 */
public class HRMServiceImpl extends UnicastRemoteObject implements HRMService {

    private final DatabaseService dbService;
    private final AuthServiceImpl authService;
    private final AuditLogger auditLogger;

    public HRMServiceImpl(DatabaseService dbService, AuthServiceImpl authService,
                          AuditLogger auditLogger, int port,
                          RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(port, csf, ssf);
        this.dbService = dbService;
        this.authService = authService;
        this.auditLogger = auditLogger;
    }

    // ==================== SESSION VALIDATION ====================

    private UserAccount requireAuth(String token) throws RemoteException {
        UserAccount user = authService.validateSession(token);
        if (user == null) throw new RemoteException("Unauthorized: Invalid or expired session");
        return user;
    }

    private UserAccount requireRole(String token, String... roles) throws RemoteException {
        UserAccount user = requireAuth(token);
        for (String role : roles) {
            if (user.getRole().equals(role)) return user;
        }
        throw new RemoteException("Forbidden: Insufficient permissions. Required: " + String.join("/", roles));
    }

    // ==================== EMPLOYEE SELF-SERVICE ====================

    @Override
    public Employee getProfile(int employeeId, String sessionToken) throws RemoteException {
        UserAccount user = requireAuth(sessionToken);
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != employeeId)
            throw new RemoteException("Forbidden: Cannot view another employee's profile");
        Employee emp = dbService.getEmployee(employeeId);
        if (emp == null) throw new RemoteException("Employee not found: " + employeeId);
        return emp;
    }

    @Override
    public synchronized boolean requestProfileUpdate(int employeeId, String fieldName,
                                                      String oldValue, String newValue, String sessionToken) throws RemoteException {
        UserAccount user = requireAuth(sessionToken);
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != employeeId)
            throw new RemoteException("Forbidden");
        if ("email".equals(fieldName) && !ValidationUtil.validateEmail(newValue))
            throw new RemoteException("Invalid email format");
        if ("phone".equals(fieldName) && !ValidationUtil.validatePhone(newValue))
            throw new RemoteException("Invalid phone format");

        int requestId = dbService.addProfileUpdateRequest(employeeId, fieldName, oldValue, newValue);
        auditLogger.log(user, "REQUEST_PROFILE_UPDATE", "employees", employeeId,
            fieldName + ": '" + oldValue + "' -> '" + newValue + "'");
        return true;
    }

    @Override
    public List<FamilyMember> getFamilyMembers(int employeeId, String sessionToken) throws RemoteException {
        UserAccount user = requireAuth(sessionToken);
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != employeeId)
            throw new RemoteException("Forbidden");
        return dbService.getFamilyMembers(employeeId);
    }

    @Override
    public synchronized boolean addFamilyMember(FamilyMember member, String sessionToken) throws RemoteException {
        UserAccount user = requireAuth(sessionToken);
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != member.getEmployeeId())
            throw new RemoteException("Forbidden");
        if (!ValidationUtil.isNotEmpty(member.getName()))
            throw new RemoteException("Family member name is required");
        if (!ValidationUtil.isNotEmpty(member.getRelationship()))
            throw new RemoteException("Relationship is required");

        int id = dbService.addFamilyMember(member);
        auditLogger.log(user, "ADD_FAMILY_MEMBER", "family_members", id,
            member.getName() + " (" + member.getRelationship() + ") for emp#" + member.getEmployeeId());
        return true;
    }

    @Override
    public synchronized boolean updateFamilyMember(FamilyMember member, String sessionToken) throws RemoteException {
        UserAccount user = requireAuth(sessionToken);
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != member.getEmployeeId())
            throw new RemoteException("Forbidden");
        boolean success = dbService.updateFamilyMember(member);
        if (success) auditLogger.log(user, "UPDATE_FAMILY_MEMBER", "family_members", member.getFamilyId(), "Updated: " + member.getName());
        return success;
    }

    @Override
    public synchronized boolean removeFamilyMember(int familyId, int employeeId, String sessionToken) throws RemoteException {
        UserAccount user = requireAuth(sessionToken);
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != employeeId)
            throw new RemoteException("Forbidden");
        boolean success = dbService.removeFamilyMember(familyId);
        if (success) auditLogger.log(user, "REMOVE_FAMILY_MEMBER", "family_members", familyId, "Removed family member #" + familyId);
        return success;
    }

    @Override
    public List<LeaveBalance> getLeaveBalances(int employeeId, int year, String sessionToken) throws RemoteException {
        UserAccount user = requireAuth(sessionToken);
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != employeeId)
            throw new RemoteException("Forbidden");
        List<LeaveBalance> balances = dbService.getLeaveBalances(employeeId, year);
        if (balances.isEmpty()) {
            // Initialize leave balances for new employee
            for (String leaveType : new String[]{"annual", "medical", "emergency"}) {
                LeaveBalance lb = new LeaveBalance();
                lb.setEmployeeId(employeeId);
                lb.setLeaveType(leaveType);
                lb.setTotalDays(leaveType.equals("annual") ? 20 : (leaveType.equals("medical") ? 10 : 5));
                lb.setUsedDays(0);
                lb.setYear(year);
                dbService.addLeaveBalance(lb);
            }
            balances = dbService.getLeaveBalances(employeeId, year);
        }
        return balances;
    }

    @Override
    public synchronized boolean applyForLeave(LeaveApplication application, String sessionToken) throws RemoteException {
        UserAccount user = requireAuth(sessionToken);
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != application.getEmployeeId())
            throw new RemoteException("Forbidden");
        if (!ValidationUtil.validateDate(application.getStartDate()) || !ValidationUtil.validateDate(application.getEndDate()))
            throw new RemoteException("Invalid date format. Use YYYY-MM-DD");
        if (!ValidationUtil.validateLeaveDays(application.getDaysRequested()))
            throw new RemoteException("Invalid number of leave days");

        int year = Integer.parseInt(application.getStartDate().substring(0, 4));
        List<LeaveBalance> balances = dbService.getLeaveBalances(application.getEmployeeId(), year);
        LeaveBalance balance = balances.stream()
            .filter(lb -> lb.getLeaveType().equals(application.getLeaveType()))
            .findFirst().orElse(null);
        if (balance == null)
            throw new RemoteException("No leave balance found for type: " + application.getLeaveType());
        if (balance.getRemainingDays() < application.getDaysRequested())
            throw new RemoteException("Insufficient balance. Remaining: " + balance.getRemainingDays());

        application.setStatus("PENDING");
        int leaveId = dbService.addLeaveApplication(application);
        auditLogger.log(user, "APPLY_LEAVE", "leave_applications", leaveId,
            application.getLeaveType() + ": " + application.getStartDate() + " to " + application.getEndDate());
        return true;
    }

    @Override
    public List<LeaveApplication> getMyLeaveApplications(int employeeId, String sessionToken) throws RemoteException {
        UserAccount user = requireAuth(sessionToken);
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != employeeId)
            throw new RemoteException("Forbidden");
        return dbService.getLeaveApplications(employeeId);
    }

    // ==================== HR STAFF OPERATIONS ====================

    @Override
    public synchronized int registerEmployee(Employee emp, String sessionToken) throws RemoteException {
        UserAccount user = requireRole(sessionToken, "HR", "ADMIN");
        if (!ValidationUtil.isNotEmpty(emp.getFirstName()))
            throw new RemoteException("First name is required");
        if (!ValidationUtil.isNotEmpty(emp.getLastName()))
            throw new RemoteException("Last name is required");
        if (!ValidationUtil.validateIcOrPassport(emp.getIcPassport()))
            throw new RemoteException("Invalid IC/Passport format");
        if (emp.getEmail() != null && !ValidationUtil.validateEmail(emp.getEmail()))
            throw new RemoteException("Invalid email format");
        if (emp.getPhone() != null && !ValidationUtil.validatePhone(emp.getPhone()))
            throw new RemoteException("Invalid phone format. Phone must be +60 followed by 10-11 digits (e.g., +601123456789)");

        int empId = dbService.addEmployee(emp);
        if (empId == -1)
            throw new RemoteException("Duplicate IC/Passport number");

        // Auto-create employee user account
        String defaultPassword = emp.getIcPassport().replaceAll("-", "");
        String hash = PasswordHasher.hashPassword(defaultPassword);
        String username = emp.getFirstName().toLowerCase() + "." + emp.getLastName().toLowerCase();
        int suffix = 1;
        String base = username;
        while (dbService.getUserByUsername(username) != null) username = base + suffix++;
        dbService.addUser(new UserAccount(0, username, hash, "EMPLOYEE", empId, true));

        auditLogger.log(user, "REGISTER_EMPLOYEE", "employees", empId,
            emp.getFullName() + " (IC: " + emp.getIcPassport() + "), user: " + username);
        return empId;
    }

    @Override
    public List<Employee> searchEmployees(String query, String sessionToken) throws RemoteException {
        requireRole(sessionToken, "HR", "ADMIN");
        return dbService.searchEmployees(query);
    }

    @Override
    public List<Employee> getAllEmployees(String sessionToken) throws RemoteException {
        requireRole(sessionToken, "HR", "ADMIN");
        return dbService.getAllEmployees();
    }

    @Override
    public List<String[]> getPendingProfileUpdates(String sessionToken) throws RemoteException {
        requireRole(sessionToken, "HR", "ADMIN");
        return dbService.getPendingProfileUpdates();
    }

    @Override
    public synchronized boolean approveProfileUpdate(int requestId, String sessionToken) throws RemoteException {
        UserAccount user = requireRole(sessionToken, "HR", "ADMIN");
        // Request retrieval - find the matching request from the list
        List<String[]> allRequests = dbService.getPendingProfileUpdates();
        String[] request = null;
        for (String[] arr : allRequests) {
            if (arr.length > 0 && Integer.parseInt(arr[0].trim()) == requestId) {
                request = arr;
                break;
            }
        }
        if (request == null) throw new RemoteException("Request not found");

        int employeeId = Integer.parseInt(request[1].trim());
        String fieldName = request[2].trim();
        String newValue = request[4].trim();

        Employee emp = dbService.getEmployee(employeeId);
        if (emp == null) throw new RemoteException("Employee not found");

        switch (fieldName.toLowerCase()) {
            case "email": emp.setEmail(newValue); break;
            case "phone": emp.setPhone(newValue); break;
            case "department": emp.setDepartment(newValue); break;
            case "position": emp.setPosition(newValue); break;
            case "firstname": emp.setFirstName(newValue); break;
            case "lastname": emp.setLastName(newValue); break;
            default: throw new RemoteException("Unknown field: " + fieldName);
        }
        dbService.updateEmployee(emp);
        dbService.updateProfileRequest(requestId, "APPROVED", user.getUserId());
        auditLogger.log(user, "APPROVE_PROFILE_UPDATE", "profile_updates", requestId,
            "emp#" + employeeId + ": " + fieldName + " = " + newValue);
        return true;
    }

    @Override
    public synchronized boolean rejectProfileUpdate(int requestId, String sessionToken) throws RemoteException {
        UserAccount user = requireRole(sessionToken, "HR", "ADMIN");
        dbService.updateProfileRequest(requestId, "REJECTED", user.getUserId());
        auditLogger.log(user, "REJECT_PROFILE_UPDATE", "profile_updates", requestId, "Rejected #" + requestId);
        return true;
    }

    @Override
    public List<LeaveApplication> getPendingLeaveApplications(String sessionToken) throws RemoteException {
        requireRole(sessionToken, "HR", "ADMIN");
        List<LeaveApplication> pending = dbService.getPendingLeaveApplications();
        for (LeaveApplication la : pending) {
            Employee emp = dbService.getEmployee(la.getEmployeeId());
            if (emp != null) la.setEmployeeName(emp.getFullName());
        }
        return pending;
    }

    @Override
    public synchronized boolean approveLeave(int leaveId, String sessionToken) throws RemoteException {
        UserAccount user = requireRole(sessionToken, "HR", "ADMIN");
        LeaveApplication app = dbService.getLeaveApplication(leaveId);
        if (app == null) throw new RemoteException("Leave application not found");
        if (!"PENDING".equals(app.getStatus())) throw new RemoteException("Already processed");

        int year = Integer.parseInt(app.getStartDate().substring(0, 4));
        if (!dbService.updateLeaveBalance(app.getEmployeeId(), app.getLeaveType(), year, app.getDaysRequested()))
            throw new RemoteException("Failed to update leave balance");

        app.setStatus("APPROVED");
        app.setReviewedBy(user.getUserId());
        app.setReviewDate(LocalDateTime.now().toString());
        dbService.updateLeaveApplication(app);

        Employee emp = dbService.getEmployee(app.getEmployeeId());
        auditLogger.log(user, "APPROVE_LEAVE", "leave_applications", leaveId,
            "Approved " + app.getLeaveType() + " for " + (emp != null ? emp.getFullName() : "emp#" + app.getEmployeeId()));
        return true;
    }

    @Override
    public synchronized boolean rejectLeave(int leaveId, String sessionToken) throws RemoteException {
        UserAccount user = requireRole(sessionToken, "HR", "ADMIN");
        LeaveApplication app = dbService.getLeaveApplication(leaveId);
        if (app == null) throw new RemoteException("Leave application not found");
        if (!"PENDING".equals(app.getStatus())) throw new RemoteException("Already processed");

        app.setStatus("REJECTED");
        app.setReviewedBy(user.getUserId());
        app.setReviewDate(LocalDateTime.now().toString());
        dbService.updateLeaveApplication(app);
        auditLogger.log(user, "REJECT_LEAVE", "leave_applications", leaveId, "Rejected leave #" + leaveId);
        return true;
    }

    @Override
    public YearlyReport generateYearlyReport(int employeeId, int year, String sessionToken) throws RemoteException {
        UserAccount user = requireRole(sessionToken, "HR", "ADMIN");
        Employee emp = dbService.getEmployee(employeeId);
        if (emp == null) throw new RemoteException("Employee not found");

        YearlyReport report = new YearlyReport(emp,
            dbService.getFamilyMembers(employeeId),
            dbService.getLeaveBalances(employeeId, year),
            dbService.getLeaveApplicationsByYear(employeeId, year),
            year, LocalDateTime.now().toString());

        auditLogger.log(user, "GENERATE_REPORT", "employees", employeeId,
            "Yearly report for " + emp.getFullName() + " (" + year + ")");
        return report;
    }

    // ==================== ADMIN OPERATIONS ====================

    @Override
    public List<UserAccount> getAllUsers(String sessionToken) throws RemoteException {
        requireRole(sessionToken, "ADMIN");
        return dbService.getAllUsers();
    }

    @Override
    public synchronized boolean addUser(String username, String password, String role,
                                         int employeeId, String sessionToken) throws RemoteException {
        UserAccount admin = requireRole(sessionToken, "ADMIN");
        if (!ValidationUtil.validateUsername(username))
            throw new RemoteException("Invalid username. Use 3-30 chars: letters, digits, dots, hyphens.");
        if (!ValidationUtil.validatePassword(password))
            throw new RemoteException("Password must be at least 6 characters.");
        if (dbService.getUserByUsername(username) != null)
            throw new RemoteException("Username already exists: " + username);

        // Ensure employee exists before linking user to it. Prevents admins
        // from guessing/pre-allocating employee IDs which can collide with
        // HR-created employees (avoids duplicate employee_id usage).
        if ("EMPLOYEE".equals(role)) {
            if (employeeId <= 0 || dbService.getEmployee(employeeId) == null) {
                throw new RemoteException("Employee not found: " + employeeId + ". Register the employee first via HR.");
            }
        }

        String hash = PasswordHasher.hashPassword(password);
        int userId = dbService.addUser(new UserAccount(0, username, hash, role, employeeId, true));
        auditLogger.log(admin, "ADD_USER", "users", userId, "Created user: " + username + " [" + role + "]");
        return true;
    }

    @Override
    public synchronized boolean updateUser(int userId, String username, String role,
                                            boolean isActive, String sessionToken) throws RemoteException {
        UserAccount admin = requireRole(sessionToken, "ADMIN");
        UserAccount target = dbService.getUserById(userId);
        if (target == null) throw new RemoteException("User not found");
        target.setUsername(username);
        target.setRole(role);
        target.setActive(isActive);
        boolean success = dbService.updateUser(target);
        if (success) auditLogger.log(admin, "UPDATE_USER", "users", userId,
            "Updated: " + username + " [" + role + "] active=" + isActive);
        return success;
    }

    @Override
    public synchronized boolean removeUser(int userId, String sessionToken) throws RemoteException {
        UserAccount admin = requireRole(sessionToken, "ADMIN");
        boolean success = dbService.removeUser(userId);
        if (success) auditLogger.log(admin, "DEACTIVATE_USER", "users", userId, "Deactivated user #" + userId);
        return success;
    }

    @Override
    public List<AuditLogEntry> getAuditLog(String sessionToken) throws RemoteException {
        requireRole(sessionToken, "ADMIN");
        return dbService.getAuditLog();
    }
}
