package com.example.cinema.controller.admin;

import com.example.cinema.domain.Movie;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.service.PosterStorageService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {

    private final MovieRepository movieRepository;
    private final PosterStorageService posterStorageService;

    public AdminMovieController(MovieRepository movieRepository,
                                PosterStorageService posterStorageService) {
        this.movieRepository = movieRepository;
        this.posterStorageService = posterStorageService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("movies", movieRepository.findAll());
        return "admin/movies/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("movie", new Movie());
        return "admin/movies/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + id));
        model.addAttribute("movie", movie);
        return "admin/movies/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("movie") Movie movie,
                       BindingResult bindingResult,
                       @RequestParam("posterFile") MultipartFile posterFile) {

        if (bindingResult.hasErrors()) {
            return "admin/movies/form";
        }

        try {
            // если загрузили новый файл – сохраняем и обновляем posterUrl
            if (posterFile != null && !posterFile.isEmpty()) {
                String url = posterStorageService.store(posterFile);
                movie.setPosterUrl(url);
            }
        } catch (Exception e) {
            bindingResult.reject("posterFile", "Failed to upload poster: " + e.getMessage());
            return "admin/movies/form";
        }

        movieRepository.save(movie);
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        movieRepository.deleteById(id);
        return "redirect:/admin/movies";
    }
}
