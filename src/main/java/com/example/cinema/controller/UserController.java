package com.example.cinema.controller;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Movie;
import com.example.cinema.domain.Ticket;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.TicketRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
@PreAuthorize("hasRole('USER')") // Все методы контроллера доступны только авторизованным пользователям с ролью USER
public class UserController {

    private final TicketRepository ticketRepository; // Репозиторий для работы с билетами пользователя
    private final AppUserRepository userRepository;  // Репозиторий пользователей
    private final MovieRepository movieRepository;   // Репозиторий фильмов (для избранного)

    public UserController(TicketRepository ticketRepository,
                          AppUserRepository userRepository,
                          MovieRepository movieRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        String username = authentication.getName(); // Текущий логин пользователя

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username)); // Проверяем, что пользователь существует

        List<Ticket> allTickets = ticketRepository.findByUser_Username(username); // Все билеты пользователя
        LocalDateTime now = LocalDateTime.now();

        // Будущие билеты (сеанс ещё не начался)
        List<Ticket> upcomingTickets = allTickets.stream()
                .filter(t -> t.getScreening().getStartTime().isAfter(now))
                .sorted(Comparator.comparing(t -> t.getScreening().getStartTime())) // Сортируем по времени сеанса
                .collect(Collectors.toList());

        // Прошедшие билеты (сеанс уже был)
        List<Ticket> pastTickets = allTickets.stream()
                .filter(t -> t.getScreening().getStartTime().isBefore(now))
                .sorted(Comparator.comparing(t -> t.getScreening().getStartTime()))
                .collect(Collectors.toList());

        // Последние 4 избранных фильма (по id, с конца)
        List<Movie> recentFavorites = user.getFavoriteMovies().stream()
                .sorted(Comparator.comparing(Movie::getId).reversed())
                .limit(4)
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("upcomingTickets", upcomingTickets);
        model.addAttribute("pastTickets", pastTickets);
        model.addAttribute("recentFavorites", recentFavorites);

        return "user/profile"; // Шаблон страницы профиля
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute AppUser form,
                                Authentication authentication) {

        String username = authentication.getName();
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Обновляем основные поля профиля (без смены логина/ролей/пароля)
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setBio(form.getBio());

        userRepository.save(user); // Сохраняем изменения

        return "redirect:/user/profile?updated"; // Редирект с флагом успешного обновления
    }

    @PostMapping("/tickets/{id}/delete")
    public String deleteTicket(@PathVariable Long id, Principal principal) {

        if (principal == null) {
            return "redirect:/login"; // Без авторизации удалять нельзя
        }

        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket != null && ticket.getUser() != null) {
            String currentUser = principal.getName();

            // Проверяем, что билет принадлежит текущему пользователю
            if (currentUser.equals(ticket.getUser().getUsername())) {
                LocalDateTime now = LocalDateTime.now();
                // Удалять можно только билет на будущий сеанс
                if (ticket.getScreening().getStartTime().isAfter(now)) {
                    ticketRepository.delete(ticket);
                }
            }
        }

        return "redirect:/user/profile"; // После попытки удаления возвращаемся в профиль
    }

    @GetMapping("/favorites")
    public String favorites(Model model, Principal principal) {
        AppUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("favorites", user.getFavoriteMovies()); // Список избранных фильмов пользователя
        return "user/favorites"; // Страница «Избранное»
    }

    @PostMapping("/favorites/{movieId}/toggle")
    public String toggleFavorite(@PathVariable Long movieId,
                                 Principal principal,
                                 HttpServletRequest request) {

        AppUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        // Если фильм уже в избранном — убираем, иначе добавляем
        if (user.getFavoriteMovies().contains(movie)) {
            user.getFavoriteMovies().remove(movie);
        } else {
            user.getFavoriteMovies().add(movie);
        }

        userRepository.save(user); // Сохраняем изменения избранного

        // Возвращаем пользователя обратно на страницу, откуда он пришёл
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/movies");
    }
}