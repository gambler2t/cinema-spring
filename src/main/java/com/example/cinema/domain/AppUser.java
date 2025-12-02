package com.example.cinema.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity // Сущность JPA — будет таблицей в базе
@Table(name = "users") // Явно указываем имя таблицы
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Автоинкрементный первичный ключ
    private Long id;

    @Column(unique = true, nullable = false) // Логин обязателен и должен быть уникален
    private String username;

    private String password; // Хранится в зашифрованном виде (BCrypt)

    private String fullName;

    private String email;

    @Column(length = 1000) // Ограничиваем длину bio в базе
    private String bio;

    // роли: USER, ADMIN и т.п.
    @ElementCollection(fetch = FetchType.EAGER) // Коллекция простых значений (строки) вместо отдельной сущности
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id")) // Отдельная таблица ролей
    @Column(name = "role") // Колонка, где хранится строковое значение роли
    private Set<String> roles = new HashSet<>();

    @ManyToMany // Многие-ко-многим: пользователь ↔ любимые фильмы
    @JoinTable(
            name = "favorite_movies", // Промежуточная таблица для связи
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    private Set<Movie> favoriteMovies = new HashSet<>();

    public AppUser() {
    }

    public AppUser(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    public void addRole(String role) {
        roles.add(role); // Удобный метод для добавления роли
    }

    // Методы для работы с избранными фильмами
    public boolean hasFavoriteMovie(Movie movie) {
        return favoriteMovies.contains(movie);
    }

    public void addFavoriteMovie(Movie movie) {
        this.favoriteMovies.add(movie);
    }

    public void removeFavoriteMovie(Movie movie) {
        this.favoriteMovies.remove(movie);
    }

    // getters / setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Set<Movie> getFavoriteMovies() {
        return favoriteMovies;
    }

    public void setFavoriteMovies(Set<Movie> favoriteMovies) {
        this.favoriteMovies = favoriteMovies;
    }
}