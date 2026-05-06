package ftth.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {}

    // Hash a password using BCrypt
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    // Smart match — works for both BCrypt hashed and plain text passwords
    // If DB has BCrypt hash (starts with $2a$) → use BCrypt check
    // If DB has plain text → use plain text comparison (CLI compatibility)
    public static boolean matches(String plainPassword, String storedPassword) {
        if (storedPassword == null || plainPassword == null) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            return BCrypt.checkpw(plainPassword, storedPassword);
        }
        return plainPassword.equals(storedPassword);
    }
}
