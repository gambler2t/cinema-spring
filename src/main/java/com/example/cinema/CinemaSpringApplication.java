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

@SpringBootApplication // Точка входа Spring Boot-приложения + авто-конфигурация
public class CinemaSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(CinemaSpringApplication.class, args); // Запуск приложения
    }

    @Bean
    public CommandLineRunner dataLoader(AppUserRepository userRepository,
                                        PasswordEncoder passwordEncoder) {
        // Код в этом CommandLineRunner выполнится один раз при старте приложения
        return args -> {

            // ADMIN
            AppUser admin = userRepository.findByUsername("admin")
                    .orElseGet(AppUser::new); // Если админа нет — создаём нового
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin")); // admin/admin
            admin.setFullName("Administrator");
            admin.getRoles().clear(); // Сбрасываем роли перед назначением
            admin.addRole("ADMIN");
            admin.addRole("USER");
            userRepository.save(admin); // Сохраняем / обновляем админа в БД

            // USER
            AppUser user = userRepository.findByUsername("user")
                    .orElseGet(AppUser::new); // Если обычного юзера нет — создаём нового
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user")); // user/user
            user.setFullName("Regular User");
            user.getRoles().clear();
            user.addRole("USER");
            userRepository.save(user); // Сохраняем / обновляем обычного пользователя
        };
    }
}