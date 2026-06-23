package server;

import common.interfaces.DatabaseService;
import common.models.UserAccount;
import java.rmi.RemoteException;

/**
 * Audit logging utility.
 * Delegates database operations to remote DatabaseService for 3-tier architecture.
 * Provides convenient high-level logging methods.
 */
public class AuditLogger {

    private final DatabaseService dbService;

    public AuditLogger(DatabaseService dbService) {
        this.dbService = dbService;
    }

    public void log(UserAccount user, String action, String targetTable, int targetId, String details) {
        if (user != null) {
            try {
                dbService.logAudit(user.getUserId(), user.getUsername(), user.getRole(),
                                  action, targetTable, targetId, details);
            } catch (RemoteException e) {
                System.err.println("[AUDIT ERROR] Failed to log action: " + e.getMessage());
            }
        } else {
            try {
                dbService.logAudit(0, "SYSTEM", "SYSTEM", action, targetTable, targetId, details);
            } catch (RemoteException e) {
                System.err.println("[AUDIT ERROR] Failed to log action: " + e.getMessage());
            }
        }
        System.out.println("[AUDIT] " + (user != null ? user.getUsername() : "SYSTEM") + " - " + action + ": " + details);
    }

    public void logLogin(UserAccount user) {
        log(user, "LOGIN", "users", user.getUserId(), "User logged in");
    }

    public void logLogout(UserAccount user) {
        log(user, "LOGOUT", "users", user.getUserId(), "User logged out");
    }

    public void logFailedLogin(String username) {
        try {
            dbService.logAudit(0, username, "UNKNOWN", "FAILED_LOGIN", "users", 0, "Failed login attempt for: " + username);
        } catch (RemoteException e) {
            System.err.println("[AUDIT ERROR] Failed to log failed login: " + e.getMessage());
        }
        System.out.println("[AUDIT] FAILED LOGIN: " + username);
    }

    public void logServerStart(int port, boolean sslEnabled) {
        try {
            dbService.logAudit(0, "SYSTEM", "SYSTEM", "SERVER_START", null, 0,
                            "Application server started on port " + port + (sslEnabled ? " (SSL)" : ""));
        } catch (RemoteException e) {
            System.err.println("[AUDIT ERROR] Failed to log server start: " + e.getMessage());
        }
    }
}
