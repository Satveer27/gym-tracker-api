package com.satveer27.gym_tracker_api.entity;

import com.satveer27.gym_tracker_api.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name= "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name="password_hash", nullable = false)
    private String passwordHash;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name="role")
    private Role role;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
