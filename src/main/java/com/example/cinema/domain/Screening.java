package com.example.cinema.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "screenings")
public class Screening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // связь "много сеансов к одному фильму"
    @ManyToOne(optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotBlank
    private String hall;

    @NotNull
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
