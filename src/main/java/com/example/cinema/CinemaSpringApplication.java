package com.example.cinema;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Movie;
import com.example.cinema.domain.Screening;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.ScreeningRepository;
import com.example.cinema.repo.TicketRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootApplication
public class CinemaSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(CinemaSpringApplication.class, args);
    }

    /**
     * Инициализация тестовых данных при старте приложения.
     * Заполняем фильмы, сеансы и пользователей (admin / user).
     */
    @Bean
    public CommandLineRunner dataLoader(MovieRepository movieRepository,
                                        ScreeningRepository screeningRepository,
                                        TicketRepository ticketRepository,
                                        AppUserRepository userRepository) {
        return args -> {

            // --- Фильмы и сеансы ---
            if (movieRepository.count() == 0) {

                Movie matrix = new Movie(
                        "The Matrix",
                        "Sci-fi classic about the nature of reality.",
                        130
                );

                Movie inception = new Movie(
                        "Inception",
                        "Mind-bending thriller about dreams.",
                        148
                );

                movieRepository.save(matrix);
                movieRepository.save(inception);

                Screening screening1 = new Screening(
                        matrix,
                        LocalDateTime.now().plusDays(1),
                        "Hall 1",
                        new BigDecimal("9.99")
                );

                Screening screening2 = new Screening(
                        inception,
                        LocalDateTime.now().plusDays(1).plusHours(3),
                        "Hall 2",
                        new BigDecimal("11.50")
                );

                screeningRepository.save(screening1);
                screeningRepository.save(screening2);
            }

            // --- Пользователи ---
            if (userRepository.count() == 0) {
                PasswordEncoder encoder = new BCryptPasswordEncoder();

                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin")); // пароль: admin
                admin.setFullName("Administrator");
                admin.addRole("ADMIN");
                admin.addRole("USER");

                AppUser user = new AppUser();
                user.setUsername("user");
                user.setPassword(encoder.encode("user"));   // пароль: user
                user.setFullName("Regular User");
                user.addRole("USER");

                userRepository.save(admin);
                userRepository.save(user);
            }
        };
    }
}
