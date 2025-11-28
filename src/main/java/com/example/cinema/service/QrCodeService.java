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

@Service
public class QrCodeService {

    public String generateQrBase64(String text, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bm = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bm);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Ошибка генерации QR-кода", e);
        }
    }
}
