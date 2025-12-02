package com.example.cinema.repo;

import com.example.cinema.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Репозиторий для работы с сущностью AppUser (пользователи)
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Поиск пользователя по логину
    Optional<AppUser> findByUsername(String username);

    // Проверка, существует ли пользователь с таким логином (для валидации при регистрации)
    boolean existsByUsername(String username);   // <-- уже было

    // Все пользователи, у которых в избранном есть фильм с таким id
    List<AppUser> findByFavoriteMovies_Id(Long movieId);
}