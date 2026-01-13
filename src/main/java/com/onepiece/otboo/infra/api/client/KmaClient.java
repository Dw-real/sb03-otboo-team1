package com.onepiece.otboo.infra.api.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepiece.otboo.infra.api.dto.KmaItem;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KmaClient {

    private final WebClient kmaApiClient;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int NUM_ROWS = 1000;

    @Cacheable(
        cacheNames = "kma:village",
        key = "T(java.lang.String).format('%d:%d:%s:%s', #nx, #ny, #baseDate.format(T(java.time.format.DateTimeFormatter).BASIC_ISO_DATE), #baseTime)",
        sync = true
    )
    public List<KmaItem> getVillageForecast(int nx, int ny, LocalDate baseDate, String baseTime) {
        try {
            String json = kmaApiClient.get()
                .uri(u -> u.path("/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst")
                    .queryParam("numOfRows", NUM_ROWS)
                    .queryParam("pageNo", 1)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .queryParam("base_date", baseDate.format(DATE))
                    .queryParam("base_time", baseTime)
                    .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                    res.bodyToMono(String.class).flatMap(b ->
                        Mono.error(
                            new RuntimeException("KMA HTTP " + res.statusCode() + " body=" + b))
                    )
                )
                .bodyToMono(String.class)
                .block();

            Root root = objectMapper.readValue(json, Root.class);
            if (root == null || root.response == null || root.response.header == null) {
                log.warn("KMA empty/invalid JSON nx={},ny={},base={}{}",
                    nx, ny, baseDate.format(DATE), baseTime);
                return List.of();
            }
            String code = root.response.header.resultCode;
            String msg = root.response.header.resultMsg;
            if (!"00".equals(code)) {
                log.warn("KMA resultCode={} msg={} nx={},ny={},base={}{}",
                    code, msg, nx, ny, baseDate.format(DATE), baseTime);
                return List.of();
            }

            List<KmaItem> items = Optional.ofNullable(root.response.body)
                .map(b -> b.items)
                .map(i -> i.item)
                .orElse(List.of());

            String baseStr = baseDate.format(DATE);
            List<KmaItem> out = new ArrayList<>(items.size());
            for (KmaItem it : items) {
                out.add(it.withBase(baseStr, baseTime));
            }
            return out;
        } catch (Exception e) {
            log.error("KMA call error nx={}, ny={}, base={}{}", nx, ny, baseDate.format(DATE),
                baseTime, e);
            return List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Root {

        public Resp response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Resp {

        public Header header;
        public Body body;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Header {

        public String resultCode;
        public String resultMsg;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Body {

        public Items items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Items {

        public List<KmaItem> item;
    }
}
