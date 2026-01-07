package com.onepiece.otboo.domain.weather.dto.data;

import com.onepiece.otboo.domain.weather.entity.Weather;
import com.onepiece.otboo.domain.weather.entity.WeatherAlertOutbox;
import java.util.List;

public record WeatherBatchResult(
    List<Weather> weathers,
    List<WeatherAlertOutbox> outboxes
) {

    public static WeatherBatchResult empty() {
        return new WeatherBatchResult(List.of(), List.of());
    }
}