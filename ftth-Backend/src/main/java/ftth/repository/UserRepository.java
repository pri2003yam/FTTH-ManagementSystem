package ftth.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ftth.config.DbConnection;
import ftth.model.User;

public class UserRepository {
public String login(String username, String password) {

    String sql = "SELECT r.role_code " +
                 "FROM users u " +
                 "JOIN roles r ON u.role_id = r.role_id " +
                 "WHERE u.username = ? AND u.password_hash = ? AND u.is_active = TRUE";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, username);
        ps.setString(2, password); // later hash

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getString("role_code").toUpperCase();
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return null; // invalid credentials
}
public List<String[]> findAllUsers() {

    List<String[]> users = new ArrayList<>();

    String sql = "SELECT u.username, u.password_hash, r.role_code, u.is_active " +
                 "FROM users u " +
                 "JOIN roles r ON u.role_id = r.role_id";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            String username = rs.getString("username");
            String password = rs.getString("password_hash");
            String role = rs.getString("role_code");

            String active = rs.getBoolean("is_active") ? "Active" : "Inactive";
            users.add(new String[]{username, password, role, active});
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return users;
}
public boolean existsByUsername(String username) {

    String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, username);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}
public Long findRoleIdByCode(String roleCode) {

    String sql = "SELECT role_id FROM roles WHERE role_code = ?";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, roleCode);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getLong("role_id");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return null;
}
public boolean insertUser(String username, String password, long roleId) {

    String sql = "INSERT INTO users (username, password_hash, role_id, is_active) VALUES (?, ?, ?, TRUE)";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, username);
        ps.setString(2, password); // later hash
        ps.setLong(3, roleId);

        return ps.executeUpdate() > 0;

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}
public boolean updatePassword(String username, String newPassword) {

    String sql = "UPDATE users SET password_hash = ? WHERE username = ?";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, newPassword); // later hash
        ps.setString(2, username);

        return ps.executeUpdate() > 0;

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}
public boolean updateUserRole(String username, long roleId) {

    String sql = "UPDATE users SET role_id = ? WHERE username = ?";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setLong(1, roleId);
        ps.setString(2, username);

        return ps.executeUpdate() > 0;

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}
public boolean toggleActive(String username) {

    String sql = "UPDATE users SET is_active = NOT is_active WHERE username = ?";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, username);
        return ps.executeUpdate() > 0;

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}
public boolean deleteUser(String username) {

    String sql = "DELETE FROM users WHERE username = ?";

    try (Connection con = DbConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, username);

        return ps.executeUpdate() > 0;

    } catch (SQLIntegrityConstraintViolationException e) {
        System.out.println("Cannot delete user '" + username + "': referenced by existing records.");
        return false;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}
private static final String FIND_BY_USERNAME_SQL =
        "SELECT user_id, username, password_hash, role_id, is_active, created_at " +
        "FROM users " +
        "WHERE username = ?";


 public User findByUsername(String username) {

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(FIND_BY_USERNAME_SQL)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                "Error fetching user by username: " + username, e
            );
        }

        return null;
    }


    // ===============================
    // Private Mapper
    // ===============================

private User mapRowToUser(ResultSet rs)
            throws SQLException {

        Long userId = rs.getLong("user_id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        Long roleId = rs.getLong("role_id");
        boolean active = rs.getBoolean("is_active");
        LocalDateTime createdAt =
            rs.getTimestamp("created_at").toLocalDateTime();

        return new User(
            userId,
            username,
            passwordHash,
            roleId,
            active,
            createdAt
        );
    }

}








 