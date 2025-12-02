package com.example.cinema.service;

import com.example.cinema.domain.Ticket;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service // Сервис для отправки email-сообщений
public class EmailService {

    private final JavaMailSender mailSender; // Spring-обёртка над JavaMail для отправки писем

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTicketsEmail(String to,
                                 List<Ticket> tickets,
                                 Map<Long, String> qrBase64) {

        if (to == null || to.isBlank()) {
            return; // Если адрес пустой — просто ничего не отправляем
        }

        try {
            MimeMessage message = mailSender.createMimeMessage(); // Создаём MIME-письмо
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8"); // Помощник для удобной работы с HTML и вложениями

            helper.setTo(to);
            helper.setSubject("Ваши билеты в кино"); // Тема письма

            StringBuilder html = new StringBuilder();
            html.append("<h2>Спасибо за покупку!</h2>");
            html.append("<p>Ниже — ваши билеты и QR-коды.</p>");

            // Формируем HTML-блок для каждого билета
            for (Ticket t : tickets) {
                String img = qrBase64.get(t.getId()); // Берём base64-картинку QR по id билета
                html.append("<hr/>");
                html.append("<p>")
                        .append("<strong>Фильм:</strong> ")
                        .append(escape(t.getScreening().getMovie().getTitle())) // Экранируем спецсимволы
                        .append("<br/>")
                        .append("<strong>Дата и время:</strong> ")
                        .append(t.getScreening().getStartTime())
                        .append("<br/>")
                        .append("<strong>Место:</strong> ")
                        .append(escape(t.getSeat()))
                        .append("</p>");

                // Встраиваем QR-код как data:image/png;base64
                if (img != null) {
                    html.append("<img style=\"width:220px;height:220px;border-radius:12px;\" ")
                            .append("src=\"data:image/png;base64,")
                            .append(img)
                            .append("\" />");
                }
            }

            helper.setText(html.toString(), true); // true — это HTML-формат

            // Может бросить MailSendException
            mailSender.send(message); // Пытаемся отправить письмо

        } catch (MessagingException | MailException e) {
            // В разработке просто логируем, чтобы покупка не падала
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Простая экранизация HTML-спецсимволов, чтобы защититься от некорректного вывода
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}