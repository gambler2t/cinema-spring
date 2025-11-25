package com.example.cinema;

import com.example.cinema.domain.AppUser;
import com.example.cinema.repo.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootApplication
public class CinemaSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(CinemaSpringApplication.class, args);
    }

    @Bean
    public CommandLineRunner dataLoader(AppUserRepository userRepository,
                                        PasswordEncoder passwordEncoder) {
        return args -> {

            // ADMIN
            AppUser admin = userRepository.findByUsername("admin")
                    .orElseGet(AppUser::new);
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin")); // admin/admin
            admin.setFullName("Administrator");
            admin.getRoles().clear();
            admin.addRole("ADMIN");
            admin.addRole("USER");
            userRepository.save(admin);

            // USER
            AppUser user = userRepository.findByUsername("user")
                    .orElseGet(AppUser::new);
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user")); // user/user
            user.setFullName("Regular User");
            user.getRoles().clear();
            user.addRole("USER");
            userRepository.save(user);
        };
    }
}
