package com.onepiece.otboo.domain.weather.batch.processor;

import com.onepiece.otboo.domain.location.entity.Location;
import com.onepiece.otboo.domain.weather.dto.data.WeatherAlertSpec;
import com.onepiece.otboo.domain.weather.entity.Weather;
import com.onepiece.otboo.domain.weather.enums.WeatherChangeType;
import com.onepiece.otboo.domain.weather.enums.WindSpeedWord;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WeatherAlertRuleEngine {

    private static final double TEMP_CRITICAL = 5.0;
    private static final double RAIN_CRITICAL = 5.0;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public List<WeatherAlertSpec> evaluate(Location location, List<Weather> weathers) {

        if (weathers == null || weathers.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, List<Weather>> byDate = weathers.stream()
            .collect(Collectors.groupingBy(
                w -> w.getForecastedAt().atZone(KST).toLocalDate(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<WeatherAlertSpec> result = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Weather>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Weather> dayWeathers = entry.getValue();

            boolean tempChangeDetected = dayWeathers.stream()
                .anyMatch(w -> Math.abs(w.getTemperatureComparedToDayBefore()) >= TEMP_CRITICAL);

            boolean strongWindDetected = dayWeathers.stream()
                .anyMatch(w -> w.getWindSpeedWord() == WindSpeedWord.STRONG);

            boolean rainDetected = dayWeathers.stream()
                .anyMatch(w -> w.getPrecipitationAmount() >= RAIN_CRITICAL);

            if (tempChangeDetected) {
                result.add(new WeatherAlertSpec(
                    location.getId(),
                    date,
                    WeatherChangeType.TEMPERATURE_CHANGE,
                    WeatherChangeType.TEMPERATURE_CHANGE.getTitle(),
                    String.format("%s 기온 변화가 %.1f℃ 이상입니다.", date, TEMP_CRITICAL)
                ));
            }
            if (strongWindDetected) {
                result.add(new WeatherAlertSpec(
                    location.getId(),
                    date,
                    WeatherChangeType.WIND_CHANGE,
                    WeatherChangeType.WIND_CHANGE.getTitle(),
                    String.format("%s에 강풍 예보가 있습니다. 바람막이나 외투를 챙기세요~!", date)
                ));
            }
            if (rainDetected) {
                result.add(new WeatherAlertSpec(
                    location.getId(),
                    date,
                    WeatherChangeType.PRECIPITATION_CHANGE,
                    WeatherChangeType.PRECIPITATION_CHANGE.getTitle(),
                    String.format("%s에 강수 소식이 있습니다. 우산을 챙기세요~!", date)
                ));
            }
        }

        return result;
    }
}
