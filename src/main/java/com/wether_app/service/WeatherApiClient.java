package com.wether_app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wether_app.config.WeatherApiProperties;
import com.wether_app.model.dto.WeatherApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WeatherApiClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WeatherApiProperties props;

    public WeatherApiClient(RestTemplate restTemplate, ObjectMapper objectMapper, WeatherApiProperties props) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    public WeatherApiResponse fetchByCity(String city) {
        String uri = UriComponentsBuilder.fromHttpUrl(props.getUrl())
                .queryParam("q", city)
                .queryParam("appid", props.getKey())
                .queryParam("units", props.getUnits())
                .toUriString();

        return callAndMap(uri);
    }

    public WeatherApiResponse fetchByZip(String zip) {
        String uri = UriComponentsBuilder.fromHttpUrl(props.getUrl())
                .queryParam("zip", zip)
                .queryParam("appid", props.getKey())
                .queryParam("units", props.getUnits())
                .toUriString();

        return callAndMap(uri);
    }

    private WeatherApiResponse callAndMap(String uri) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            Double temperature = optDouble(root.at("/main/temp"));
            Integer humidity = optInt(root.at("/main/humidity"));
            String description = optText(root.at("/weather/0/description"));
            Double windSpeed = optDouble(root.at("/wind/speed"));
            String city = optText(root.at("/name"));

            WeatherApiResponse dto = new WeatherApiResponse();
            dto.setTemperature(temperature);
            dto.setHumidity(humidity);
            dto.setDescription(description);
            dto.setWindSpeed(windSpeed);
            dto.setCity(city);

            return dto;

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RuntimeException("API key is invalid or not activated yet (401).", e);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Location not found (404).", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch weather data.", e);
        }
    }

    // small helpers to avoid NPEs
    private String optText(JsonNode node) {
        return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asText();
    }

    private Double optDouble(JsonNode node) {
        return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asDouble();
    }

    private Integer optInt(JsonNode node) {
        return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asInt();
    }
}
