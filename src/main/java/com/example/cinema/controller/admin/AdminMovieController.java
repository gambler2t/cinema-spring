package com.example.cinema.controller.admin;

import com.example.cinema.domain.Movie;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.service.MovieService;
import com.example.cinema.service.PosterStorageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {

    private static final Logger logger = LoggerFactory.getLogger(AdminMovieController.class);

    private final MovieRepository movieRepository;
    private final PosterStorageService posterStorageService;
    private final MovieService movieService; // Новое поле

    public AdminMovieController(MovieRepository movieRepository,
                                PosterStorageService posterStorageService,
                                MovieService movieService) { // Добавили movieService
        this.movieRepository = movieRepository;
        this.posterStorageService = posterStorageService;
        this.movieService = movieService;
    }

    // Список всех фильмов
    @GetMapping
    public String list(Model model) {
        model.addAttribute("movies", movieRepository.findAll()); // Передаём в модель список всех фильмов из базы
        return "admin/movies/list"; // Отображаем шаблон со списком фильмов
    }

    // Форма создания фильма
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("movie", new Movie()); // Кладём в модель пустой объект Movie для формы
        return "admin/movies/form"; // Используем ту же форму, что и для редактирования
    }

    // Форма редактирования существующего фильма
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Фильм не найден: " + id)); // Если фильм не найден — выбрасываем исключение
        model.addAttribute("movie", movie); // Передаём найденный фильм в модель
        return "admin/movies/form"; // Показываем форму с уже заполненными данными
    }

    // Сохранение фильма (создание или редактирование)
    @PostMapping
    public String save(@Valid @ModelAttribute("movie") Movie movie,
                       BindingResult bindingResult,
                       @RequestParam(value = "posterFile", required = false) MultipartFile posterFile) {

        // базовая валидация
        if (movie.getDurationMinutes() != null && movie.getDurationMinutes() < 0) {
            bindingResult.rejectValue("durationMinutes",
                    "value.negative",
                    "Длительность должна быть положительным числом"); // Добавляем ошибку валидации для поля длительности
        }

        if (bindingResult.hasErrors()) {
            return "admin/movies/form"; // Если есть ошибки — снова показываем форму
        }

        try {
            // Загружаем постер, если был выбран новый файл
            if (posterFile != null && !posterFile.isEmpty()) { // Проверяем, что файл передан и не пустой
                String url = posterStorageService.store(posterFile); // Сохраняем файл и получаем URL постера
                movie.setPosterUrl(url); // Сохраняем URL постера в сущность фильма
            }
        } catch (Exception e) {
            logger.error("Ошибка загрузки постера", e); // Логируем ошибку сохранения постера
            bindingResult.rejectValue("posterFile",
                    "upload.failed",
                    "Не удалось загрузить постер: " + e.getMessage()); // Добавляем ошибку к полю постера
            return "admin/movies/form"; // Возвращаем форму с сообщением об ошибке
        }

        movieRepository.save(movie); // Сохраняем фильм (новый или обновлённый) в базе
        return "redirect:/admin/movies"; // После успешного сохранения редиректим на список фильмов
    }

    // Удаление фильма
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        movieService.deleteMovieWithRelations(id); // Вместо прямого delete через репозиторий
        return "redirect:/admin/movies";
    }
}