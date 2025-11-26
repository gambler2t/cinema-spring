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

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    // ---------- Главная ----------

    @GetMapping("/")
    public String home() {
        return "redirect:/movies";
    }

    // ---------- Логин (только GET, без редиректов) ----------

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // ---------- Регистрация ----------

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult bindingResult,
            Model model
    ) {
        // 1) Валидация совпадения паролей
        if (!bindingResult.hasFieldErrors("password")
                && !bindingResult.hasFieldErrors("confirmPassword")) {
            if (!form.getPassword().equals(form.getConfirmPassword())) {
                bindingResult.rejectValue(
                        "confirmPassword",
                        "password.mismatch",
                        "Пароли не совпадают"
                );
            }
        }

        // 2) Уникальность логина
        if (!bindingResult.hasFieldErrors("username")
                && userRepository.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue(
                    "username",
                    "username.taken",
                    "Пользователь с таким логином уже существует"
            );
        }

        // Если есть ошибки — остаёмся на странице регистрации
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // 3) Создаём пользователя
        AppUser user = new AppUser();
        user.setUsername(form.getUsername());
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.addRole("USER");

        userRepository.save(user);

        // 4) После успешной регистрации — на логин
        return "redirect:/login?registered";
    }

    // ---------- Публичные страницы ----------

    @GetMapping("/movies")
    public String moviesList(@RequestParam(value = "q", required = false) String query,
                             Model model,
                             Principal principal) {

        // Поиск фильмов
        List<Movie> movies;
        if (query != null && !query.isBlank()) {
            movies = movieRepository.findByTitleContainingIgnoreCase(query);
        } else {
            movies = movieRepository.findAll();
        }

        model.addAttribute("movies", movies);
        model.addAttribute("query", query);

        // Избранные фильмы текущего пользователя
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                if (user.getFavoriteMovies() != null) {
                    Set<Long> favoriteIds = user.getFavoriteMovies().stream()
                            .map(Movie::getId)
                            .collect(Collectors.toSet());
                    model.addAttribute("favoriteIds", favoriteIds);
                }
            });
        }

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
                               Model model,
                               Principal principal) {

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

        // Флаг "в избранном" для текущего пользователя
        boolean isFavorite = false;
        if (principal != null) {
            AppUser user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null && user.getFavoriteMovies() != null
                    && user.getFavoriteMovies().contains(movie)) {
                isFavorite = true;
            }
        }

        model.addAttribute("movie", movie);
        model.addAttribute("availableDates", dates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("screeningsForDay", screeningsForDay);
        model.addAttribute("isFavorite", isFavorite);

        return "movies/details";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
