package com.example.cinema.controller;

import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.ScreeningRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicController {

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;

    public PublicController(MovieRepository movieRepository,
                            ScreeningRepository screeningRepository) {
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
    }

    // главная страница-«домик» (оставим общий обзор)
    @GetMapping("/")
    public String home() {
        return "redirect:/movies";
    }

    // страница со списком фильмов + строка поиска
    @GetMapping("/movies")
    public String movies(@RequestParam(name = "q", required = false) String query,
                         Model model) {
        if (query != null && !query.isBlank()) {
            model.addAttribute("movies",
                    movieRepository.findByTitleContainingIgnoreCase(query));
        } else {
            model.addAttribute("movies", movieRepository.findAll());
        }
        model.addAttribute("query", query);
        return "movies/list";
    }

    // страница со списком сеансов
    @GetMapping("/screenings")
    public String screenings(Model model) {
        model.addAttribute("screenings", screeningRepository.findAll());
        return "screenings/list";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/movies/{id}")
    public String movieDetails(@PathVariable Long id, Model model) {
        var movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + id));

        model.addAttribute("movie", movie);
        model.addAttribute("screenings",
                screeningRepository.findByMovie_IdOrderByStartTimeAsc(id));

        return "movies/details";
    }
}
