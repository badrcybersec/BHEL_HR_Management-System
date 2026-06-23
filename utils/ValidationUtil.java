package utils;

/**
 * Utility class for validating user inputs.
 * Enforces Malaysian IC/Passport format and other field validations.
 */
public class ValidationUtil {

    /**
     * Validate Malaysian IC number.
     * Format: YYMMDD-PB-####  (12 digits with or without dashes)
     */
    public static boolean validateIC(String ic) {
        if (ic == null || ic.trim().isEmpty()) return false;
        String clean = ic.replaceAll("-", "");
        if (!clean.matches("\\d{12}")) return false;
        int month = Integer.parseInt(clean.substring(2, 4));
        int day = Integer.parseInt(clean.substring(4, 6));
        return month >= 1 && month <= 12 && day >= 1 && day <= 31;
    }

    /**
     * Validate Passport number.
     * Format: Starts with a letter, followed by 6-8 alphanumeric characters.
     */
    public static boolean validatePassport(String passport) {
        if (passport == null || passport.trim().isEmpty()) return false;
        return passport.matches("[A-Za-z][A-Za-z0-9]{6,8}");
    }

    /** Validate IC or Passport — tries IC first, then passport. */
    public static boolean validateIcOrPassport(String value) {
        return validateIC(value) || validatePassport(value);
    }

    /** Validate that a required string field is not empty. */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Validate email format (basic check). */
    public static boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) return true;
        return email.matches("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    }

    /** Validate phone number (Malaysian format only: +60 followed by 10-11 digits). */
    public static boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return true;
        String clean = phone.replaceAll("[\\s\\-()]", "");
        // Must be +60 followed by 10-11 digits (Malaysian format)
        return clean.matches("\\+60\\d{10,11}");
    }

    /** Validate date format (YYYY-MM-DD). */
    public static boolean validateDate(String date) {
        if (date == null || date.trim().isEmpty()) return false;
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    /** Validate leave days (must be positive and reasonable). */
    public static boolean validateLeaveDays(int days) {
        return days > 0 && days <= 365;
    }

    /**
     * Validate username.
     * Allows letters, digits, underscores, dots, and hyphens. 3-30 chars.
     * FIX: Previous regex [a-zA-Z0-9_] rejected dots in auto-generated
     * usernames like "ahmad.ibrahim", causing add-user to fail.
     */
    public static boolean validateUsername(String username) {
        if (username == null) return false;
        return username.matches("[a-zA-Z0-9._\\-]{3,30}");
    }

    /** Validate password strength (minimum 6 characters). */
    public static boolean validatePassword(String password) {
        return password != null && password.length() >= 6;
    }

    /** Format IC number with dashes: YYMMDD-PB-#### */
    public static String formatIC(String ic) {
        String clean = ic.replaceAll("-", "");
        if (clean.length() == 12) {
            return clean.substring(0, 6) + "-" + clean.substring(6, 8) + "-" + clean.substring(8);
        }
        return ic;
    }
}
