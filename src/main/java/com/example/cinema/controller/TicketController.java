package com.example.cinema.controller;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Screening;
import com.example.cinema.domain.Ticket;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.ScreeningRepository;
import com.example.cinema.repo.TicketRepository;
import com.example.cinema.service.EmailService;
import com.example.cinema.service.QrCodeService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final ScreeningRepository screeningRepository;
    private final TicketRepository ticketRepository;
    private final AppUserRepository userRepository;
    private final QrCodeService qrCodeService;
    private final EmailService emailService;

    public TicketController(ScreeningRepository screeningRepository,
                            TicketRepository ticketRepository,
                            AppUserRepository userRepository,
                            QrCodeService qrCodeService,
                            EmailService emailService) {
        this.screeningRepository = screeningRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.emailService = emailService;
    }

    // ================= Шаг 1. Выбор мест =================

    // Больше НЕ ограничиваем только USER — гость тоже может бронировать
    @GetMapping("/book/{screeningId}")
    public String showBookingForm(@PathVariable Long screeningId,
                                  Model model,
                                  Authentication authentication) {

        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + screeningId));

        // Пустой билет для формы
        Ticket ticket = new Ticket();
        ticket.setScreening(screening);

        // Заполняем имя пользователя, если залогинен
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

        int rowsCount = 10;
        int seatsPerRow = 18;

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

    // Теперь этот метод просто показывает страницу оплаты
    @PostMapping("/book")
    public String showPaymentPage(@ModelAttribute("ticket") Ticket ticketTemplate,
                                  @RequestParam("selectedSeats") String selectedSeatsRaw,
                                  Authentication authentication,
                                  Model model) {

        if (selectedSeatsRaw == null || selectedSeatsRaw.isBlank()) {
            // если нет мест — обратно на выбор
            return "redirect:/tickets/book/" + ticketTemplate.getScreening().getId();
        }

        Screening screening = screeningRepository.findById(ticketTemplate.getScreening().getId())
                .orElseThrow(() -> new IllegalArgumentException("Screening not found"));

        List<String> selectedSeats = Arrays.stream(selectedSeatsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        int count = selectedSeats.size();
        BigDecimal pricePer = screening.getPrice() != null ? screening.getPrice() : BigDecimal.ZERO;
        BigDecimal total = pricePer.multiply(BigDecimal.valueOf(count));

        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        String prefilledEmail = null;

        if (isAuthenticated) {
            AppUser user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                prefilledEmail = user.getEmail();
            }
        }

        model.addAttribute("screening", screening);
        model.addAttribute("customerName", ticketTemplate.getCustomerName());
        model.addAttribute("selectedSeats", selectedSeats);
        model.addAttribute("selectedSeatsRaw", String.join(",", selectedSeats));
        model.addAttribute("ticketsCount", count);
        model.addAttribute("totalPrice", total);
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("prefilledEmail", prefilledEmail);

        return "tickets/payment";
    }

    // ================= Шаг 2. Оплата и создание билетов =================

    @PostMapping("/pay")
    public String processPayment(@RequestParam("screeningId") Long screeningId,
                                 @RequestParam("customerName") String customerName,
                                 @RequestParam("selectedSeats") String selectedSeatsRaw,
                                 @RequestParam(value = "email", required = false) String email,
                                 Authentication authentication,
                                 Model model) {

        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found"));

        List<String> selectedSeats = Arrays.stream(selectedSeatsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        int count = selectedSeats.size();
        BigDecimal pricePer = screening.getPrice() != null ? screening.getPrice() : BigDecimal.ZERO;
        BigDecimal total = pricePer.multiply(BigDecimal.valueOf(count));

        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        AppUser user = null;

        if (isAuthenticated) {
            user = userRepository.findByUsername(authentication.getName()).orElse(null);
            // если email не указан в форме — попробуем взять из профиля
            if ((email == null || email.isBlank()) && user != null) {
                email = user.getEmail();
            }
        }

        // Для гостя email ОБЯЗАТЕЛЕН
        if (!isAuthenticated && (email == null || email.isBlank())) {
            model.addAttribute("emailError", "Для покупки без регистрации укажите электронную почту.");

            model.addAttribute("screening", screening);
            model.addAttribute("customerName", customerName);
            model.addAttribute("selectedSeats", selectedSeats);
            model.addAttribute("selectedSeatsRaw", String.join(",", selectedSeats));
            model.addAttribute("ticketsCount", count);
            model.addAttribute("totalPrice", total);
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("prefilledEmail", null);

            return "tickets/payment";
        }

        // Здесь должна быть интеграция с платёжкой. Сейчас просто считаем, что оплата прошла успешно.

        List<Ticket> createdTickets = new ArrayList<>();

        for (String seat : selectedSeats) {
            // проверим, что место ещё свободно
            if (!ticketRepository.existsByScreening_IdAndSeat(screeningId, seat)) {
                Ticket ticket = new Ticket();
                ticket.setScreening(screening);
                ticket.setUser(user);   // null для гостя
                ticket.setCustomerName(customerName);
                ticket.setSeat(seat);
                ticket.setEmail(email);

                // уникальный токен для QR
                ticket.setQrToken(UUID.randomUUID().toString());

                ticketRepository.save(ticket);
                createdTickets.add(ticket);
            }
        }

        if (createdTickets.isEmpty()) {
            // все места уже заняты – редирект обратно к выбору
            return "redirect:/tickets/book/" + screeningId + "?occupied";
        }

        // генерируем QR-коды (по одному на каждый билет)
        Map<Long, String> qrCodes = new HashMap<>();
        for (Ticket t : createdTickets) {
            String text = "TICKET:" + t.getQrToken();
            String base64 = qrCodeService.generateQrBase64(text, 220, 220);
            qrCodes.put(t.getId(), base64);
        }

        // письмо на почту (если email есть)
        if (email != null && !email.isBlank()) {
            emailService.sendTicketsEmail(email, createdTickets, qrCodes);
        }

        // показываем страницу успешной оплаты с QR-кодами
        model.addAttribute("screening", screening);
        model.addAttribute("tickets", createdTickets);
        model.addAttribute("qrCodes", qrCodes);
        model.addAttribute("totalPrice", total);
        model.addAttribute("ticketsCount", createdTickets.size());
        model.addAttribute("emailUsed", email);

        return "tickets/success";
    }
}
