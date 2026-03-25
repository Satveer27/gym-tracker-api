package com.satveer27.gym_tracker_api.repository;

import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
