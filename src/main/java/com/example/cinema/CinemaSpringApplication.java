package com.example.cinema;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Movie;
import com.example.cinema.domain.Screening;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.ScreeningRepository;
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
    public CommandLineRunner dataLoader(MovieRepository movieRepository,
                                        ScreeningRepository screeningRepository,
                                        AppUserRepository userRepository,
                                        PasswordEncoder passwordEncoder) {
        return args -> {

            // 1. Инициализируем фильмы и сеансы
            if (movieRepository.count() == 0) {
                Movie matrix = new Movie(
                        "The Matrix",
                        "Sci-fi classic about the nature of reality.",
                        130
                );
                matrix.setGenre("Sci-fi");
                matrix.setDirector("Lana Wachowski, Lilly Wachowski");
                matrix.setCountry("USA");
                // простой плейсхолдер-картинка с надписью Matrix
                matrix.setPosterUrl("/images/matrix.jpg");;

                Movie inception = new Movie(
                        "Inception",
                        "Mind-bending thriller about dreams within dreams.",
                        148
                );
                inception.setGenre("Sci-fi, Thriller");
                inception.setDirector("Christopher Nolan");
                inception.setCountry("USA");
                inception.setPosterUrl("/images/inception.jpg");

                movieRepository.save(matrix);
                movieRepository.save(inception);

                // делаем 2 сеанса на завтра: 19:00 и 22:00
                LocalDateTime tomorrow = LocalDateTime.now().plusDays(1)
                        .withSecond(0).withNano(0);

                Screening screening1 = new Screening(
                        matrix,
                        tomorrow.withHour(19).withMinute(0),
                        "Hall 1",
                        new BigDecimal("9.99")
                );

                Screening screening2 = new Screening(
                        inception,
                        tomorrow.withHour(22).withMinute(0),
                        "Hall 2",
                        new BigDecimal("11.50")
                );

                screeningRepository.save(screening1);
                screeningRepository.save(screening2);
            }

            // 2. Инициализируем пользователей
            if (userRepository.count() == 0) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin")); // admin / admin
                admin.setFullName("Administrator");
                admin.addRole("ADMIN");
                admin.addRole("USER");

                AppUser user = new AppUser();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user"));   // user / user
                user.setFullName("Regular User");
                user.addRole("USER");

                userRepository.save(admin);
                userRepository.save(user);
            }
        };
    }
}
