package com.example.cinema.repo;

import com.example.cinema.domain.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    // при необходимости сюда можно добавлять свои методы поиска
}
