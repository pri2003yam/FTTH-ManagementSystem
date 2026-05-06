package ftth.api.controller;

import ftth.model.Role;
import ftth.model.User;
import ftth.repository.RoleRepository;
import ftth.repository.UserRepository;
import ftth.service.UserManagerService;

import ftth.util.PasswordUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UserManagerService userManagerService;

    public AuthApiController() {
        UserRepository userRepo = new UserRepository();
        RoleRepository roleRepo = new RoleRepository();
        this.userManagerService = new UserManagerService(userRepo, roleRepo);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // Fetch user directly and use smart BCrypt/plain text match
        ftth.model.User user = new ftth.repository.UserRepository().findByUsername(username);
        if (user == null || !user.isActive() || !PasswordUtil.matches(password, user.getPasswordHash())) {
            Map<String, String> err = new HashMap<>();
            err.put("message", "Invalid credentials");
            return ResponseEntity.status(401).body(err);
        }

        Role role = userManagerService.getRole(user);

        Map<String, String> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("role", role.getRoleCode());
        result.put("userId", String.valueOf(user.getUserId()));
        return ResponseEntity.ok(result);
    }
}
