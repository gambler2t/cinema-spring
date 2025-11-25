package com.example.cinema.config;

import com.example.cinema.domain.AppUser;
import com.example.cinema.repo.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // CSRF выключаем, чтобы не мучиться с токенами в форме логина (для учебного проекта ок)
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // открыто всем (гости тоже видят фильмы и логин)
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/about",
                                "/movies/**",
                                "/screenings/**",
                                "/css/**",
                                "/images/**",
                                "/posters/**",
                                "/h2-console/**"
                        ).permitAll()

                        // админка только для ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // билеты и профиль только для USER
                        .requestMatchers("/tickets/**", "/user/**").hasRole("USER")

                        // всё остальное требует входа
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")          // наш login.html
                        .loginProcessingUrl("/login") // POST /login обрабатывает Spring Security
                        .defaultSuccessUrl("/", true) // после успешного логина → на главную (/ → /movies)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )

                // чтобы открывалась H2-console в iframe
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    // Говорим Spring Security, как искать пользователя в БД
    @Bean
    public UserDetailsService userDetailsService(AppUserRepository userRepository) {
        return username -> {
            AppUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            // здесь исправление: тип списка = List<SimpleGrantedAuthority>
            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    authorities   // это Collection<? extends GrantedAuthority>
            );
        };
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
