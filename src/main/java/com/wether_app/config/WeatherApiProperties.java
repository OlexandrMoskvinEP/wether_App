package com.wether_app.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "weather.api")
@RequiredArgsConstructor
@Data
public class WeatherApiProperties {
    private String key;
    private String url;
    private String units = "metric";
}
