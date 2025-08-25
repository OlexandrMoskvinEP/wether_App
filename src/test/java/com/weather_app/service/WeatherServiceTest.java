package com.weather_app.service;

import com.weather_app.model.WeatherRecord;
import com.weather_app.model.dto.WeatherApiResponse;
import com.weather_app.repository.WeatherRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    WeatherRecordRepository repository;
    @Mock
    WeatherApiClient apiClient;

    WeatherService service;

    // fixed clock for reproducible tests
    private final Clock fixedClock =
            Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        // TTL = 15 minutes (matches default in service constructor)
        service = new WeatherService(repository, apiClient, 15, fixedClock);
    }

    @Test
    void getByCity_returnsExisting_ifNotStale_doesNotCallApi() {
        var existing = new WeatherRecord();
        existing.setId(1L);
        existing.setLocationKey("city:kyiv");
        existing.setCity("Kyiv");
        existing.setTemperature(10.0);
        existing.setUpdatedAt(LocalDateTime.ofInstant(Instant.parse("2025-01-01T11:50:00Z"), ZoneOffset.UTC)); // 10 min ago

        when(repository.findByLocationKey("city:kyiv")).thenReturn(Optional.of(existing));

        var result = service.getByCity("  Kyiv  ");

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(apiClient);
        verify(repository, never()).save(any());
    }

    @Test
    void getByCity_refreshes_ifStale_andSavesUpdated() {
        var stale = new WeatherRecord();
        stale.setId(1L);
        stale.setLocationKey("city:london");
        stale.setCity("London");
        stale.setTemperature(5.0);
        stale.setUpdatedAt(LocalDateTime.ofInstant(Instant.parse("2025-01-01T11:00:00Z"), ZoneOffset.UTC)); // 1h ago -> stale

        when(repository.findByLocationKey("city:london")).thenReturn(Optional.of(stale));

        var api = new WeatherApiResponse();
        api.setCity("London");
        api.setTemperature(6.5);
        api.setHumidity(70);
        api.setDescription("overcast");
        api.setWindSpeed(3.2);

        when(apiClient.fetchByCity("london")).thenReturn(api);
        when(repository.save(any(WeatherRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.getByCity("London");

        assertThat(result.getTemperature()).isEqualTo(6.5);
        assertThat(result.getDescription()).isEqualTo("overcast");
        assertThat(result.getUpdatedAt()).isEqualTo(LocalDateTime.ofInstant(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC));
        verify(apiClient).fetchByCity("london");
        verify(repository).save(any(WeatherRecord.class));
    }

    @Test
    void getByCity_missing_createsNewFromApi_andSaves() {
        when(repository.findByLocationKey("city:kyiv")).thenReturn(Optional.empty());

        var api = new WeatherApiResponse();
        api.setCity("Kyiv");
        api.setTemperature(21.2);
        api.setHumidity(50);
        api.setDescription("clear sky");
        api.setWindSpeed(2.1);

        when(apiClient.fetchByCity("kyiv")).thenReturn(api);
        when(repository.save(any(WeatherRecord.class))).thenAnswer(inv -> {
            WeatherRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        var result = service.getByCity("Kyiv");

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getLocationKey()).isEqualTo("city:kyiv");
        assertThat(result.getCity()).isEqualTo("Kyiv");
        assertThat(result.getTemperature()).isEqualTo(21.2);
        verify(apiClient).fetchByCity("kyiv");
        verify(repository).save(any(WeatherRecord.class));
    }

    @Test
    void getByCity_prefersProviderName_ifPresent_elseUsesNormalized() {
        when(repository.findByLocationKey("city:new york")).thenReturn(Optional.empty());

        var api = new WeatherApiResponse();
        api.setCity(null); // provider didn’t return name
        api.setTemperature(18.0);
        when(apiClient.fetchByCity("new york")).thenReturn(api);
        when(repository.save(any(WeatherRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.getByCity("  New York ");

        assertThat(result.getCity()).isEqualTo("new york"); // falls back to normalized
    }

    @Test
    void getByZip_returnsExisting_ifNotStale_doesNotCallApi() {
        var existing = new WeatherRecord();
        existing.setLocationKey("zip:10001");
        existing.setZip("10001");
        existing.setUpdatedAt(LocalDateTime.ofInstant(Instant.parse("2025-01-01T11:50:00Z"), ZoneOffset.UTC));

        when(repository.findByLocationKey("zip:10001")).thenReturn(Optional.of(existing));

        var result = service.getByZip("10001");

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(apiClient);
        verify(repository, never()).save(any());
    }

    @Test
    void getByZip_missing_callsApi_andSaves() {
        when(repository.findByLocationKey("zip:90210")).thenReturn(Optional.empty());

        var api = new WeatherApiResponse();
        api.setCity("Beverly Hills");
        api.setTemperature(25.0);
        when(apiClient.fetchByZip("90210")).thenReturn(api);
        when(repository.save(any(WeatherRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.getByZip("  90210 ");

        assertThat(result.getLocationKey()).isEqualTo("zip:90210");
        assertThat(result.getZip()).isEqualTo("90210");
        assertThat(result.getCity()).isEqualTo("Beverly Hills");
        assertThat(result.getTemperature()).isEqualTo(25.0);
        verify(apiClient).fetchByZip("90210");
        verify(repository).save(any(WeatherRecord.class));
    }
}