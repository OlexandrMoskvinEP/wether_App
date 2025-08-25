package com.wether_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wether_app.model.WeatherRecord;
import com.wether_app.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WeatherController.class)
class WeatherControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    WeatherService weatherService;

    @Test
    void getWeather_byCity_returns200AndBody() throws Exception {
        var rec = new WeatherRecord();
        rec.setId(1L);
        rec.setLocationKey("city:kyiv");
        rec.setCity("Kyiv");
        rec.setTemperature(22.5);
        rec.setDescription("clear sky");
        rec.setHumidity(55);
        rec.setWindSpeed(3.6);
        rec.setUpdatedAt(LocalDateTime.parse("2025-01-01T12:00:00"));

        Mockito.when(weatherService.getByCity("Kyiv")).thenReturn(rec);

        mvc.perform(get("/weather").param("city", "Kyiv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city", is("Kyiv")))
                .andExpect(jsonPath("$.temperature", is(22.5)))
                .andExpect(jsonPath("$.description", is("clear sky")))
                .andExpect(jsonPath("$.humidity", is(55)))
                .andExpect(jsonPath("$.windSpeed", is(3.6)))
                .andExpect(jsonPath("$.locationKey", is("city:kyiv")));
    }

    @Test
    void getWeather_byZip_returns200AndBody() throws Exception {
        var rec = new WeatherRecord();
        rec.setId(2L);
        rec.setLocationKey("zip:90210");
        rec.setZip("90210");
        rec.setCity("Beverly Hills");
        rec.setTemperature(25.0);
        rec.setDescription("sunny");
        rec.setHumidity(40);
        rec.setWindSpeed(2.0);
        rec.setUpdatedAt(LocalDateTime.parse("2025-01-01T12:00:00"));

        Mockito.when(weatherService.getByZip("90210")).thenReturn(rec);

        mvc.perform(get("/weather").param("zip", "90210"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.zip", is("90210")))
                .andExpect(jsonPath("$.city", is("Beverly Hills")))
                .andExpect(jsonPath("$.temperature", is(25.0)))
                .andExpect(jsonPath("$.locationKey", is("zip:90210")));
    }

    @Test
    void getWeather_missingParams_returns400() throws Exception {
        mvc.perform(get("/weather"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWeather_bothParams_returns400() throws Exception {
        mvc.perform(get("/weather").param("city", "Kyiv").param("zip", "01001"))
                .andExpect(status().isBadRequest());
    }
}