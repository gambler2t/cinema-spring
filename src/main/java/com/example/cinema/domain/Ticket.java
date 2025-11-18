package com.example.cinema.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // много билетов к одному сеансу
    @ManyToOne(optional = false)
    @JoinColumn(name = "screening_id")
    private Screening screening;

    @NotBlank
    private String customerName;

    @NotBlank
    private String seat;

    public Ticket() {
    }

    public Ticket(Screening screening, String customerName, String seat) {
        this.screening = screening;
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
}
