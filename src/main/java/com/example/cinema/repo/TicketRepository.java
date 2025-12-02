package com.example.cinema.repo;

import com.example.cinema.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository // Репозиторий для работы с сущностью Ticket (билеты)
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUser_Username(String username); // Все билеты, принадлежащие пользователю с данным логином

    List<Ticket> findByScreening_Id(Long screeningId); // Все билеты для конкретного сеанса

    void deleteByUser_Id(Long userId); // Удалить все билеты пользователя (используется при удалении юзера админом)

    boolean existsByScreening_IdAndSeat(Long screeningId, String seat); // Проверить, занято ли конкретное место на сеансе

    // Поиск по токену QR (если понадобится)
    Ticket findByQrToken(String qrToken); // Найти билет по его уникальному QR-токену

    // Билеты гостя по e-mail, только будущие, по времени
    List<Ticket> findByEmailAndScreening_StartTimeAfterOrderByScreening_StartTimeAsc(
            String email,
            LocalDateTime startTime
    ); // Билеты гостя с заданным email на сеансы, которые ещё не начались, по возрастанию времени

    // Удалить все билеты для всех сеансов указанного фильма
    void deleteByScreening_Movie_Id(Long movieId);
}