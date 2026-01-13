package com.onepiece.otboo.infra.api.provider;

import com.onepiece.otboo.global.util.ArrayUtil;
import com.onepiece.otboo.infra.api.client.KmaClient;
import com.onepiece.otboo.infra.api.dto.BaseDt;
import com.onepiece.otboo.infra.api.dto.KmaItem;
import com.onepiece.otboo.infra.converter.LatLonXYConverter;
import com.onepiece.otboo.infra.converter.LatLonXYConverter.Point;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.bulkhead.annotation.Bulkhead.Type;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KmaWeatherProvider implements WeatherProvider {

    private final KmaClient kmaClient;
    private final CacheManager cacheManager;
    private final Clock clock;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String[] BASE_TIMES = {"2300", "2000", "1700"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    @Override
    @Retry(name = "kma")
    @CircuitBreaker(name = "kma", fallbackMethod = "fallback")
    @Bulkhead(name = "kma", type = Type.SEMAPHORE)
    public List<KmaItem> fetchLatestItems(double latitude, double longitude) {
        BaseDt latestBase = resolveLatestBaseToday(); // 오늘 기준으로 잡기
        List<KmaItem> acc = new ArrayList<>();

        int startIdx = ArrayUtil.indexOf(BASE_TIMES, latestBase.time());
        if (startIdx < 0) {
            // latestBase가 허용목록에 없다면, 가장 가까운 이전 허용 base를 선택
            startIdx = nearestAllowedIndex(latestBase.time());
        }
        Point point = LatLonXYConverter.latLonToXY(latitude, longitude);
        int x = point.x();
        int y = point.y();

        for (int i = startIdx; i < BASE_TIMES.length; i++) {
            BaseDt tb = new BaseDt(latestBase.date(), BASE_TIMES[i]);
            List<KmaItem> items = callVillageOnce(x, y, tb.date(), tb.time());
            if (!items.isEmpty()) {
                acc.addAll(items);
                break;
            }
        }
        if (acc.isEmpty()) {
            log.warn("[KMA] no data for today base nx={}, ny={}", x, y);
            return List.of();
        }

        // 2) 어제 fcstDate가 없으면, 어제 base도 1~2개 보완 조회
        LocalDate today = ZonedDateTime.now(clock).withZoneSameInstant(KST).toLocalDate();
        boolean hasYesterday = acc.stream().anyMatch(it ->
            LocalDate.parse(it.fcstDate(), DATE).isEqual(today.minusDays(1))
        );
        if (!hasYesterday) {
            LocalDate prevDate = today.minusDays(1);
            int[] prevCandidates = {2300, 2000}; // 필요시 1700까지
            for (int bt : prevCandidates) {
                String baseTime = String.format("%04d", bt);
                List<KmaItem> prev = callVillageOnce(x, y, prevDate, baseTime);
                if (!prev.isEmpty()) {
                    acc.addAll(prev);
                    break;
                }
            }
        }

        // 3) 중복 제거: 같은 (category|fcstDate|fcstTime)은 base 최신(=baseDate+baseTime 큰 것)으로
        Map<String, KmaItem> dedup = getStringKmaItemMap(acc);
        log.info("[KMA] 날씨 API 호출");

        return new ArrayList<>(dedup.values());
    }

    private int nearestAllowedIndex(String time) {
        // time은 "HHmm". time 이하 중 가장 큰 허용 base 반환 (없으면 0)
        for (int i = 0; i < BASE_TIMES.length; i++) {
            if (time.compareTo(BASE_TIMES[i]) == 0) {
                return i;
            }
            if (time.compareTo(BASE_TIMES[i]) > 0) {
                return i; // 23/20/17 내에서 가장 가까운 과거
            }
        }
        return BASE_TIMES.length - 1; // 기본값: 1700
    }

    private Map<String, KmaItem> getStringKmaItemMap(List<KmaItem> acc) {
        Map<String, KmaItem> dedup = new LinkedHashMap<>();
        for (KmaItem it : acc) {
            String key = it.category() + "|" + it.fcstDate() + "|" + it.fcstTime();
            KmaItem ex = dedup.get(key);
            if (ex == null) {
                dedup.put(key, it);
            } else {
                String oldBase = ex.baseDate() + ex.baseTime();
                String newBase = it.baseDate() + it.baseTime();
                if (oldBase.compareTo(newBase) < 0) {
                    dedup.put(key, it);
                }
            }
        }
        return dedup;
    }

    // 오늘 기준으로 베이스 선택
    private BaseDt resolveLatestBaseToday() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(KST);
        LocalDate d = now.toLocalDate();
        LocalTime t = now.toLocalTime();
        for (String bt : BASE_TIMES) {
            LocalTime cand = LocalTime.of(Integer.parseInt(bt) / 100, 0);
            if (!t.isBefore(cand)) {
                return new BaseDt(d, bt);
            }
        }
        return new BaseDt(d.minusDays(1), "2300");
    }


    private List<KmaItem> callVillageOnce(int nx, int ny, LocalDate baseDate, String baseTime) {
        List<KmaItem> items = kmaClient.getVillageForecast(nx, ny, baseDate, baseTime);

        if (items == null || items.isEmpty()) {
            Cache cache = cacheManager.getCache("kma:village");
            if (cache != null) {
                String key = cacheKey(nx, ny, baseDate, baseTime);
                cache.evictIfPresent(key);
                log.debug("[KMA] evicted empty cache entry key={}, cache='kma:village'", key);
            }
        }
        return items;
    }

    private static String cacheKey(int nx, int ny, LocalDate baseDate, String baseTime) {
        return String.format("%d:%d:%s:%s", nx, ny, baseDate.format(DATE), baseTime);
    }

    private List<KmaItem> fallback(double latitude, double longitude, Throwable ex) {
        log.warn("[KMA] fallback 호출 lat={}, lon={}, reason={}", latitude, longitude, ex.toString());
        return List.of();
    }
}