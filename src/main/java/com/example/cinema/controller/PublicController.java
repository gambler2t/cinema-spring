package com.example.cinema.controller;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Movie;
import com.example.cinema.domain.Screening;
import com.example.cinema.dto.RegistrationForm;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.ScreeningRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class PublicController {

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PublicController(MovieRepository movieRepository,
                            ScreeningRepository screeningRepository,
                            AppUserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Главная — сразу на список фильмов
    @GetMapping("/")
    public String home() {
        return "redirect:/movies";
    }

    // Страница логина
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // ---------- РЕГИСТРАЦИЯ ----------

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult bindingResult,
            Model model
    ) {
        // Базовая валидация
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // Совпадение паролей
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Passwords do not match"
            );
            return "register";
        }

        // Уникальность логина
        if (userRepository.findByUsername(form.getUsername()).isPresent()) {
            bindingResult.rejectValue(
                    "username",
                    "username.exists",
                    "Username already taken"
            );
            return "register";
        }

        // Создаём пользователя
        AppUser user = new AppUser();
        user.setUsername(form.getUsername());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.addRole("USER"); // новая учётка = обычный пользователь

        userRepository.save(user);

        // После регистрации отправляем на логин
        return "redirect:/login?registered";
    }

    // ---------- ПУБЛИЧНЫЕ СТРАНИЦЫ ----------

    @GetMapping("/movies")
    public String moviesList(@RequestParam(value = "q", required = false) String query,
                             Model model) {
        List<Movie> movies;
        if (query != null && !query.isBlank()) {
            movies = movieRepository.findByTitleContainingIgnoreCase(query);
        } else {
            movies = movieRepository.findAll();
        }
        model.addAttribute("movies", movies);
        model.addAttribute("query", query);
        return "movies/list";
    }

    @GetMapping("/screenings")
    public String screeningsList(Model model) {
        List<Screening> screenings = screeningRepository.findAll();
        model.addAttribute("screenings", screenings);
        return "screenings/list";
    }

    @GetMapping("/movies/{id}")
    public String movieDetails(@PathVariable Long id,
                               @RequestParam(value = "date", required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + id));

        // все сеансы этого фильма
        List<Screening> allScreenings =
                screeningRepository.findByMovie_IdOrderByStartTimeAsc(id);

        // только будущие сеансы
        LocalDateTime now = LocalDateTime.now();
        List<Screening> upcoming = allScreenings.stream()
                .filter(s -> !s.getStartTime().isBefore(now))
                .toList();

        // доступные даты
        List<LocalDate> dates = upcoming.stream()
                .map(s -> s.getStartTime().toLocalDate())
                .distinct()
                .sorted()
                .toList();

        // выбранная дата
        LocalDate selectedDate;
        if (date != null) {
            selectedDate = date;
        } else if (!dates.isEmpty()) {
            selectedDate = dates.get(0);
        } else {
            selectedDate = null;
        }

        // сеансы только выбранного дня
        List<Screening> screeningsForDay;
        if (selectedDate != null) {
            screeningsForDay = upcoming.stream()
                    .filter(s -> s.getStartTime().toLocalDate().equals(selectedDate))
                    .toList();
        } else {
            screeningsForDay = List.of();
        }

        model.addAttribute("movie", movie);
        model.addAttribute("availableDates", dates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("screeningsForDay", screeningsForDay);

        return "movies/details";
    }

    @GetMapping("/about")
    public String about() {
        return "about";   // шаблон about.html
    }
}
