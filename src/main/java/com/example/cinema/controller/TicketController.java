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
@RequestMapping("/tickets") // Все маршруты этого контроллера начинаются с /tickets
public class TicketController {

    private final ScreeningRepository screeningRepository; // Репозиторий для работы с сеансами
    private final TicketRepository ticketRepository;       // Репозиторий для работы с билетами
    private final AppUserRepository userRepository;        // Репозиторий пользователей (нужен для связи билетов с юзером)
    private final QrCodeService qrCodeService;             // Сервис генерации QR-кодов
    private final EmailService emailService;               // Сервис отправки писем с билетами

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

    // ШАГ 1. ВЫБОР МЕСТ

    @GetMapping("/book/{screeningId}")
    public String showBookingForm(@PathVariable Long screeningId,
                                  Model model,
                                  Authentication authentication) {

        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + screeningId)); // Проверяем, что сеанс существует

        Ticket ticket = new Ticket();
        ticket.setScreening(screening); // Привязываем билет к выбранному сеансу

        // Если пользователь залогинен, подставляем его имя в поле "ФИО" в форме
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

        int rowsCount = 10;     // Количество рядов в зале
        int seatsPerRow = 18;   // Количество мест в каждом ряду

        // Находим уже занятые места для этого сеанса
        Set<String> occupiedSeats = ticketRepository.findByScreening_Id(screeningId)
                .stream()
                .map(Ticket::getSeat)
                .collect(Collectors.toSet());

        // Списки номеров рядов и мест для отображения в шаблоне
        List<Integer> rows = IntStream.rangeClosed(1, rowsCount).boxed().collect(Collectors.toList());
        List<Integer> seats = IntStream.rangeClosed(1, seatsPerRow).boxed().collect(Collectors.toList());

        model.addAttribute("ticket", ticket);
        model.addAttribute("rows", rows);
        model.addAttribute("seats", seats);
        model.addAttribute("occupiedSeats", occupiedSeats);      // Эти места в шаблоне делаются недоступными
        model.addAttribute("selectedSeats", new ArrayList<String>()); // Список выбранных мест (изначально пустой)

        return "tickets/book"; // Шаблон выбора мест
    }

    // После выбора мест показываем страницу оплаты
    @PostMapping("/book")
    public String showPaymentPage(@ModelAttribute("ticket") Ticket ticketTemplate,
                                  @RequestParam("selectedSeats") String selectedSeatsRaw,
                                  Authentication authentication,
                                  Model model) {

        // Если места не выбраны — возвращаемся к выбору мест
        if (selectedSeatsRaw == null || selectedSeatsRaw.isBlank()) {
            return "redirect:/tickets/book/" + ticketTemplate.getScreening().getId();
        }

        Screening screening = screeningRepository.findById(ticketTemplate.getScreening().getId())
                .orElseThrow(() -> new IllegalArgumentException("Screening not found"));

        // Разбираем строку вида "1-5,1-6" в список
        List<String> selectedSeats = Arrays.stream(selectedSeatsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        int count = selectedSeats.size(); // Сколько мест выбрано
        BigDecimal pricePer = screening.getPrice() != null ? screening.getPrice() : BigDecimal.ZERO;
        BigDecimal total = pricePer.multiply(BigDecimal.valueOf(count)); // Общая цена

        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        String prefilledEmail = null;

        // Для залогиненного пользователя подставляем email из профиля
        if (isAuthenticated) {
            AppUser user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                prefilledEmail = user.getEmail();
            }
        }

        model.addAttribute("screening", screening);
        model.addAttribute("customerName", ticketTemplate.getCustomerName()); // Имя покупателя с предыдущего шага
        model.addAttribute("selectedSeats", selectedSeats);
        model.addAttribute("selectedSeatsRaw", String.join(",", selectedSeats)); // Строка для повторной передачи
        model.addAttribute("ticketsCount", count);
        model.addAttribute("totalPrice", total);
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("prefilledEmail", prefilledEmail); // Почта, подставленная для удобства

        return "tickets/payment"; // Страница оплаты
    }

    // ШАГ 2. ОПЛАТА И СОЗДАНИЕ БИЛЕТОВ

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
        BigDecimal total = pricePer.multiply(BigDecimal.valueOf(count)); // Финальная сумма за все билеты

        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        AppUser user = null;

        // Если пользователь залогинен, связываем билеты с ним и при необходимости берём email из профиля
        if (isAuthenticated) {
            user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if ((email == null || email.isBlank()) && user != null) {
                email = user.getEmail();
            }
        }

        // Для гостя email обязателен — иначе некуда отправить билеты
        if (!isAuthenticated && (email == null || email.isBlank())) {
            model.addAttribute("emailError", "Для покупки без регистрации укажите электронную почту.");

            // Возвращаем все данные обратно на страницу оплаты
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

        // Создаём билеты для каждого выбранного места (проверяем, что место ещё свободно)
        for (String seat : selectedSeats) {
            if (!ticketRepository.existsByScreening_IdAndSeat(screeningId, seat)) {
                Ticket ticket = new Ticket();
                ticket.setScreening(screening);
                ticket.setUser(user); // null для гостя
                ticket.setCustomerName(customerName);
                ticket.setSeat(seat);
                ticket.setEmail(email);
                ticket.setQrToken(UUID.randomUUID().toString()); // Уникальный токен для QR-кода

                ticketRepository.save(ticket);
                createdTickets.add(ticket);
            }
        }

        // Если все места уже заняты — возвращаемся к выбору с флагом occupied
        if (createdTickets.isEmpty()) {
            return "redirect:/tickets/book/" + screeningId + "?occupied";
        }

        // Генерируем QR-коды (в base64) для свежесозданных билетов
        Map<Long, String> qrCodes = new HashMap<>();
        for (Ticket t : createdTickets) {
            String text = "TICKET:" + t.getQrToken(); // Содержимое, зашитое в QR
            String base64 = qrCodeService.generateQrBase64(text, 220, 220);
            qrCodes.put(t.getId(), base64);
        }

        // Отправка письма с билетами, если указан email
        if (email != null && !email.isBlank()) {
            emailService.sendTicketsEmail(email, createdTickets, qrCodes);
        }

        model.addAttribute("screening", screening);
        model.addAttribute("tickets", createdTickets);
        model.addAttribute("qrCodes", qrCodes);
        model.addAttribute("totalPrice", total);
        model.addAttribute("ticketsCount", createdTickets.size());
        model.addAttribute("emailUsed", email); // Почта, на которую отправили билеты

        return "tickets/success"; // Страница успешной покупки
    }

    // ГОСТЕВОЕ УПРАВЛЕНИЕ БИЛЕТАМИ

    @GetMapping("/guest")
    public String showGuestTickets(@RequestParam(value = "email", required = false) String email,
                                   Model model) {

        List<Ticket> tickets = Collections.emptyList();

        // Если email указан — ищем будущие сеансы с такими билетами
        if (email != null && !email.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            tickets = ticketRepository
                    .findByEmailAndScreening_StartTimeAfterOrderByScreening_StartTimeAsc(email, now);
        }

        model.addAttribute("email", email);
        model.addAttribute("tickets", tickets); // Список билетов гостя для отображения

        return "tickets/guest-tickets"; // Страница управления билетами без аккаунта
    }

    @PostMapping("/guest/cancel/{id}")
    public String cancelGuestTicket(@PathVariable Long id,
                                    @RequestParam("email") String email) {

        String encodedEmail = email != null
                ? UriUtils.encode(email, StandardCharsets.UTF_8) // Кодируем email, чтобы безопасно вернуть его в URL
                : "";

        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket == null) {
            return "redirect:/tickets/guest?email=" + encodedEmail + "&error=notfound"; // Если билет не найден
        }

        LocalDateTime now = LocalDateTime.now();

        // Можно отменить только свой билет, по совпадающему email и только до начала сеанса
        if (ticket.getEmail() == null
                || !ticket.getEmail().equalsIgnoreCase(email)
                || !ticket.getScreening().getStartTime().isAfter(now)) {

            return "redirect:/tickets/guest?email=" + encodedEmail + "&error=notallowed";
        }

        ticketRepository.delete(ticket); // Удаляем билет

        return "redirect:/tickets/guest?email=" + encodedEmail + "&cancelled=1"; // Флаг успешной отмены
    }

    // QR-код для билета пользователя

    @GetMapping("/qr/{id}")
    public ResponseEntity<byte[]> getTicketQr(@PathVariable Long id,
                                              Authentication authentication) {

        // Только авторизованный пользователь может запросить QR
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket == null || ticket.getUser() == null) {
            return ResponseEntity.notFound().build(); // Нет билета или он не привязан к пользователю
        }

        String username = authentication.getName();
        AppUser user = userRepository.findByUsername(username).orElse(null);
        // Проверяем, что билет принадлежит текущему пользователю
        if (user == null || !user.getId().equals(ticket.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String text = "TICKET:" + ticket.getQrToken();
        byte[] png = qrCodeService.generateQrBytes(text, 220, 220); // Генерируем PNG с QR-кодом

        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_PNG) // Возвращаем бинарный PNG
                .body(png);
    }
}