package com.wether_app.service;

import com.wether_app.model.WeatherRecord;
import com.wether_app.model.dto.WeatherApiResponse;
import com.wether_app.repository.WeatherRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class WeatherService {
    private final WeatherRecordRepository repository;
    private final WeatherApiClient apiClient;
    private final Duration ttl;
    private final Clock clock;

    public WeatherService(WeatherRecordRepository repository,
                          WeatherApiClient apiClient,
                          @Value("${weather.cache.ttl-minutes:15}") long ttlMinutes,
                          Clock clock) {
        this.repository = repository;
        this.apiClient = apiClient;
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.clock = clock;
    }

    @Autowired
    public WeatherService(WeatherRecordRepository repository,
                          WeatherApiClient apiClient,
                          @Value("${weather.cache.ttl-minutes:15}") long ttlMinutes) {
        this(repository, apiClient, ttlMinutes, Clock.systemUTC());
    }

    @Transactional
    public WeatherRecord getByCity(String cityRaw) {
        String normCity = normalizeCity(cityRaw);
        String key = "city:" + normCity;

        var existing = repository.findByLocationKey(key);
        if (existing.isPresent() && !isStale(existing.get())) {
            return existing.get();
        }
        return refreshFromApiAndSave(key, normCity, null);
    }

    @Transactional
    public WeatherRecord getByZip(String zipRaw) {
        String normZip = normalizeZip(zipRaw);
        String key = "zip:" + normZip;

        var existing = repository.findByLocationKey(key);
        if (existing.isPresent() && !isStale(existing.get())) {
            return existing.get();
        }
        return refreshFromApiAndSave(key, null, normZip);
    }
    // ----- helpers -----

    private WeatherRecord refreshFromApiAndSave(String locationKey, String normCity, String normZip) {
        WeatherApiResponse api = (normCity != null)
                ? apiClient.fetchByCity(normCity)
                : apiClient.fetchByZip(normZip);

        WeatherRecord rec = repository.findByLocationKey(locationKey)
                .orElseGet(WeatherRecord::new);

        rec.setLocationKey(locationKey);
        // Prefer provider's proper city name if present
        rec.setCity(api.getCity() != null ? api.getCity() : normCity);
        rec.setZip(normZip);
        rec.setTemperature(api.getTemperature());
        rec.setDescription(api.getDescription());
        rec.setHumidity(api.getHumidity());
        rec.setWindSpeed(api.getWindSpeed());
        rec.setUpdatedAt(LocalDateTime.now(clock));

        return repository.save(rec);
    }

    private boolean isStale(WeatherRecord r) {
        if (r.getUpdatedAt() == null) return true;
        LocalDateTime now = LocalDateTime.now(clock);
        return r.getUpdatedAt().plus(ttl).isBefore(now);
    }

    private String normalizeCity(String city) {
        return city == null ? null : city.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeZip(String zip) {
        return zip == null ? null : zip.trim(); // keep case as-is for zips
    }
}
