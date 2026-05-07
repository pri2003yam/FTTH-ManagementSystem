package ftth.api.controller;

import ftth.repository.RoleRepository;
import ftth.repository.UserRepository;
import ftth.service.UserManagerService;

import ftth.util.PasswordUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UsersApiController {

    private final UserManagerService userManagerService;

    public UsersApiController() {
        UserRepository userRepo = new UserRepository();
        RoleRepository roleRepo = new RoleRepository();
        this.userManagerService = new UserManagerService(userRepo, roleRepo);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> listUsers() {
        List<String[]> raw = userManagerService.listUser();
        List<Map<String, String>> result = new ArrayList<>();
        for (String[] u : raw) {
            Map<String, String> row = new HashMap<>();
            row.put("username", u[0]);
            row.put("role", u[2]);
            row.put("status", u.length > 3 ? u[3] : "Active");
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{username}/toggle-status")
    public ResponseEntity<Map<String, String>> toggleStatus(@PathVariable String username) {
        boolean ok = new UserRepository().toggleActive(username);
        Map<String, String> res = new HashMap<>();
        if (ok) {
            res.put("message", "User status toggled");
            return ResponseEntity.ok(res);
        } else {
            res.put("message", "Failed to toggle status.");
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String role = body.get("role");

        boolean ok = userManagerService.addUser(username, PasswordUtil.hash(password), role);
        Map<String, String> res = new HashMap<>();
        if (ok) {
            res.put("message", "User created successfully");
            return ResponseEntity.ok(res);
        } else {
            res.put("message", "Failed to create user. Username may already exist or role is invalid.");
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PutMapping("/{username}")
    public ResponseEntity<Map<String, String>> editUser(
            @PathVariable String username,
            @RequestBody Map<String, String> body) {

        Map<String, String> res = new HashMap<>();
        String newRole = body.get("role");
        String newPassword = body.get("password");

        if (newRole != null && !newRole.isBlank()) {
            boolean ok = userManagerService.changeRole(username, newRole);
            if (!ok) {
                res.put("message", "Failed to update role.");
                return ResponseEntity.badRequest().body(res);
            }
        }

        if (newPassword != null && !newPassword.isBlank()) {
            boolean ok = userManagerService.changePassword(username, PasswordUtil.hash(newPassword));
            if (!ok) {
                res.put("message", "Failed to update password.");
                return ResponseEntity.badRequest().body(res);
            }
        }

        res.put("message", "User updated successfully");
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String username) {
        boolean ok = userManagerService.deleteUser(username);
        Map<String, String> res = new HashMap<>();
        if (ok) {
            res.put("message", "User deleted");
            return ResponseEntity.ok(res);
        } else {
            res.put("message", "User not found or delete failed.");
            return ResponseEntity.badRequest().body(res);
        }
    }
}
