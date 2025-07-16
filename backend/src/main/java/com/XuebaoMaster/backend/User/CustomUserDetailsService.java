package com.XuebaoMaster.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()));

        // Return a custom UserDetails that contains our User entity
        return new CustomUserPrincipal(user, authorities);
    }

    // Custom UserDetails implementation that wraps our User entity
    public static class CustomUserPrincipal extends org.springframework.security.core.userdetails.User {
        private final User user;

        public CustomUserPrincipal(User user, List<SimpleGrantedAuthority> authorities) {
            super(user.getUsername(), user.getPassword(), authorities);
            this.user = user;
        }

        public User getUser() {
            return user;
        }
    }
}
