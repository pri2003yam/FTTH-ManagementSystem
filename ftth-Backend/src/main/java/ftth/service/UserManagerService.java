package ftth.service;
import java.util.List;

import ftth.model.Role;
import ftth.model.User;
import ftth.repository.RoleRepository;
import ftth.repository.UserRepository;
import ftth.util.PasswordUtil;
/**
 * UserManager — handles login, role lookup, and user CRUD.
 *
 * users.txt format (one line per user):
 *   username,password,role
 *
 * Roles  : ADMIN | CSR | MAINT
 * Default seed written on first run if file is missing.
 */
public class UserManagerService {
    private UserRepository userRepository;
    private RoleRepository roleRepository;

public UserManagerService(UserRepository userRepository,RoleRepository roleRepository) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
}

 public User login(String username, String password) {

        User user = userRepository.findByUsername(username);

        if (user == null || !user.isActive()) {
            return null;
        }
    
       if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            return null;
        }


        return user; // ✅ logged-in user
    }
    /**
     * Get Role for logged-in user.
     */
    public Role getRole(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        Role role = roleRepository.findById(user.getRoleId());

        if (role == null) {
            throw new RuntimeException(
                "Role not found for roleId: " + user.getRoleId()
            );
        }

        return role;
    }

  
public List<String[]> listUser() {
    return userRepository.findAllUsers();
}
public void listUsers() {

    System.out.println("\n--- Registered Users ---");
    System.out.printf("%-20s %-20s %-10s%n", "Username", "Password", "Role");
    System.out.println("-".repeat(52));

    List<String[]> users = listUser();

    if (users.isEmpty()) {
        System.out.println("  (no users found)");
    } else {
        for (String[] u : users) {
            System.out.printf("%-20s %-20s %-10s%n", u[0], u[1], u[2]);
        }
    }

    System.out.println();
}
    
public boolean addUser(String username, String password, String role) {

    username = username.trim();
    password = password.trim();
    role     = role.trim().toUpperCase();

    // 🔹 Validate role
    if (!role.equals("CSR") && !role.equals("MAINT")) {
        System.out.println("Invalid role. Must be CSR or MAINT.");
        return false;
    }

    // 🔹 Check duplicate
    if (userRepository.existsByUsername(username)) {
        System.out.println("Username '" + username + "' already exists.");
        return false;
    }

    // 🔹 Get roleId
    Long roleId = userRepository.findRoleIdByCode(role);
    if (roleId == null) {
        System.out.println("Role not found in DB.");
        return false;
    }

    // 🔹 Insert user
    boolean saved = userRepository.insertUser(username, PasswordUtil.hash(password), roleId);

    if (saved) {
        System.out.println("User '" + username + "' added with role " + role + ".");
        return true;
    } else {
        System.out.println("Error adding user.");
        return false;
    }
}
    // =========================================================
    // CHANGE PASSWORD
    // =========================================================
    public boolean changePassword(String username, String newPassword) {

    username = username.trim();
    newPassword = newPassword.trim();

    boolean updated = userRepository.updatePassword(username, PasswordUtil.hash(newPassword));

    if (updated) {
        System.out.println("Password updated successfully.");
        return true;
    } else {
        System.out.println("User not found or update failed.");
        return false;
    }
}

    // =========================================================
    // CHANGE ROLE
    // =========================================================
   
    public boolean changeRole(String username, String newRole) {

    newRole = newRole.trim().toUpperCase();

    // 🔹 Validate role
    if (!newRole.equals("CSR") && !newRole.equals("MAINT")) {
        System.out.println("Invalid role. Must be CSR or MAINT.");
        return false;
    }

    // 🔹 Get roleId from DB
    Long roleId = userRepository.findRoleIdByCode(newRole);

    if (roleId == null) {
        System.out.println("Role not found in DB.");
        return false;
    }

    // 🔹 Update role in DB
    boolean updated = userRepository.updateUserRole(username, roleId);

    if (updated) {
        System.out.println("Role updated successfully.");
        return true;
    } else {
        System.out.println("User not found or update failed.");
        return false;
    }
}
    // =========================================================
    // DELETE a user
    // =========================================================    
public boolean deleteUser(String username) {
    username = username.trim();
    // 🔹 Check if user exists
    if (!userRepository.existsByUsername(username)) {
        System.out.println("User '" + username + "' not found.");
        return false;
    }
    // 🔹 Delete user
    boolean deleted = userRepository.deleteUser(username);
    if (deleted) {
        System.out.println("User '" + username + "' deleted.");
        return true;
    } else {
        System.out.println("Delete failed.");
        return false;
    }
}
public void showRoles() {
    List<String> roles = roleRepository.findAllRoles();
    System.out.println("\nAvailable Roles:");
    for (String r : roles) {
        System.out.println(" - " + r);
    }
}
}


   
    