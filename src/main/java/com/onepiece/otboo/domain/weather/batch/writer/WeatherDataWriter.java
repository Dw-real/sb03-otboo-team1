package com.onepiece.otboo.domain.weather.batch.writer;

import com.onepiece.otboo.domain.weather.dto.data.WeatherBatchResult;
import com.onepiece.otboo.domain.weather.entity.Weather;
import com.onepiece.otboo.domain.weather.entity.WeatherAlertOutbox;
import com.onepiece.otboo.domain.weather.repository.WeatherAlertOutboxRepository;
import com.onepiece.otboo.domain.weather.repository.WeatherRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@RequiredArgsConstructor
public class WeatherDataWriter implements ItemWriter<WeatherBatchResult> {

    private final WeatherRepository weatherRepository;
    private final WeatherAlertOutboxRepository outboxRepository;

    // 대량 저장 시 한 번에 너무 많이 flush하지 않도록 분할 크기(상황에 따라 조절)
    private static final int BATCH_SIZE = 1000;

    @Override
    public void write(Chunk<? extends WeatherBatchResult> chunk) {
        // 1) 평탄화
        List<Weather> weatherFlat = new ArrayList<>();
        List<WeatherAlertOutbox> outboxFlat = new ArrayList<>();

        for (WeatherBatchResult r : chunk.getItems()) {
            if (r == null) {
                continue;
            }
            if (r.weathers() != null) {
                weatherFlat.addAll(r.weathers());
            }
            if (r.outboxes() != null) {
                outboxFlat.addAll(r.outboxes());
            }
        }

        // 2) Weather 저장
        saveWeathers(weatherFlat);

        // 3) Outbox 저장
        saveOutboxes(outboxFlat);
    }

    private void saveWeathers(List<Weather> flat) {
        List<Weather> filtered = flat.stream()
            .filter(Objects::nonNull)
            .toList();

        if (filtered.isEmpty()) {
            log.warn("[WeatherDataWriter] 날씨 데이터가 비어있습니다.");
            return;
        }

        // (키: locationId + forecastAt) 중복 제거
        Map<String, Weather> dedup = new LinkedHashMap<>();
        for (Weather w : filtered) {
            UUID locationId = (w.getLocation() != null) ? w.getLocation().getId() : null;
            Instant forecastAt = w.getForecastAt();
            if (locationId == null || forecastAt == null) {
                log.debug("[WeatherDataWriter] 키 정보 누락으로 스킵: {}", w);
                continue;
            }
            String key = locationId + "|" + forecastAt;
            dedup.putIfAbsent(key, w); // 최초 유지
        }

        List<Weather> toSave = new ArrayList<>(dedup.values());
        if (toSave.isEmpty()) {
            log.warn("[WeatherDataWriter] 중복 제거 후 저장할 날씨 데이터가 없습니다.");
            return;
        }

        try {
            for (int i = 0; i < toSave.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, toSave.size());
                List<Weather> slice = toSave.subList(i, end);

                weatherRepository.saveAll(slice);
                log.info("[WeatherDataWriter] 날씨 데이터 {}개 저장 완료 ({}~{})",
                    slice.size(), i, end - 1);
            }
        } catch (Exception e) {
            log.error("[WeatherDataWriter] 날씨 데이터 저장 실패", e);
            throw e;
        }
    }

    private void saveOutboxes(List<WeatherAlertOutbox> flat) {
        List<WeatherAlertOutbox> filtered = flat.stream()
            .filter(Objects::nonNull)
            .toList();

        if (filtered.isEmpty()) {
            log.info("[WeatherDataWriter] outbox 데이터가 비어있습니다.");
            return;
        }

        // unique 제약이 (locationId, userId, type, date) 같은 조합이라면 아래처럼 키로 dedup 가능
        Map<String, WeatherAlertOutbox> dedup = new LinkedHashMap<>();
        for (WeatherAlertOutbox outbox : filtered) {
            UUID locationId = outbox.getLocationId();
            UUID userId = outbox.getUserId();
            String type = String.valueOf(outbox.getType());
            LocalDate date = outbox.getAlertDate();
            if (locationId == null || userId == null || type == null || date == null) {
                log.debug("[WeatherDataWriter] outbox 키 정보 누락으로 스킵: {}", outbox);
                continue;
            }
            String key = locationId + "|" + userId + "|" + type + "|" + date;
            dedup.putIfAbsent(key, outbox);
        }

        List<WeatherAlertOutbox> toSave = new ArrayList<>(dedup.values());
        if (toSave.isEmpty()) {
            log.info("[WeatherDataWriter] 중복 제거 후 저장할 outbox가 없습니다.");
            return;
        }

        try {
            for (int i = 0; i < toSave.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, toSave.size());
                List<WeatherAlertOutbox> slice = toSave.subList(i, end);

                outboxRepository.saveAll(slice);
                log.info("[WeatherDataWriter] outbox {}개 저장 완료 ({}~{})",
                    slice.size(), i, end - 1);
            }
        } catch (DataIntegrityViolationException e) {
            // 유니크 인덱스(중복 알림)
            log.info("[WeatherDataWriter] 중복 알림 발생으로 일부 outbox 저장 스킵. reason={}",
                e.getMostSpecificCause().getMessage());
        } catch (Exception e) {
            log.error("[WeatherDataWriter] outbox 저장 실패", e);
            throw e;
        }
    }
}
