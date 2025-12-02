package com.example.cinema.service;

import com.example.cinema.domain.AppUser;
import com.example.cinema.domain.Movie;
import com.example.cinema.repo.AppUserRepository;
import com.example.cinema.repo.MovieRepository;
import com.example.cinema.repo.ScreeningRepository;
import com.example.cinema.repo.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;
    private final TicketRepository ticketRepository;
    private final AppUserRepository userRepository;

    public MovieService(MovieRepository movieRepository,
                        ScreeningRepository screeningRepository,
                        TicketRepository ticketRepository,
                        AppUserRepository userRepository) {
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void deleteMovieWithRelations(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + movieId));

        // 1. Удаляем все билеты на сеансы этого фильма
        ticketRepository.deleteByScreening_Movie_Id(movieId);

        // 2. Удаляем все сеансы этого фильма
        screeningRepository.deleteByMovie_Id(movieId);

        // 3. Убираем фильм из избранного у всех пользователей
        List<AppUser> usersWithFavorite = userRepository.findByFavoriteMovies_Id(movieId);
        for (AppUser user : usersWithFavorite) {
            user.getFavoriteMovies().remove(movie);
        }
        userRepository.saveAll(usersWithFavorite);

        // 4. Удаляем сам фильм
        movieRepository.delete(movie);
    }
}