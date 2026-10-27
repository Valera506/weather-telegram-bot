package ru.example.weatherbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class WeatherService {

    private final RestClient citiesClient;
    private final RestClient weatherClient;

    public WeatherService() {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(7));

        citiesClient = RestClient.builder()
                .baseUrl("https://geocoding-api.open-meteo.com")
                .requestFactory(factory).build();
        weatherClient = RestClient.builder()
                .baseUrl("https://weather.tk.gg")
                .requestFactory(factory).build();
    }

    public String getWeather(String name) {
        JsonNode cities = citiesClient.get().uri(uri -> uri.path("/v1/search")
                .queryParam("name", name)
                .queryParam("count", 1)
                .queryParam("language", "ru")
                .build()).retrieve().body(JsonNode.class);

        if (cities == null || cities.path("results").isEmpty()) {
            return "Не нашёл такой город. Попробуй другой.";
        }

        JsonNode city = cities.path("results").get(0);
        JsonNode forecast = weatherClient.get().uri(uri -> uri.path("/v1/forecast")
                .queryParam("latitude", city.path("latitude").asDouble())
                .queryParam("longitude", city.path("longitude").asDouble())
                .queryParam("current", "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m")
                .queryParam("wind_speed_unit", "ms")
                .build()).retrieve().body(JsonNode.class);

        if (forecast == null || !forecast.path("current").has("temperature_2m")) {
            throw new IllegalStateException("Погода недоступна");
        }

        JsonNode current = forecast.path("current");
        return "📍 " + city.path("name").asText(name) + ", " + city.path("country").asText("")
                + "\n🌡 Температура: " + current.path("temperature_2m").asDouble() + " °C"
                + "\n🤔 Ощущается как: " + current.path("apparent_temperature").asDouble() + " °C"
                + "\n💧 Влажность: " + current.path("relative_humidity_2m").asInt() + "%"
                + "\n💨 Ветер: " + current.path("wind_speed_10m").asDouble() + " м/с";
    }
}
