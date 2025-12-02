package com.example.cinema.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank(message = "Логин обязателен") // Логин не может быть пустым
    @Size(min = 3, max = 32, message = "Логин должен быть от 3 до 32 символов") // Ограничение длины логина
    private String username;

    @Size(max = 100, message = "Имя слишком длинное") // Необязательное поле, но с ограничением длины
    private String fullName;

    @Email(message = "Некорректный email") // Проверка формата email
    @Size(max = 100, message = "Email слишком длинный") // Ограничение длины email
    private String email;

    @NotBlank(message = "Пароль обязателен") // Пароль обязателен
    @Size(min = 6, message = "Пароль должен быть не короче 6 символов") // Минимальная длина пароля
    private String password;

    @NotBlank(message = "Повтор пароля обязателен") // Повтор пароля тоже обязателен
    private String confirmPassword;

    // getters / setters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}