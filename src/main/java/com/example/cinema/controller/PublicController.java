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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller // Публичный контроллер для страниц, доступных без админки
public class PublicController {

    private final MovieRepository movieRepository; // Доступ к фильмам
    private final ScreeningRepository screeningRepository; // Доступ к сеансам
    private final AppUserRepository userRepository; // Доступ к пользователям
    private final PasswordEncoder passwordEncoder; // Кодировщик паролей

    public PublicController(MovieRepository movieRepository,
                            ScreeningRepository screeningRepository,
                            AppUserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Главная

    @GetMapping("/")
    public String home() {
        return "redirect:/movies"; // Главная сразу перекидывает на список фильмов
    }

    // ---------- Логин (только GET, без редиректов) ----------

    @GetMapping("/login")
    public String login() {
        return "login"; // Возвращаем шаблон страницы входа
    }

    // Регистрация

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registrationForm")) { // Если форма ещё не добавлена (после редиректа с ошибками)
            model.addAttribute("registrationForm", new RegistrationForm()); // Кладём пустую форму регистрации
        }
        return "register"; // Шаблон страницы регистрации
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form, // Биндим и валидируем форму
            BindingResult bindingResult, // Результат валидации
            Model model
    ) {
        // 1) Валидация совпадения паролей
        if (!bindingResult.hasFieldErrors("password")
                && !bindingResult.hasFieldErrors("confirmPassword")) {
            if (!form.getPassword().equals(form.getConfirmPassword())) {
                bindingResult.rejectValue(
                        "confirmPassword",
                        "password.mismatch",
                        "Пароли не совпадают" // Сообщение об ошибке если пароли разные
                );
            }
        }

        // 2) Уникальность логина
        if (!bindingResult.hasFieldErrors("username")
                && userRepository.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue(
                    "username",
                    "username.taken",
                    "Пользователь с таким логином уже существует" // Ошибка, если логин уже занят
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
        user.setPassword(passwordEncoder.encode(form.getPassword())); // Сохраняем пароль в зашифрованном виде
        user.addRole("USER"); // Выдаём обычную роль пользователя

        userRepository.save(user); // Сохраняем пользователя в БД

        // 4) После успешной регистрации — на логин
        return "redirect:/login?registered"; // Флаг в URL можно использовать для показа уведомления
    }

    // Публичные страницы

    @GetMapping("/movies")
    public String moviesList(@RequestParam(value = "q", required = false) String query,
                             Model model,
                             Principal principal) {

        // Поиск фильмов
        List<Movie> movies;
        if (query != null && !query.isBlank()) {
            movies = movieRepository.findByTitleContainingIgnoreCase(query); // Поиск по подстроке в названии
        } else {
            movies = movieRepository.findAll(); // Если строки поиска нет — показываем все фильмы
        }

        model.addAttribute("movies", movies);
        model.addAttribute("query", query); // Подставляем обратно введённую строку поиска

        // Избранные фильмы текущего пользователя
        if (principal != null) { // Если пользователь авторизован
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                if (user.getFavoriteMovies() != null) {
                    Set<Long> favoriteIds = user.getFavoriteMovies().stream()
                            .map(Movie::getId)
                            .collect(Collectors.toSet()); // Собираем ID избранных фильмов в множество
                    model.addAttribute("favoriteIds", favoriteIds); // Используется в шаблоне для подсветки «избранного»
                }
            });
        }

        return "movies/list"; // Шаблон списка фильмов
    }

    @GetMapping("/screenings")
    public String screeningsList(Model model) {
        List<Screening> screenings = screeningRepository.findAll(); // Получаем все сеансы
        model.addAttribute("screenings", screenings);
        return "screenings/list"; // Шаблон списка всех сеансов
    }

    @GetMapping("/movies/{id}")
    public String movieDetails(@PathVariable Long id,
                               @RequestParam(value = "date", required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, // Опциональная дата для фильтрации сеансов
                               Model model,
                               Principal principal) {

        // Получаем фильм по id
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + id)); // Ошибка, если фильм не найден

        // Получаем все сеансы для фильма
        List<Screening> allScreenings = screeningRepository.findByMovie_IdOrderByStartTimeAsc(id);

        // Фильтруем только будущие сеансы
        LocalDateTime now = LocalDateTime.now();
        List<Screening> upcoming = allScreenings.stream()
                .filter(s -> s.getStartTime().isAfter(now)) // Оставляем только сеансы, которые ещё не начались
                .collect(Collectors.toList());

        // Список доступных дат для сеансов
        List<LocalDate> dates = upcoming.stream()
                .map(s -> s.getStartTime().toLocalDate())
                .distinct()
                .sorted()
                .collect(Collectors.toList()); // Уникальные даты в порядке возрастания

        // Выбираем дату
        LocalDate selectedDate;
        if (date != null) {
            selectedDate = date; // Если дата передана в параметрах — берём её
        } else {
            selectedDate = !dates.isEmpty() ? dates.get(0) : null; // Иначе первая доступная дата (или null, если сеансов нет)
        }

        // Сеансы на выбранную дату + группировка по залам
        List<Screening> screeningsForDay = Collections.emptyList();
        Map<String, List<Screening>> screeningsByHall = new LinkedHashMap<>();

        if (selectedDate != null) {
            screeningsForDay = upcoming.stream()
                    .filter(s -> s.getStartTime().toLocalDate().equals(selectedDate)) // Сеансы только на выбранный день
                    .collect(Collectors.toList());

            screeningsByHall = screeningsForDay.stream()
                    .collect(Collectors.groupingBy(
                            Screening::getHall, // Ключ — название зала
                            LinkedHashMap::new, // Сохраняем порядок добавления залов
                            Collectors.toList()
                    ));
        }

        // Проверка избранного для текущего пользователя
        boolean isFavorite = false;
        if (principal != null) {
            AppUser user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null && user.getFavoriteMovies() != null) {
                isFavorite = user.getFavoriteMovies().contains(movie); // Проверяем, добавлен ли фильм в избранное
            }
        }

        // Передаем данные в модель
        model.addAttribute("movie", movie);
        model.addAttribute("availableDates", dates); // Доступные даты с сеансами
        model.addAttribute("selectedDate", selectedDate); // Выбранная дата
        model.addAttribute("screeningsForDay", screeningsForDay); // Сеансы в выбранный день
        model.addAttribute("screeningsByHall", screeningsByHall); // Сеансы, сгруппированные по залам
        model.addAttribute("isFavorite", isFavorite); // Флаг, избран ли фильм

        return "movies/details"; // Шаблон страницы с подробностями фильма и расписанием
    }

    @GetMapping("/about")
    public String about() {
        return "about"; // Статическая страница «О кинотеатре»
    }
}