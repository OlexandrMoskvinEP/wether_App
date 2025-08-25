package com.wether_app.controller;

import com.wether_app.model.WeatherRecord;
import com.wether_app.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService service;

    public WeatherController(WeatherService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<WeatherRecord> getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String zip) {

        if ((city == null && zip == null) || (city != null && zip != null)) {
            return ResponseEntity.badRequest().build();
        }

        WeatherRecord rec = (city != null)
                ? service.getByCity(city)
                : service.getByZip(zip);

        return ResponseEntity.ok(rec);
    }
}
