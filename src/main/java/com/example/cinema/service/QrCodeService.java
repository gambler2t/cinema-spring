package com.example.cinema.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service // Сервис для генерации QR-кодов
public class QrCodeService {

    // Для success-страницы и писем
    public String generateQrBase64(String text, int width, int height) {
        byte[] bytes = generateQrBytes(text, width, height); // Генерируем PNG как массив байт
        return Base64.getEncoder().encodeToString(bytes);     // Кодируем PNG в base64-строку для встраивания в HTML
    }

    // Для отдачи PNG по /tickets/qr/{id}
    public byte[] generateQrBytes(String text, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter(); // ZXing-генератор QR-кодов
            BitMatrix bm = writer.encode(text, BarcodeFormat.QR_CODE, width, height); // Строим матрицу QR-кода
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bm); // Конвертируем матрицу в картинку

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos); // Записываем PNG в буфер

            return baos.toByteArray(); // Возвращаем бинарные данные PNG
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Ошибка генерации QR-кода", e); // Превращаем в unchecked-исключение
        }
    }
}