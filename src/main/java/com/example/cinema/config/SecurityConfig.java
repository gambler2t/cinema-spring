package com.example.cinema.config;

import com.example.cinema.domain.AppUser;
import com.example.cinema.repo.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration // Класс с конфигурацией Spring Security
public class SecurityConfig {

    @Bean // Определяем цепочку фильтров безопасности для HTTP-запросов
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN") // Доступ только для администраторов
                        .requestMatchers("/**").permitAll() // Все остальные страницы доступны всем
                )
                .formLogin(form -> form
                        .loginPage("/login") // Страница входа
                        .loginProcessingUrl("/process-login") // URL для обработки формы входа
                        .defaultSuccessUrl("/movies", true) // После успешного входа — редирект на главную страницу
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/movies") // После выхода — редирект на главную страницу
                        .permitAll())
                .csrf(csrf -> csrf.disable()) // Отключаем CSRF для простоты
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())); // Разрешаем фреймы с того же источника

        return http.build(); // Собираем и возвращаем конфигурацию фильтров
    }

    @Bean // Сервис, который Spring Security использует для загрузки пользователей по логину
    public UserDetailsService userDetailsService(AppUserRepository userRepository) {
        return username -> {
            AppUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username)); // Если пользователя нет — бросаем исключение

            // Присваиваем роли пользователю
            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // Префикс ROLE_ добавляется автоматически
                    .toList();

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(), // Логин из базы
                    user.getPassword(), // Уже закодированный пароль
                    authorities // Коллекция ролей/прав для Spring Security
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Используем BCrypt для кодирования паролей
    }
}