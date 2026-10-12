package com.example.skyguard;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username.trim(), password)
            );

            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(auth);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            User u = userRepository.findByUsername(username.trim()).orElseThrow();

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("username", u.username);
            resp.put("fullName", u.fullName);
            resp.put("email", u.email);
            resp.put("role", u.role);
            resp.put("message", "Welcome back, " + (u.fullName != null ? u.fullName : u.username) + "!");

            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String fullName = body.get("fullName");
        String email = body.get("email");
        String roleRequested = body.get("role");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        if (userRepository.existsByUsername(username.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + username + "' is already taken");
        }

        String role = "ROLE_STAFF";
        if ("ROLE_ADMIN".equalsIgnoreCase(roleRequested) || "ADMIN".equalsIgnoreCase(roleRequested)) {
            // First user or explicit admin request
            if (userRepository.count() == 0) {
                role = "ROLE_ADMIN";
            }
        } else if ("ROLE_VIEWER".equalsIgnoreCase(roleRequested) || "VIEWER".equalsIgnoreCase(roleRequested)) {
            role = "ROLE_VIEWER";
        }

        User user = new User(
                username.trim(),
                passwordEncoder.encode(password),
                fullName != null && !fullName.isBlank() ? fullName.trim() : username.trim(),
                email != null ? email.trim() : "",
                role
        );

        userRepository.save(user);

        return Map.of(
                "success", true,
                "username", user.username,
                "role", user.role,
                "message", "User registered successfully! You can now log in."
        );
    }

    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }

        String username = authentication.getName();
        User u = userRepository.findByUsername(username).orElse(null);

        if (u == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("authenticated", true);
        resp.put("username", u.username);
        resp.put("fullName", u.fullName);
        resp.put("email", u.email);
        resp.put("role", u.role);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.id);
            map.put("username", u.username);
            map.put("fullName", u.fullName);
            map.put("email", u.email);
            map.put("role", u.role);
            map.put("enabled", u.enabled);
            map.put("createdAt", u.createdAt);
            list.add(map);
        }
        return list;
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id, Authentication authentication) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.username.equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
        }
        userRepository.deleteById(id);
    }
}
