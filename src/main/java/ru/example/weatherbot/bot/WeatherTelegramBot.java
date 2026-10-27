package ru.example.weatherbot.bot;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.example.weatherbot.service.WeatherService;

@Component
public class WeatherTelegramBot {

    private final TelegramClient telegram;
    private final WeatherService weather;
    private long offset;

    public WeatherTelegramBot(TelegramClient telegram, WeatherService weather) {
        this.telegram = telegram;
        this.weather = weather;
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        try {
            JsonNode response = telegram.getUpdates(offset);
            if (response == null) return;

            for (JsonNode update : response.path("result")) {
                offset = update.path("update_id").asLong() + 1;
                JsonNode message = update.path("message");
                if (!message.path("text").isTextual()) continue;

                long chatId = message.path("chat").path("id").asLong();
                String city = message.path("text").asText().trim();
                if (city.isEmpty() || chatId == 0) continue;

                if (city.equals("/start") || city.equals("/help")) {
                    telegram.sendMessage(chatId, "Привет! Напиши название города, и я покажу текущую погоду.");
                    continue;
                }

                try {
                    telegram.sendMessage(chatId, weather.getWeather(city));
                } catch (Exception e) {
                    System.out.println("Ошибка получения погоды: " + e.getClass().getSimpleName());
                    telegram.sendMessage(chatId, "Не удалось получить погоду. Попробуй позже.");
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка Telegram: " + e.getClass().getSimpleName());
        }
    }
}
