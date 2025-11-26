package com.example.cinema.controller;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Screening;
import com.example.cinema.domain.Ticket;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.ScreeningRepository;
import com.example.cinema.repo.TicketRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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

    // Отображение формы бронирования билетов
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/book/{screeningId}")
    public String showBookingForm(@PathVariable Long screeningId,
                                  Model model,
                                  Authentication authentication) {

        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + screeningId));

        // Создаем пустой билет для формы
        Ticket ticket = new Ticket();
        ticket.setScreening(screening);

        // Заполняем имя пользователя
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

        // Параметры зала
        int rowsCount = 10;
        int seatsPerRow = 18;

        // Получаем занятые места
        Set<String> occupiedSeats = ticketRepository.findByScreening_Id(screeningId)
                .stream()
                .map(Ticket::getSeat)
                .collect(Collectors.toSet());

        List<Integer> rows = IntStream.rangeClosed(1, rowsCount).boxed().collect(Collectors.toList());
        List<Integer> seats = IntStream.rangeClosed(1, seatsPerRow).boxed().collect(Collectors.toList());

        model.addAttribute("ticket", ticket);
        model.addAttribute("rows", rows);
        model.addAttribute("seats", seats);
        model.addAttribute("occupiedSeats", occupiedSeats);
        model.addAttribute("selectedSeats", new ArrayList<String>());

        return "tickets/book";
    }

    // Обработка бронирования нескольких билетов
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/book")
    public String book(@ModelAttribute("ticket") Ticket ticketTemplate,
                       @RequestParam("selectedSeats") List<String> selectedSeats,
                       Authentication authentication) {

        if (authentication == null) {
            return "redirect:/login";
        }

        // Находим пользователя
        String username = authentication.getName();
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Находим сеанс
        Screening screening = screeningRepository.findById(ticketTemplate.getScreening().getId())
                .orElseThrow(() -> new IllegalArgumentException("Screening not found"));

        // Создаем билеты для каждого выбранного места
        for (String seat : selectedSeats) {
            // Проверяем, что место свободно
            if (!ticketRepository.existsByScreening_IdAndSeat(screening.getId(), seat)) {
                Ticket ticket = new Ticket();
                ticket.setScreening(screening);
                ticket.setUser(user);
                ticket.setCustomerName(ticketTemplate.getCustomerName());
                ticket.setSeat(seat);
                ticketRepository.save(ticket);
            }
        }

        return "redirect:/user/profile?booked=" + selectedSeats.size();
    }
}