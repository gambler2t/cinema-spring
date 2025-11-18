package com.example.cinema.controller;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Screening;
import com.example.cinema.domain.Ticket;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.ScreeningRepository;
import com.example.cinema.repo.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final ScreeningRepository screeningRepository;
    private final TicketRepository ticketRepository;
    private final AppUserRepository userRepository;

    public TicketController(ScreeningRepository screeningRepository,
                            TicketRepository ticketRepository,
                            AppUserRepository userRepository) {
        this.screeningRepository = screeningRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/book/{screeningId}")
    public String showBookingForm(@PathVariable Long screeningId,
                                  Model model,
                                  Authentication authentication) {
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + screeningId));

        Ticket ticket = new Ticket();
        ticket.setScreening(screening);

        if (authentication != null) {
            String username = authentication.getName();
            userRepository.findByUsername(username).ifPresent(user -> {
                if (user.getFullName() != null && !user.getFullName().isBlank()) {
                    ticket.setCustomerName(user.getFullName());
                } else {
                    ticket.setCustomerName(username);
                }
            });
        }

        // параметры зала (можно потом вынести в настройки)
        int rowsCount = 10;
        int seatsPerRow = 18;

        Set<String> occupiedSeats = ticketRepository.findByScreening_Id(screeningId)
                .stream()
                .map(Ticket::getSeat) // seat в формате "row-seat", напр. "5-7"
                .collect(Collectors.toSet());

        List<Integer> rows = IntStream.rangeClosed(1, rowsCount).boxed().toList();
        List<Integer> seats = IntStream.rangeClosed(1, seatsPerRow).boxed().toList();

        model.addAttribute("ticket", ticket);
        model.addAttribute("rows", rows);
        model.addAttribute("seats", seats);
        model.addAttribute("occupiedSeats", occupiedSeats);

        return "tickets/book";
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/book")
    public String book(@Valid @ModelAttribute("ticket") Ticket ticket,
                       BindingResult bindingResult,
                       Model model,
                       Authentication authentication) {

        if (authentication == null) {
            bindingResult.reject("user", "You must be logged in");
        }

        // подтягиваем сеанс
        if (ticket.getScreening() == null || ticket.getScreening().getId() == null) {
            bindingResult.reject("screening", "Screening is required");
        } else {
            Screening screening = screeningRepository.findById(ticket.getScreening().getId())
                    .orElse(null);
            if (screening == null) {
                bindingResult.reject("screening", "Screening not found");
            } else {
                ticket.setScreening(screening);
            }
        }

        // находим пользователя
        if (authentication != null) {
            String username = authentication.getName();
            AppUser user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                bindingResult.reject("user", "User not found");
            } else {
                ticket.setUser(user);
            }
        }

        // проверяем, что место не пустое и ещё свободно
        if (ticket.getSeat() == null || ticket.getSeat().isBlank()) {
            bindingResult.rejectValue("seat", "seat.empty", "Seat must be selected");
        } else if (!bindingResult.hasErrors()) {
            boolean alreadyTaken = ticketRepository.findByScreening_Id(
                            ticket.getScreening().getId()).stream()
                    .anyMatch(t -> t.getSeat().equals(ticket.getSeat()));
            if (alreadyTaken) {
                bindingResult.rejectValue("seat", "seat.taken", "Seat already taken");
            }
        }

        if (bindingResult.hasErrors()) {
            // если есть ошибки, нужно снова отдать схему зала
            int rowsCount = 10;
            int seatsPerRow = 18;
            List<Integer> rows = IntStream.rangeClosed(1, rowsCount).boxed().toList();
            List<Integer> seats = IntStream.rangeClosed(1, seatsPerRow).boxed().toList();
            Set<String> occupiedSeats = ticketRepository.findByScreening_Id(
                            ticket.getScreening().getId()).stream()
                    .map(Ticket::getSeat)
                    .collect(Collectors.toSet());

            model.addAttribute("rows", rows);
            model.addAttribute("seats", seats);
            model.addAttribute("occupiedSeats", occupiedSeats);

            return "tickets/book";
        }

        ticketRepository.save(ticket);
        return "redirect:/user/profile";
    }
}
