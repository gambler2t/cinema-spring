package com.example.cinema.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration // Помечаем класс как конфигурацию Spring MVC
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.posters-dir:posters}") // Читаем путь к папке с постерами из настроек (или берём "posters" по умолчанию)
    private String postersDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Paths.get(postersDir).toAbsolutePath() + "/"; // Строим абсолютный файловый путь к каталогу постеров
        registry.addResourceHandler("/posters/**") // Все запросы к /posters/... обрабатываем как к статическим ресурсам
                .addResourceLocations(location);    // И отдаем файлы из папки postersDir на диске
    }
}