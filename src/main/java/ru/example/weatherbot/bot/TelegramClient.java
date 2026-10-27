package ru.example.weatherbot.bot;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
public class TelegramClient {

    private final RestClient pollingClient;
    private final RestClient messageClient;

    public TelegramClient(@Value("${telegram.bot.token}") String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Укажи TELEGRAM_BOT_TOKEN");
        }
        pollingClient = createClient(token, 35);
        messageClient = createClient(token, 8);
    }

    private RestClient createClient(String token, int readTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(readTimeout));
        return RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + token)
                .requestFactory(factory)
                .build();
    }

    public JsonNode getUpdates(long offset) {
        return pollingClient.get()
                .uri(uri -> uri.path("/getUpdates")
                        .queryParam("offset", offset)
                        .queryParam("timeout", 25)
                        .build())
                .retrieve().body(JsonNode.class);
    }

    public void sendMessage(long chatId, String text) {
        messageClient.post().uri("/sendMessage")
                .body(Map.of(
                        "chat_id", chatId,
                        "text", text,
                        "reply_markup", Map.of("remove_keyboard", true)))
                .retrieve().toBodilessEntity();
    }
}
