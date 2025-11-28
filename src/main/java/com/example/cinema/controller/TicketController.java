package com.example.cinema.controller;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Screening;
import com.example.cinema.domain.Ticket;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.ScreeningRepository;
import com.example.cinema.repo.TicketRepository;
import com.example.cinema.service.EmailService;
import com.example.cinema.service.QrCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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

    // ================= ШАГ 1. ВЫБОР МЕСТ =================

    @GetMapping("/book/{screeningId}")
    public String showBookingForm(@PathVariable Long screeningId,
                                  Model model,
                                  Authentication authentication) {

        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + screeningId));

        Ticket ticket = new Ticket();
        ticket.setScreening(screening);

        if (authentication != null && authentication.isAuthenticated()) {
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

    // После выбора мест показываем страницу оплаты
    @PostMapping("/book")
    public String showPaymentPage(@ModelAttribute("ticket") Ticket ticketTemplate,
                                  @RequestParam("selectedSeats") String selectedSeatsRaw,
                                  Authentication authentication,
                                  Model model) {

        if (selectedSeatsRaw == null || selectedSeatsRaw.isBlank()) {
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

    // ================= ШАГ 2. ОПЛАТА И СОЗДАНИЕ БИЛЕТОВ =================

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
            if ((email == null || email.isBlank()) && user != null) {
                email = user.getEmail();
            }
        }

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

        // тут могла бы быть реальная платёжка :)

        List<Ticket> createdTickets = new ArrayList<>();

        for (String seat : selectedSeats) {
            if (!ticketRepository.existsByScreening_IdAndSeat(screeningId, seat)) {
                Ticket ticket = new Ticket();
                ticket.setScreening(screening);
                ticket.setUser(user); // null для гостя
                ticket.setCustomerName(customerName);
                ticket.setSeat(seat);
                ticket.setEmail(email);
                ticket.setQrToken(UUID.randomUUID().toString());

                ticketRepository.save(ticket);
                createdTickets.add(ticket);
            }
        }

        if (createdTickets.isEmpty()) {
            return "redirect:/tickets/book/" + screeningId + "?occupied";
        }

        Map<Long, String> qrCodes = new HashMap<>();
        for (Ticket t : createdTickets) {
            String text = "TICKET:" + t.getQrToken();
            String base64 = qrCodeService.generateQrBase64(text, 220, 220);
            qrCodes.put(t.getId(), base64);
        }

        if (email != null && !email.isBlank()) {
            emailService.sendTicketsEmail(email, createdTickets, qrCodes);
        }

        model.addAttribute("screening", screening);
        model.addAttribute("tickets", createdTickets);
        model.addAttribute("qrCodes", qrCodes);
        model.addAttribute("totalPrice", total);
        model.addAttribute("ticketsCount", createdTickets.size());
        model.addAttribute("emailUsed", email);

        return "tickets/success";
    }

    // ================= ГОСТЕВОЕ УПРАВЛЕНИЕ БИЛЕТАМИ =================

    @GetMapping("/guest")
    public String showGuestTickets(@RequestParam(value = "email", required = false) String email,
                                   Model model) {

        List<Ticket> tickets = Collections.emptyList();

        if (email != null && !email.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            tickets = ticketRepository
                    .findByEmailAndScreening_StartTimeAfterOrderByScreening_StartTimeAsc(email, now);
        }

        model.addAttribute("email", email);
        model.addAttribute("tickets", tickets);

        return "tickets/guest-tickets";
    }

    @PostMapping("/guest/cancel/{id}")
    public String cancelGuestTicket(@PathVariable Long id,
                                    @RequestParam("email") String email) {

        String encodedEmail = email != null
                ? UriUtils.encode(email, StandardCharsets.UTF_8)
                : "";

        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket == null) {
            return "redirect:/tickets/guest?email=" + encodedEmail + "&error=notfound";
        }

        LocalDateTime now = LocalDateTime.now();

        if (ticket.getEmail() == null
                || !ticket.getEmail().equalsIgnoreCase(email)
                || !ticket.getScreening().getStartTime().isAfter(now)) {

            return "redirect:/tickets/guest?email=" + encodedEmail + "&error=notallowed";
        }

        ticketRepository.delete(ticket);

        return "redirect:/tickets/guest?email=" + encodedEmail + "&cancelled=1";
    }

    // ================= QR-код для билета пользователя =================

    @GetMapping("/qr/{id}")
    public ResponseEntity<byte[]> getTicketQr(@PathVariable Long id,
                                              Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket == null || ticket.getUser() == null) {
            return ResponseEntity.notFound().build();
        }

        String username = authentication.getName();
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !user.getId().equals(ticket.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String text = "TICKET:" + ticket.getQrToken();
        byte[] png = qrCodeService.generateQrBytes(text, 220, 220);

        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
