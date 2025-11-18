package com.example.cinema.config;

import com.example.cinema.repo.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // открыто всем
                        .requestMatchers("/", "/login",
                                "/css/**", "/js/**", "/images/**").permitAll()
                        // админка
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // операции с билетами для авторизованных юзеров
                        .requestMatchers("/tickets/**").hasRole("USER")
                        // всё остальное требует входа
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")    // своя страница логина
                        .permitAll()
                        .defaultSuccessUrl("/", true)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        // чтобы работала H2-консоль в iframe (когда подключим)
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**"));
        http.headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Загружаем пользователя из БД по username.
     * Роли из сущности AppUser (строки "ADMIN"/"USER")
     * превращаются в роли Spring Security (ROLE_ADMIN/ROLE_USER).
     */
    @Bean
    public UserDetailsService userDetailsService(AppUserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(user -> User.withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRoles().toArray(new String[0]))
                        .build()
                )
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
