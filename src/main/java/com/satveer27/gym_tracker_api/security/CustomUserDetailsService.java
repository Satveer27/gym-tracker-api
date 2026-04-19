package com.satveer27.gym_tracker_api.security;

import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> result = userRepository.findByUsernameIgnoreCase(username);
        if (result.isPresent()) {
            return result.get();
        }
        throw new UsernameNotFoundException(username);
    }
}
