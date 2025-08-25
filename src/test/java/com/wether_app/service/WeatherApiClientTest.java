package com.wether_app.service;

import com.wether_app.config.WeatherApiProperties;
import com.wether_app.model.dto.WeatherApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherApiClientTest {
    private MockRestServiceServer mockServer;
    private WeatherApiClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        WeatherApiProperties props = new WeatherApiProperties();
        props.setKey("dummyKey");
        props.setUrl("https://api.openweathermap.org/data/2.5/weather");
        props.setUnits("metric");

        client = new WeatherApiClient(restTemplate, new ObjectMapper(), props);
    }

    @Test
    void fetchByCity_shouldMapResponse() {
        String fakeJson = """
            {
              "weather":[{"description":"clear sky"}],
              "main":{"temp":22.5,"humidity":55},
              "wind":{"speed":3.6},
              "name":"Kyiv"
            }
            """;

        String expectedUrl = "https://api.openweathermap.org/data/2.5/weather?q=Kyiv&appid=dummyKey&units=metric";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(fakeJson, MediaType.APPLICATION_JSON));

        WeatherApiResponse response = client.fetchByCity("Kyiv");

        assertThat(response.getCity()).isEqualTo("Kyiv");
        assertThat(response.getTemperature()).isEqualTo(22.5);
        assertThat(response.getHumidity()).isEqualTo(55);
        assertThat(response.getDescription()).isEqualTo("clear sky");
        assertThat(response.getWindSpeed()).isEqualTo(3.6);
    }
}
