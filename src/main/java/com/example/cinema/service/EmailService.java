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

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTicketsEmail(String to,
                                 List<Ticket> tickets,
                                 Map<Long, String> qrBase64) {

        // если e-mail не указан — просто выходим
        if (to == null || to.isBlank()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Ваши билеты в кино");

            StringBuilder html = new StringBuilder();
            html.append("<h2>Спасибо за покупку!</h2>");
            html.append("<p>Ниже — ваши билеты и QR-коды.</p>");

            for (Ticket t : tickets) {
                String img = qrBase64.get(t.getId());
                html.append("<hr/>");
                html.append("<p>")
                        .append("<strong>Фильм:</strong> ")
                        .append(escape(t.getScreening().getMovie().getTitle()))
                        .append("<br/>")
                        .append("<strong>Дата и время:</strong> ")
                        .append(t.getScreening().getStartTime())
                        .append("<br/>")
                        .append("<strong>Место:</strong> ")
                        .append(escape(t.getSeat()))
                        .append("</p>");

                if (img != null) {
                    html.append("<img style=\"width:220px;height:220px;border-radius:12px;\" ")
                            .append("src=\"data:image/png;base64,")
                            .append(img)
                            .append("\" />");
                }
            }

            helper.setText(html.toString(), true);

            // здесь может вылетать MailSendException, её тоже ловим
            mailSender.send(message);

        } catch (MessagingException | MailException e) {
            // В режиме разработки просто логируем и продолжаем,
            // чтобы покупка не падала из-за почты.
            e.printStackTrace();
        } catch (Exception e) {
            // на всякий пожарный, чтобы вообще НИЧЕГО из почты не сломало покупку
            e.printStackTrace();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
