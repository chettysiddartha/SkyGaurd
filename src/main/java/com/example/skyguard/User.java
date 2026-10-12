package com.example.skyguard;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    public String username;

    @NotBlank
    @Column(nullable = false)
    public String password;

    public String fullName;

    public String email;

    public String role = "ROLE_STAFF"; // ROLE_ADMIN, ROLE_STAFF, ROLE_VIEWER

    public boolean enabled = true;

    public LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String username, String password, String fullName, String email, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
    }
}
