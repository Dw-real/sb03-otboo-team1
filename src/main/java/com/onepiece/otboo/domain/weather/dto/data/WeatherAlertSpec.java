package com.onepiece.otboo.domain.weather.dto.data;

import com.onepiece.otboo.domain.weather.enums.WeatherChangeType;
import java.time.LocalDate;
import java.util.UUID;

public record WeatherAlertSpec(
    UUID locationId,
    LocalDate date,
    WeatherChangeType type,
    String title,
    String message
) {

}
