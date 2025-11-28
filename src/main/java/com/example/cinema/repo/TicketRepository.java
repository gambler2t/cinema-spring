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

    // Поиск по токену QR (если понадобится)
    Ticket findByQrToken(String qrToken);

    // Билеты гостя по e-mail, только будущие, по времени
    List<Ticket> findByEmailAndScreening_StartTimeAfterOrderByScreening_StartTimeAsc(
            String email,
            LocalDateTime startTime
    );
}
