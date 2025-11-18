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

@Controller
@RequestMapping("/admin/screenings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminScreeningController {

    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;

    public AdminScreeningController(ScreeningRepository screeningRepository,
                                    MovieRepository movieRepository) {
        this.screeningRepository = screeningRepository;
        this.movieRepository = movieRepository;
    }

    // список сеансов
    @GetMapping
    public String list(Model model) {
        model.addAttribute("screenings", screeningRepository.findAll());
        return "admin/screenings/list";
    }

    // форма создания
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("screening", new Screening());
        model.addAttribute("movies", movieRepository.findAll());
        return "admin/screenings/form";
    }

    // создание
    @PostMapping
    public String create(@Valid @ModelAttribute("screening") Screening screening,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("movies", movieRepository.findAll());
            return "admin/screenings/form";
        }
        screeningRepository.save(screening);
        return "redirect:/admin/screenings";
    }

    // форма редактирования
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Screening screening = screeningRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Screening not found: " + id));
        model.addAttribute("screening", screening);
        model.addAttribute("movies", movieRepository.findAll());
        return "admin/screenings/form";
    }

    // обновление
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("screening") Screening screening,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("movies", movieRepository.findAll());
            return "admin/screenings/form";
        }
        screening.setId(id);
        screeningRepository.save(screening);
        return "redirect:/admin/screenings";
    }

    // удаление
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        screeningRepository.deleteById(id);
        return "redirect:/admin/screenings";
    }
}
