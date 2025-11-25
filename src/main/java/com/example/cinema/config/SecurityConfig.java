package com.example.cinema.config;

import com.example.cinema.domain.AppUser;
import com.example.cinema.repo.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
                .authorizeHttpRequests(auth -> auth
                        // ОТКРЫТЫЕ СТРАНИЦЫ (видны всем)
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/about",
                                "/movies/**",
                                "/screenings/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/posters/**",
                                "/h2-console/**"
                        ).permitAll()

                        // админка только для ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // личный кабинет и билеты – только для USER
                        .requestMatchers("/tickets/**", "/user/**").hasRole("USER")

                        // всё остальное – только после логина
                        .anyRequest().authenticated()
                )

                // ЛОГИН
                .formLogin(form -> form
                        .loginPage("/login")          // своя страница логина
                        .loginProcessingUrl("/login") // POST /login обрабатывает Spring Security
                        .permitAll()
                        .defaultSuccessUrl("/movies", true) // после удачного логина → /movies
                )

                // ЛОГАУТ
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/movies")
                        .permitAll()
                )

                // CSRF выключаем для простоты
                .csrf(AbstractHttpConfigurer::disable)

                // чтобы работала H2-консоль
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    // как искать пользователя в БД
    @Bean
    public UserDetailsService userDetailsService(AppUserRepository userRepository) {
        return username -> {
            AppUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            // ВАЖНО: тип именно List<SimpleGrantedAuthority>, никакого GrantedAuthority
            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    authorities      // подходит, т.к. это Collection<? extends GrantedAuthority>
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
