package com.example.cinema.controller.admin;

import com.example.cinema.domain.AppUser;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.TicketRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')") // Доступ только для админов
public class AdminUserController {

    private final AppUserRepository userRepository;
    private final TicketRepository ticketRepository;

    // Инжектируем репозитории
    public AdminUserController(AppUserRepository userRepository,
                               TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    // Получить список всех пользователей
    @GetMapping
    public String listUsers(Model model) {
        // Получаем всех пользователей
        model.addAttribute("users", userRepository.findAll());
        return "admin/users/list";  // Шаблон, который отображает список пользователей
    }

    // Удалить пользователя
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, Principal principal) {

        // Найти пользователя по ID
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/admin/users";  // Если пользователя не существует, редирект на список
        }

        // Проверка на то, чтобы не удалить самого себя
        if (principal != null && principal.getName().equals(user.getUsername())) {
            return "redirect:/admin/users"; // Нельзя удалить себя
        }

        // Удаляем все билеты пользователя
        ticketRepository.deleteByUser_Id(id);

        // Удаляем самого пользователя
        userRepository.delete(user);

        // После удаления редирект на список пользователей
        return "redirect:/admin/users";
    }
}

