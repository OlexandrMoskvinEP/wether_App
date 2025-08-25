package com.weather_app.repository;

import com.weather_app.model.WeatherRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WeatherRecordRepositoryTest {

    @Autowired
    private WeatherRecordRepository repository;

    @Test
    void saveAndFindByLocationKey_shouldWork() {
        WeatherRecord rec = new WeatherRecord();
        rec.setLocationKey("city:kyiv");
        rec.setCity("Kyiv");
        rec.setTemperature(20.5);
        rec.setDescription("clear sky");
        rec.setHumidity(60);
        rec.setWindSpeed(3.4);
        rec.setUpdatedAt(LocalDateTime.now());

        repository.save(rec);

        var found = repository.findByLocationKey("city:kyiv");

        assertThat(found).isPresent();
        assertThat(found.get().getCity()).isEqualTo("Kyiv");
        assertThat(found.get().getTemperature()).isEqualTo(20.5);
    }

    @Test
    void findByCity_shouldReturnRecord() {
        WeatherRecord rec = new WeatherRecord();
        rec.setLocationKey("city:london");
        rec.setCity("London");
        rec.setTemperature(15.0);
        rec.setUpdatedAt(LocalDateTime.now());

        repository.save(rec);

        var found = repository.findByCity("London");

        assertThat(found).isPresent();
        assertThat(found.get().getLocationKey()).isEqualTo("city:london");
    }
}