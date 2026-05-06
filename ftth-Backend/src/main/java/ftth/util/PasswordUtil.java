package ftth.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {}

    // Hash a password using BCrypt
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    // Smart match — handles BCrypt and plain text
    public static boolean matches(String plainPassword, String storedPassword) {
        if (storedPassword == null || plainPassword == null) return false;
        try {
            // BCrypt hash
            if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
                return BCrypt.checkpw(plainPassword, storedPassword);
            }
        } catch (Exception e) {
            return false;
        }
        // Plain text (CLI compatibility)
        return plainPassword.equals(storedPassword);
    }
}
