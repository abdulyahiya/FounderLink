package com.capgemini.authservice.config;

import com.capgemini.authservice.entity.RoleEntity;
import com.capgemini.authservice.entity.UserEntity;
import com.capgemini.authservice.repository.RoleRepository;
import com.capgemini.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // Seed roles
        List<String> roles = List.of("ROLE_FOUNDER", "ROLE_INVESTOR", "ROLE_COFOUNDER", "ROLE_ADMIN");
        for (String roleName : roles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(RoleEntity.builder().name(roleName).build());
            }
        }

        // Seed default admin user if not exists
        if (userRepository.findByEmail("admin@founderlink.com").isEmpty()) {
            RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
            UserEntity admin = UserEntity.builder()
                    .name("Admin")
                    .email("admin@founderlink.com")
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(admin);
        }
    }
}
