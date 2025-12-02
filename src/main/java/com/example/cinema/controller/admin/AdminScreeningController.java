package com.example.cinema.controller.admin;

import com.example.cinema.domain.Screening;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.ScreeningRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller // MVC-контроллер для управления сеансами
@RequestMapping("/admin/screenings") // Все обработчики внутри работают под URL /admin/screenings
@PreAuthorize("hasRole('ADMIN')") // Доступ к этому контроллеру только для пользователей с ролью ADMIN
public class AdminScreeningController {

    private final ScreeningRepository screeningRepository; // Репозиторий для работы с сеансами
    private final MovieRepository movieRepository; // Репозиторий для получения списка фильмов

    public AdminScreeningController(ScreeningRepository screeningRepository,
                                    MovieRepository movieRepository) {
        this.screeningRepository = screeningRepository; // Внедрение репозитория сеансов через конструктор
        this.movieRepository = movieRepository; // Внедрение репозитория фильмов через конструктор
    }

    // список сеансов
    @GetMapping
    public String list(Model model) {
        model.addAttribute("screenings", screeningRepository.findAll()); // Передаём все сеансы в модель
        return "admin/screenings/list"; // Шаблон для отображения списка сеансов
    }

    // форма создания
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("screening", new Screening()); // Пустой объект сеанса для формы
        model.addAttribute("movies", movieRepository.findAll()); // Список фильмов для выбора в форме
        return "admin/screenings/form"; // Общий шаблон формы создания/редактирования
    }

    // создание
    @PostMapping
    public String create(@Valid @ModelAttribute("screening") Screening screening,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) { // Если есть ошибки валидации — возвращаем форму
            model.addAttribute("movies", movieRepository.findAll()); // Повторно добавляем фильмы для селекта
            return "admin/screenings/form";
        }
        screeningRepository.save(screening); // Сохраняем новый сеанс в БД
        return "redirect:/admin/screenings"; // После сохранения редирект на список
    }

    // форма редактирования
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Screening screening = screeningRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + id)); // Если сеанс не найден — ошибка
        model.addAttribute("screening", screening); // Существующий сеанс для редактирования
        model.addAttribute("movies", movieRepository.findAll()); // Список фильмов для селекта
        return "admin/screenings/form"; // Та же форма, но с заполненными данными
    }

    // обновление
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("screening") Screening screening,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) { // Проверка ошибок валидации при обновлении
            model.addAttribute("movies", movieRepository.findAll());
            return "admin/screenings/form";
        }
        screening.setId(id); // Явно устанавливаем id, чтобы сохранить изменения существующего сеанса
        screeningRepository.save(screening); // Сохраняем обновлённый сеанс
        return "redirect:/admin/screenings"; // Редирект обратно к списку
    }

    // удаление
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        screeningRepository.deleteById(id); // Удаляем сеанс по id
        return "redirect:/admin/screenings"; // После удаления возвращаемся к списку
    }
}