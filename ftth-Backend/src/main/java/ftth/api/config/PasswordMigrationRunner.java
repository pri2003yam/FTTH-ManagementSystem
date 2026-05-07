package ftth.api.config;

import ftth.config.DbConnection;
import ftth.util.PasswordUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordMigrationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<long[]> toUpdate = new ArrayList<>();
        List<String> plainPasswords = new ArrayList<>();

        // Fetch all users whose password is NOT already BCrypt hashed
        String selectSql = "SELECT user_id, password_hash FROM users WHERE password_hash NOT LIKE '$2a$%' AND password_hash NOT LIKE '$2b$%'";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                toUpdate.add(new long[]{rs.getLong("user_id")});
                plainPasswords.add(rs.getString("password_hash"));
            }
        }

        if (toUpdate.isEmpty()) {
            System.out.println("[PasswordMigration] All passwords are already BCrypt hashed.");
            return;
        }

        System.out.println("[PasswordMigration] Hashing " + toUpdate.size() + " plain text password(s)...");

        // Update each with BCrypt hash
        String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            for (int i = 0; i < toUpdate.size(); i++) {
                ps.setString(1, PasswordUtil.hash(plainPasswords.get(i)));
                ps.setLong(2, toUpdate.get(i)[0]);
                ps.executeUpdate();
            }
        }

        System.out.println("[PasswordMigration] Done. All passwords are now BCrypt hashed.");
    }
}
