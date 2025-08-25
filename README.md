# Weather Demo Application 🌤️

A simple Spring Boot application that integrates with the **OpenWeatherMap API** to fetch weather data by **city name** or **zip code**.  
Weather data is cached in a local **H2 database** using Hibernate/JPA, and automatically refreshed when stale.

---

## Features

- **Search weather by city or zip**
    - `GET /weather?city=Kyiv`
    - `GET /weather?zip=90210`
- **Data persistence** with Spring Data JPA + H2
- **Automatic refresh**: weather data is refreshed if older than configured TTL (default 15 minutes)
- **Layered architecture**:
    - `Controller` – REST endpoints
    - `Service` – domain logic (cache vs API)
    - `Client` – calls OpenWeatherMap
    - `Repository` – JPA persistence
- **Test coverage**:
    - Repository tests (`@DataJpaTest`)
    - API client tests (`MockRestServiceServer`)
    - Service tests (Mockito)
    - Controller tests (`@WebMvcTest`)

---

## Requirements

- Java 17+
- Maven 3.x
- OpenWeatherMap API key (free)

---

## Configuration

Set your API key in `src/main/resources/application.yml`:

```yaml
weather:
  api:
    key: YOUR_API_KEY
    url: https://api.openweathermap.org/data/2.5/weather
    units: metric

weather:
  cache:
    ttl-minutes: 15
