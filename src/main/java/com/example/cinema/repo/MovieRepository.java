package com.example.cinema.repo;

import com.example.cinema.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    // пока достаточно базовых методов из JpaRepository
}
