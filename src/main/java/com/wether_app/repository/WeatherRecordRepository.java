package com.wether_app.repository;

import com.wether_app.model.WeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeatherRecordRepository extends JpaRepository<WeatherRecord, Integer> {
    Optional<WeatherRecord> findByLocationKey(String locationKey);

    Optional<WeatherRecord> findByCity(String city);

    Optional<WeatherRecord> findByZip(String zip);
}
