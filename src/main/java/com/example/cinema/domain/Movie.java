package com.example.cinema.domain;

import jakarta.persistence.*;

@Entity // Сущность JPA — таблица movie в базе (имя по умолчанию = Movie)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Автоинкрементный первичный ключ
    private Long id;

    // Название фильма
    private String title;

    // Жанр
    private String genre;

    // Описание
    @Column(length = 2000) // Длинное текстовое поле, увеличиваем максимальную длину
    private String description;

    // Длительность в минутах
    @Column(name = "duration_minutes") // Явно задаём имя столбца в БД
    private Integer durationMinutes;

    // Режиссёр
    private String director;

    // Страна
    private String country;

    // URL постера
    @Column(name = "poster_url") // Ссылка (путь) на картинку постера
    private String posterUrl;

    // ===== getters / setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }
}