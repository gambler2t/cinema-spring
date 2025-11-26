package com.example.cinema.repo;

import com.example.cinema.domain.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    // Этот метод должен быть для PublicController
    List<Screening> findByMovie_IdOrderByStartTimeAsc(Long movieId);

    // Другие методы если нужны...
}