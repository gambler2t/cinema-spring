package com.example.cinema.controller.admin;

import com.example.cinema.domain.Movie;
import com.example.cinema.repo.MovieRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {

    private final MovieRepository movieRepository;

    public AdminMovieController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // список фильмов
    @GetMapping
    public String list(Model model) {
        model.addAttribute("movies", movieRepository.findAll());
        return "admin/movies/list";
    }

    // форма создания
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("movie", new Movie());
        return "admin/movies/form";
    }

    // создание фильма
    @PostMapping
    public String create(@Valid @ModelAttribute("movie") Movie movie,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/movies/form";
        }
        movieRepository.save(movie);
        return "redirect:/admin/movies";
    }

    // форма редактирования
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + id));
        model.addAttribute("movie", movie);
        return "admin/movies/form";
    }

    // обновление фильма
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("movie") Movie movie,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/movies/form";
        }
        movie.setId(id);
        movieRepository.save(movie);
        return "redirect:/admin/movies";
    }

    // удаление
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        movieRepository.deleteById(id);
        return "redirect:/admin/movies";
    }
}
