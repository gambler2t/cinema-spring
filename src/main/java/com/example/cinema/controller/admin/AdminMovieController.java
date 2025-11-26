package com.example.cinema.controller.admin;

import com.example.cinema.domain.Movie;
import com.example.cinema.repo.MovieRepository;
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

    public AdminMovieController(MovieRepository movieRepository,
                                PosterStorageService posterStorageService) {
        this.movieRepository = movieRepository;
        this.posterStorageService = posterStorageService;
    }

    // Список всех фильмов
    @GetMapping
    public String list(Model model) {
        model.addAttribute("movies", movieRepository.findAll());
        return "admin/movies/list";
    }

    // Форма создания фильма
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("movie", new Movie());
        return "admin/movies/form";
    }

    // Форма редактирования существующего фильма
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Фильм не найден: " + id));
        model.addAttribute("movie", movie);
        return "admin/movies/form";
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
                    "Длительность должна быть положительным числом");
        }

        if (bindingResult.hasErrors()) {
            return "admin/movies/form";
        }

        try {
            // Загружаем постер, если был выбран новый файл
            if (posterFile != null && !posterFile.isEmpty()) {
                String url = posterStorageService.store(posterFile);
                movie.setPosterUrl(url);
            }
        } catch (Exception e) {
            logger.error("Ошибка загрузки постера", e);
            bindingResult.rejectValue("posterFile",
                    "upload.failed",
                    "Не удалось загрузить постер: " + e.getMessage());
            return "admin/movies/form";
        }

        movieRepository.save(movie);
        return "redirect:/admin/movies";
    }

    // Удаление фильма
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        movieRepository.findById(id).ifPresent(movieRepository::delete);
        return "redirect:/admin/movies";
    }
}
