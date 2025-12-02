package com.example.cinema.repo;

import com.example.cinema.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Помечаем интерфейс как Spring-репозиторий (для сканирования компонентов)
public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Поиск фильмов, у которых в названии есть подстрока titlePart (без учёта регистра)
    List<Movie> findByTitleContainingIgnoreCase(String titlePart);
}