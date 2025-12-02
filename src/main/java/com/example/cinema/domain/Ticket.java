package com.example.cinema.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tickets") // Таблица билетов в базе
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Автоинкрементный первичный ключ
    private Long id;

    // много билетов к одному сеансу
    @ManyToOne(optional = false) // Каждый билет всегда привязан к какому-то сеансу
    @JoinColumn(name = "screening_id") // Внешний ключ на таблицу сеансов
    private Screening screening;

    // владелец билета (пользователь) - может быть null для гостя
    @ManyToOne(fetch = FetchType.LAZY) // Пользователь подгружается по мере необходимости
    @JoinColumn(name = "user_id")
    private AppUser user;

    @NotBlank // Имя покупателя обязательно
    private String customerName;

    @NotBlank // Место (ряд-место) обязательно
    private String seat;

    // Email покупателя (для гостей обязателен, для пользователя можно пустой)
    private String email;

    // Уникальный токен, который кодируем в QR
    @Column(name = "qr_token", unique = true, length = 64) // Уникальное значение для идентификации билета
    private String qrToken;

    public Ticket() {
    }

    public Ticket(Screening screening, AppUser user, String customerName, String seat) {
        this.screening = screening;
        this.user = user;
        this.customerName = customerName;
        this.seat = seat;
    }

    // getters / setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Screening getScreening() {
        return screening;
    }

    public void setScreening(Screening screening) {
        this.screening = screening;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getSeat() {
        return seat;
    }

    public void setSeat(String seat) {
        this.seat = seat;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }
}