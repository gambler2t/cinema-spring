package com.example.cinema.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "screenings") // Таблица сеансов в базе
public class Screening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Автоинкрементный ID
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER) // Много сеансов к одному фильму, фильм всегда обязателен, загружается сразу
    @JoinColumn(name = "movie_id") // Внешний ключ на таблицу фильмов
    private Movie movie;

    @NotNull
    @Future // Дата/время сеанса должны быть в будущем (валидация)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") // Формат для биндинга из HTML-формы
    private LocalDateTime startTime;

    @NotBlank // Название зала обязательно
    private String hall;

    @NotNull // Цена обязательна
    private BigDecimal price;

    public Screening() {
    }

    public Screening(Movie movie,
                     LocalDateTime startTime,
                     String hall,
                     BigDecimal price) {
        this.movie = movie;
        this.startTime = startTime;
        this.hall = hall;
        this.price = price;
    }

    // getters / setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public String getHall() {
        return hall;
    }

    public void setHall(String hall) {
        this.hall = hall;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}