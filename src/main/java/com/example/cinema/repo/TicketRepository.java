package com.example.cinema.repo;

import com.example.cinema.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUser_Username(String username);

    List<Ticket> findByScreening_Id(Long screeningId);

    void deleteByUser_Id(Long userId);

    boolean existsByScreening_IdAndSeat(Long screeningId, String seat);

    // если где-то пригодится поиск по токену
    Ticket findByQrToken(String qrToken);

    // все будущие билеты, купленные на этот e-mail, отсортированные по времени
    List<Ticket> findByEmailAndScreening_StartTimeAfterOrderByScreening_StartTimeAsc(
            String email,
            LocalDateTime startTime
    );
}
