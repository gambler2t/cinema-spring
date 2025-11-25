package com.example.cinema.controller;

import com.example.cinema.repo.TicketRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.example.cinema.domain.Ticket;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;


@Controller
@RequestMapping("/user")
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final TicketRepository ticketRepository;

    public UserController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("username", username);
        model.addAttribute("tickets", ticketRepository.findByUser_Username(username));
        return "user/profile";
    }

    @PostMapping("/tickets/{id}/delete")
    public String deleteTicket(@PathVariable Long id,
                               Principal principal) {

        // ищем билет
        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket != null && ticket.getUser() != null) {

            String currentUser = principal.getName();

            // удалять можно только свои билеты
            if (currentUser.equals(ticket.getUser().getUsername())) {
                ticketRepository.delete(ticket);
            }
        }

        // возвращаемся на страницу "My tickets"
        return "redirect:/user/profile";
    }

}
