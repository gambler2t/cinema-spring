package com.example.cinema.controller;

import com.example.cinema.domain.Screening;
import com.example.cinema.domain.Ticket;
import com.example.cinema.repo.ScreeningRepository;
import com.example.cinema.repo.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final ScreeningRepository screeningRepository;
    private final TicketRepository ticketRepository;

    public TicketController(ScreeningRepository screeningRepository,
                            TicketRepository ticketRepository) {
        this.screeningRepository = screeningRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Показать форму бронирования билета для конкретного сеанса.
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/book/{screeningId}")
    public String showBookingForm(@PathVariable Long screeningId, Model model) {
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + screeningId));

        Ticket ticket = new Ticket();
        ticket.setScreening(screening);

        model.addAttribute("ticket", ticket);
        return "tickets/book";
    }

    /**
     * Обработка формы бронирования.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/book")
    public String book(@Valid @ModelAttribute("ticket") Ticket ticket,
                       BindingResult bindingResult,
                       Model model) {

        // нужно заново подтянуть screening из БД по id
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

        if (bindingResult.hasErrors()) {
            // снова отдадим форму, чтобы показать ошибки
            return "tickets/book";
        }

        ticketRepository.save(ticket);
        // после успешного бронирования вернёмся на главную
        return "redirect:/";
    }
}
