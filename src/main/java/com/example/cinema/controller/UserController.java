package com.example.cinema.controller;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Movie;
import com.example.cinema.domain.Ticket;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.TicketRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/user")
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final TicketRepository ticketRepository;
    private final AppUserRepository userRepository;
    private final MovieRepository movieRepository;

    public UserController(TicketRepository ticketRepository,
                          AppUserRepository userRepository,
                          MovieRepository movieRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    // -------- Профиль, ближайшие сеансы, история, последние избранные --------

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        String username = authentication.getName();

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // все билеты пользователя
        List<Ticket> allTickets = ticketRepository.findByUser_Username(username);
        LocalDateTime now = LocalDateTime.now();

        // ближайшие сеансы
        List<Ticket> upcomingTickets = allTickets.stream()
                .filter(t -> t.getScreening().getStartTime().isAfter(now))
                .toList();

        // прошедшие (история посещений)
        List<Ticket> pastTickets = allTickets.stream()
                .filter(t -> t.getScreening().getStartTime().isBefore(now))
                .toList();

        // последние избранные фильмы (до 4 штук, по id "сверху")
        List<Movie> recentFavorites = (user.getFavoriteMovies() == null)
                ? List.of()
                : user.getFavoriteMovies().stream()
                .sorted(Comparator.comparing(Movie::getId).reversed())
                .limit(4)
                .toList();

        model.addAttribute("username", user.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("upcomingTickets", upcomingTickets);
        model.addAttribute("pastTickets", pastTickets);
        model.addAttribute("recentFavorites", recentFavorites);

        return "user/profile";
    }

    // -------- Обновление профиля (ФИО, email, bio) --------

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("user") AppUser form,
                                Authentication authentication) {

        String username = authentication.getName();
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setBio(form.getBio());

        userRepository.save(user);

        return "redirect:/user/profile?updated";
    }

    // -------- Удаление билета --------

    @PostMapping("/tickets/{id}/delete")
    public String deleteTicket(@PathVariable Long id,
                               Principal principal) {

        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket != null && ticket.getUser() != null) {
            String currentUser = principal.getName();

            // удалять можно только свои билеты
            if (currentUser.equals(ticket.getUser().getUsername())) {
                ticketRepository.delete(ticket);
            }
        }

        return "redirect:/user/profile";
    }

    // -------- Избранные фильмы --------

    @PostMapping("/favorites/{movieId}/toggle")
    public String toggleFavorite(@PathVariable Long movieId,
                                 Principal principal,
                                 @RequestHeader(value = "Referer", required = false) String referer) {

        // текущий пользователь
        AppUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // фильм
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        // если фильм уже в избранном — убираем, иначе добавляем
        if (user.getFavoriteMovies().contains(movie)) {
            user.removeFavoriteMovie(movie);
        } else {
            user.addFavoriteMovie(movie);
        }

        userRepository.save(user);

        // возвращаемся туда, откуда пришли (детали фильма / список)
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/movies";
    }

    @GetMapping("/favorites")
    public String favorites(Model model, Principal principal) {
        AppUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("favorites", user.getFavoriteMovies());
        return "user/favorites";
    }
}
