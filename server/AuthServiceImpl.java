package server;

import common.interfaces.AuthService;
import common.interfaces.DatabaseService;
import common.models.UserAccount;
import utils.PasswordHasher;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the Authentication Service.
 * Manages login/logout and session tokens.
 * Delegates data operations to remote DatabaseService.
 * Supports SSL/TLS when socket factories are provided.
 */
public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {

    private final DatabaseService dbService;
    private final AuditLogger auditLogger;
    private final Map<String, UserAccount> activeSessions = new ConcurrentHashMap<>();

    public AuthServiceImpl(DatabaseService dbService, AuditLogger auditLogger,
                           int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(port, csf, ssf);
        this.dbService = dbService;
        this.auditLogger = auditLogger;
    }

    @Override
    public String login(String username, String password) throws RemoteException {
        System.out.println("[AUTH] Login attempt: " + username);

        UserAccount user = dbService.getUserByUsername(username);
        if (user == null) {
            auditLogger.logFailedLogin(username);
            return null;
        }
        if (!user.isActive()) {
            auditLogger.logFailedLogin(username);
            return null;
        }
        if (!PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
            auditLogger.logFailedLogin(username);
            return null;
        }

        String token = UUID.randomUUID().toString();
        activeSessions.put(token, user);
        auditLogger.logLogin(user);
        System.out.println("[AUTH] Login successful: " + username + " [" + user.getRole() + "]");
        return token;
    }

    @Override
    public boolean logout(String sessionToken) throws RemoteException {
        UserAccount user = activeSessions.remove(sessionToken);
        if (user != null) {
            auditLogger.logLogout(user);
            System.out.println("[AUTH] Logout: " + user.getUsername());
            return true;
        }
        return false;
    }

    @Override
    public UserAccount getCurrentUser(String sessionToken) throws RemoteException {
        return activeSessions.get(sessionToken);
    }

    @Override
    public boolean changePassword(String oldPassword, String newPassword, String sessionToken) throws RemoteException {
        UserAccount user = activeSessions.get(sessionToken);
        if (user == null) return false;

        UserAccount stored = dbService.getUserById(user.getUserId());
        if (stored == null || !PasswordHasher.verifyPassword(oldPassword, stored.getPasswordHash())) {
            return false;
        }

        stored.setPasswordHash(PasswordHasher.hashPassword(newPassword));
        boolean success = dbService.updateUser(stored);
        if (success) {
            activeSessions.put(sessionToken, stored);
            auditLogger.log(user, "CHANGE_PASSWORD", "users", user.getUserId(), "Password changed");
        }
        return success;
    }

    /** Validate a session token. Used by other services internally. */
    public UserAccount validateSession(String sessionToken) {
        return activeSessions.get(sessionToken);
    }

    /** Check if session has a specific role. */
    public boolean hasRole(String sessionToken, String role) {
        UserAccount user = activeSessions.get(sessionToken);
        return user != null && user.getRole().equals(role);
    }
}
