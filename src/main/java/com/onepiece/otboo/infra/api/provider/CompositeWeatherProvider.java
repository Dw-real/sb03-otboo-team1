package com.onepiece.otboo.infra.api.provider;

import com.onepiece.otboo.infra.api.dto.KmaItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class CompositeWeatherProvider implements WeatherProvider {

    private final KmaWeatherProvider kmaWeatherProvider;
    private final OpenWeatherProvider openWeatherProvider;

    @Override
    public List<KmaItem> fetchLatestItems(double latitude, double longitude) {

        List<KmaItem> kma = kmaWeatherProvider.fetchLatestItems(latitude, longitude);
        if (kma != null && !kma.isEmpty()) {
            log.info("[Weather] KMA로 날씨 수집 성공");
            return kma;
        }

        log.warn("[Weather] KMA 실패 → OWM fallback");
        
        List<KmaItem> owm = openWeatherProvider.fetchLatestItems(latitude, longitude);
        if (owm != null && !owm.isEmpty()) {
            log.info("[Weather] OWM으로 날씨 수집 성공");
            return owm;
        }

        log.warn("[Weather] 모든 API 실패");
        return List.of();
    }
}
