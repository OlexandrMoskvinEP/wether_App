package com.wether_app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wether_app.config.WeatherApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherApiClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private WeatherApiClient client;
    private WeatherApiProperties props;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        props = new WeatherApiProperties();
        props.setKey("dummyKey");
        props.setUrl("https://api.openweathermap.org/data/2.5/weather");
        props.setUnits("metric");

        client = new WeatherApiClient(restTemplate, new ObjectMapper(), props);
    }

    @Test
    void fetchByCity_ok_mapsFields() {
        String json = """
        {
          "weather":[{"description":"clear sky"}],
          "main":{"temp":22.5,"humidity":55},
          "wind":{"speed":3.6},
          "name":"Kyiv"
        }
        """;

        mockServer.expect(once(), requestTo(startsWith(props.getUrl())))
                .andExpect(queryParam("q", "Kyiv"))
                .andExpect(queryParam("appid", props.getKey()))
                .andExpect(queryParam("units", props.getUnits()))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        var dto = client.fetchByCity("Kyiv");

        assertThat(dto.getCity()).isEqualTo("Kyiv");
        assertThat(dto.getTemperature()).isEqualTo(22.5);
        assertThat(dto.getHumidity()).isEqualTo(55);
        assertThat(dto.getDescription()).isEqualTo("clear sky");
        assertThat(dto.getWindSpeed()).isEqualTo(3.6);

        mockServer.verify();
    }

    @Test
    void fetchByZip_ok_mapsFields() {
        String json = """
                {
                  "weather":[{"description":"overcast clouds"}],
                  "main":{"temp":15.0,"humidity":70},
                  "wind":{"speed":5.1},
                  "name":"London"
                }
                """;

        mockServer.expect(once(), requestTo(startsWith(props.getUrl())))
                .andExpect(queryParam("zip", "90210"))
                .andExpect(queryParam("appid", props.getKey()))
                .andExpect(queryParam("units", props.getUnits()))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        var dto = client.fetchByZip("90210");

        assertThat(dto.getCity()).isEqualTo("London");
        assertThat(dto.getTemperature()).isEqualTo(15.0);
        assertThat(dto.getHumidity()).isEqualTo(70);
        assertThat(dto.getDescription()).isEqualTo("overcast clouds");
        assertThat(dto.getWindSpeed()).isEqualTo(5.1);

        mockServer.verify();
    }

    @Test
    void fetchByCity_401_wrapsAsRuntimeExceptionWithMessage() {
        mockServer.expect(once(), requestTo(startsWith(props.getUrl())))
                .andExpect(queryParam("q", "Kyiv"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.fetchByCity("Kyiv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API key is invalid or not activated yet (401)");

        mockServer.verify();
    }

    @Test
    void fetchByCity_404_wrapsAsRuntimeExceptionWithMessage() {
        mockServer.expect(once(), requestTo(startsWith(props.getUrl())))
                .andExpect(queryParam("q", "NoSuchCity"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.fetchByCity("NoSuchCity"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Location not found (404)");

        mockServer.verify();
    }

    @Test
    void fetchByCity_malformedJson_throwsRuntimeException() {
        String broken = "{ \"main\": { \"temp\": \"NaN\" "; // intentionally invalid JSON

        mockServer.expect(once(), requestTo(startsWith(props.getUrl())))
                .andExpect(queryParam("q", "Kyiv"))
                .andRespond(withSuccess(broken, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchByCity("Kyiv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch weather data");

        mockServer.verify();
    }
}
