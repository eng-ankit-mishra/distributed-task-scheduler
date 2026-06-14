package com.ankitmishra.task_scheduler.config;

import com.ankitmishra.task_scheduler.repository.UserRepository;
import org.springframework.context.annotation.Lazy; // Added Lazy import
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
// @AllArgsConstructor removed!
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Explicit constructor to lazy-load the database dependency
    public UserDetailsServiceImpl(@Lazy UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Name not found " + username));
    }
}