package com.onepiece.otboo.domain.weather.batch.processor;

import com.onepiece.otboo.domain.location.entity.Location;
import com.onepiece.otboo.domain.profile.entity.Profile;
import com.onepiece.otboo.domain.profile.repository.ProfileRepository;
import com.onepiece.otboo.domain.weather.dto.data.WeatherAlertSpec;
import com.onepiece.otboo.domain.weather.dto.data.WeatherBatchResult;
import com.onepiece.otboo.domain.weather.entity.Weather;
import com.onepiece.otboo.domain.weather.entity.WeatherAlertOutbox;
import com.onepiece.otboo.domain.weather.support.Extremes;
import com.onepiece.otboo.domain.weather.support.ForecastGrouping;
import com.onepiece.otboo.domain.weather.support.ForecastKey;
import com.onepiece.otboo.domain.weather.support.Indices;
import com.onepiece.otboo.domain.weather.support.WeatherFactory;
import com.onepiece.otboo.infra.api.dto.KmaItem;
import com.onepiece.otboo.infra.api.provider.WeatherProvider;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class Weather5DayProcessor implements ItemProcessor<Location, WeatherBatchResult> {

    private final WeatherProvider weatherProvider;
    private final ProfileRepository profileRepository;
    private final WeatherAlertRuleEngine ruleEngine;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public WeatherBatchResult process(Location location) {
        List<KmaItem> items = weatherProvider.fetchLatestItems(
            location.getLatitude(), location.getLongitude()
        );

        if (items.isEmpty()) {
            log.warn("해당 지역에 대한 날씨 데이터 없음 - id: {}", location.getId());
            return WeatherBatchResult.empty();
        }

        LocalDate today = LocalDate.now(KST);
        LocalDate end = today.plusDays(4);

        Extremes extremes = ForecastGrouping.collectDailyExtremes(items, today, end);
        Map<ForecastKey, Map<String, KmaItem>> bucket = ForecastGrouping.groupByForecastKey(items,
            today, end);
        Indices indices = ForecastGrouping.buildIndices(bucket);

        List<Weather> result = bucket.entrySet().stream()
            .map(e -> WeatherFactory.buildWeather(
                e.getKey(), e.getValue(), extremes, indices, location, today, end
            ))
            .flatMap(Optional::stream)
            .toList();

        List<WeatherAlertSpec> specs = ruleEngine.evaluate(location, result);
        if (specs.isEmpty()) {
            log.info("알림 스펙 없음 - locationId: {}", location.getId());
            return new WeatherBatchResult(result, List.of());
        }

        List<Profile> profiles = profileRepository.findAllByLocationId(location.getId());
        if (profiles.isEmpty()) {
            log.info("구독 프로필 없음 - locationId: {}", location.getId());
            return new WeatherBatchResult(result, List.of());
        }

        List<WeatherAlertOutbox> outboxes = new ArrayList<>();
        for (WeatherAlertSpec spec : specs) {
            for (Profile p : profiles) {
                UUID userId = p.getUser().getId();
                outboxes.add(WeatherAlertOutbox.create(
                    spec.locationId(),
                    userId,
                    spec.type(),
                    spec.date(),
                    spec.title(),
                    spec.message()
                ));
            }
        }

        log.info("날씨 생성 {}개, outbox 생성 {}개 - locationId: {}",
            result.size(), outboxes.size(), location.getId());

        return new WeatherBatchResult(result, outboxes);
    }
}
