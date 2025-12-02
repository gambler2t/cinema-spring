package com.example.cinema.repo;

import com.example.cinema.domain.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Репозиторий для работы с сущностью Screening (сеансы)
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    // Этот метод должен быть для PublicController
    // Находит все сеансы по id фильма и сортирует их по времени начала по возрастанию
    List<Screening> findByMovie_IdOrderByStartTimeAsc(Long movieId);

    // Удалить все сеансы указанного фильма
    void deleteByMovie_Id(Long movieId);
}