package com.example.cinema.controller;

import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.ScreeningRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;

    public PublicController(MovieRepository movieRepository,
                            ScreeningRepository screeningRepository) {
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("screenings", screeningRepository.findAll());
        return "index"; // templates/index.html
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // templates/login.html
    }
}
