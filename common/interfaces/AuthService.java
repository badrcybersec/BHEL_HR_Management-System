package common.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import common.models.UserAccount;

/**
 * Remote interface for Authentication Service.
 * Handles login, logout, and session management.
 */
public interface AuthService extends Remote {

    /** Login with username and password. Returns session token on success, null on failure. */
    String login(String username, String password) throws RemoteException;

    /** Logout and invalidate session token */
    boolean logout(String sessionToken) throws RemoteException;

    /** Get the current logged-in user from session token */
    UserAccount getCurrentUser(String sessionToken) throws RemoteException;

    /** Change password for the currently logged-in user */
    boolean changePassword(String oldPassword, String newPassword, String sessionToken) throws RemoteException;
}
