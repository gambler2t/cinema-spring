package com.example.cinema.controller;

import com.example.cinema.domain.Movie;
import com.example.cinema.domain.Screening;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.ScreeningRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    public String home() {
        return "redirect:/movies";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/movies")
    public String moviesList(@RequestParam(value = "q", required = false) String query,
                             Model model) {
        List<Movie> movies;
        if (query != null && !query.isBlank()) {
            movies = movieRepository.findByTitleContainingIgnoreCase(query);
        } else {
            movies = movieRepository.findAll();
        }
        model.addAttribute("movies", movies);
        model.addAttribute("query", query);
        return "movies/list";
    }

    @GetMapping("/screenings")
    public String screeningsList(Model model) {
        List<Screening> screenings = screeningRepository.findAll();
        model.addAttribute("screenings", screenings);
        return "screenings/list";
    }

    @GetMapping("/movies/{id}")
    public String movieDetails(@PathVariable Long id,
                               @RequestParam(value = "date", required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + id));

        // все сеансы этого фильма
        List<Screening> allScreenings =
                screeningRepository.findByMovie_IdOrderByStartTimeAsc(id);

        // только будущие сеансы
        LocalDateTime now = LocalDateTime.now();
        List<Screening> upcoming = allScreenings.stream()
                .filter(s -> !s.getStartTime().isBefore(now))
                .toList();

        // доступные даты
        List<LocalDate> dates = upcoming.stream()
                .map(s -> s.getStartTime().toLocalDate())
                .distinct()
                .sorted()
                .toList();

        // выбранная дата
        LocalDate selectedDate;
        if (date != null) {
            selectedDate = date;
        } else if (!dates.isEmpty()) {
            selectedDate = dates.get(0);
        } else {
            selectedDate = null;
        }

        // сеансы только выбранного дня
        List<Screening> screeningsForDay;
        if (selectedDate != null) {
            screeningsForDay = upcoming.stream()
                    .filter(s -> s.getStartTime().toLocalDate().equals(selectedDate))
                    .toList();
        } else {
            screeningsForDay = List.of();
        }

        model.addAttribute("movie", movie);
        model.addAttribute("availableDates", dates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("screeningsForDay", screeningsForDay);

        return "movies/details";
    }

    @GetMapping("/about")
    public String about() {
        return "about";   // будет искать шаблон about.html
    }

}
