package com.example.cinema.repo;

import com.example.cinema.domain.Screening;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    // все сеансы конкретного фильма, отсортированные по времени
    List<Screening> findByMovie_IdOrderByStartTimeAsc(Long movieId);
}
