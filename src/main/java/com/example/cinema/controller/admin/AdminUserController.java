package com.example.cinema.controller.admin;

import com.example.cinema.domain.AppUser;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.TicketRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller // Контроллер для управления пользователями в админке
@RequestMapping("/admin/users") // Все маршруты начинаются с /admin/users
@PreAuthorize("hasRole('ADMIN')") // Доступ только для админов
public class AdminUserController {

    private final AppUserRepository userRepository; // Репозиторий для работы с пользователями
    private final TicketRepository ticketRepository; // Репозиторий для работы с билетами

    // Инжектируем репозитории
    public AdminUserController(AppUserRepository userRepository,
                               TicketRepository ticketRepository) {
        this.userRepository = userRepository; // Сохраняем репозиторий пользователей
        this.ticketRepository = ticketRepository; // Сохраняем репозиторий билетов
    }

    // Получить список всех пользователей
    @GetMapping
    public String listUsers(Model model) {
        // Получаем всех пользователей
        model.addAttribute("users", userRepository.findAll()); // Кладём список пользователей в модель
        return "admin/users/list";  // Шаблон, который отображает список пользователей
    }

    // Удалить пользователя
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, Principal principal) {

        // Найти пользователя по ID
        AppUser user = userRepository.findById(id).orElse(null); // Ищем пользователя в базе
        if (user == null) {
            return "redirect:/admin/users";  // Если пользователя не существует, редирект на список
        }

        // Проверка на то, чтобы не удалить самого себя
        if (principal != null && principal.getName().equals(user.getUsername())) {
            return "redirect:/admin/users"; // Нельзя удалить себя
        }

        // Удаляем все билеты пользователя
        ticketRepository.deleteByUser_Id(id); // Сначала очищаем связанные билеты, чтобы не оставлять висящие записи

        // Удаляем самого пользователя
        userRepository.delete(user); // Удаляем пользователя из базы

        // После удаления редирект на список пользователей
        return "redirect:/admin/users";
    }
}