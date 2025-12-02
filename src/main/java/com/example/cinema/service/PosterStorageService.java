package com.example.cinema.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service // Сервис для сохранения файлов постеров на диск
public class PosterStorageService {

    private final Path uploadDir; // Абсолютный путь к директории, куда сохраняем постеры

    // app.posters-dir можно переопределить в application.properties
    public PosterStorageService(@Value("${app.posters-dir:posters}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize(); // Строим нормализованный абсолютный путь
        try {
            Files.createDirectories(this.uploadDir); // Создаём директорию, если её ещё нет
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload dir " + this.uploadDir, e); // Падаем, если каталог создать не удалось
        }
    }

    /**
     * Сохраняет файл и возвращает URL вида /posters/имяфайла.ext
     */
    public String store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file"); // Не даём сохранять пустой файл
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename()); // Очищаем исходное имя файла
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot); // Выделяем расширение (".png", ".jpg" и т.п.)
        }

        String filename = UUID.randomUUID() + ext; // Генерируем уникальное имя файла
        Path target = uploadDir.resolve(filename); // Полный путь, куда сохраняем файл

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING); // Копируем содержимое файла в целевую директорию
        }

        return "/posters/" + filename; // URL, по которому потом будет доступен постер
    }
}