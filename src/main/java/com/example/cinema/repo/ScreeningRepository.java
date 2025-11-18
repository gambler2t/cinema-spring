package com.example.cinema.repo;

import com.example.cinema.domain.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    List<Screening> findByMovie_IdOrderByStartTimeAsc(Long movieId);
}
