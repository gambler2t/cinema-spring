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

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        String username = authentication.getName();

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        List<Ticket> allTickets = ticketRepository.findByUser_Username(username);
        LocalDateTime now = LocalDateTime.now();

        List<Ticket> upcomingTickets = allTickets.stream()
                .filter(t -> t.getScreening().getStartTime().isAfter(now))
                .sorted(Comparator.comparing(t -> t.getScreening().getStartTime()))
                .collect(Collectors.toList());

        List<Ticket> pastTickets = allTickets.stream()
                .filter(t -> t.getScreening().getStartTime().isBefore(now))
                .sorted(Comparator.comparing(t -> t.getScreening().getStartTime()))
                .collect(Collectors.toList());

        List<Movie> recentFavorites = user.getFavoriteMovies().stream()
                .sorted(Comparator.comparing(Movie::getId).reversed())
                .limit(4)
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("upcomingTickets", upcomingTickets);
        model.addAttribute("pastTickets", pastTickets);
        model.addAttribute("recentFavorites", recentFavorites);

        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute AppUser form,
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

    @PostMapping("/tickets/{id}/delete")
    public String deleteTicket(@PathVariable Long id, Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket != null && ticket.getUser() != null) {
            String currentUser = principal.getName();

            if (currentUser.equals(ticket.getUser().getUsername())) {
                LocalDateTime now = LocalDateTime.now();
                if (ticket.getScreening().getStartTime().isAfter(now)) {
                    ticketRepository.delete(ticket);
                }
            }
        }

        return "redirect:/user/profile";
    }

    @GetMapping("/favorites")
    public String favorites(Model model, Principal principal) {
        AppUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("favorites", user.getFavoriteMovies());
        return "user/favorites";
    }

    @PostMapping("/favorites/{movieId}/toggle")
    public String toggleFavorite(@PathVariable Long movieId,
                                 Principal principal,
                                 HttpServletRequest request) {

        AppUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        if (user.getFavoriteMovies().contains(movie)) {
            user.getFavoriteMovies().remove(movie);
        } else {
            user.getFavoriteMovies().add(movie);
        }

        userRepository.save(user);

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/movies");
    }
}
