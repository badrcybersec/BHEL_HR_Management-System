package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing using SHA-256.
 * Uses salted hashing for security.
 */
public class PasswordHasher {

    /**
     * Hash a password with a random salt.
     * Returns "salt:hash" format.
     */
    public static String hashPassword(String password) {
        try {
            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            String saltStr = Base64.getEncoder().encodeToString(salt);

            // Hash password + salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes());
            String hashStr = Base64.getEncoder().encodeToString(hash);

            return saltStr + ":" + hashStr;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Verify a password against a stored "salt:hash" string.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) return false;

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String expectedHash = parts[1];

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes());
            String actualHash = Base64.getEncoder().encodeToString(hash);

            return expectedHash.equals(actualHash);
        } catch (Exception e) {
            return false;
        }
    }
}
