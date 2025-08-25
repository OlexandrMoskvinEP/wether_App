package com.wether_app.model.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@RequiredArgsConstructor
@ToString
public class WeatherApiResponse {
    private Double temperature;
    private Integer humidity;
    private String description;
    private Double windSpeed;
    private String city;
}
