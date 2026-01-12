package com.onepiece.otboo.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public HttpClient commonHttpClient() {
        return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 4000) // TCP 연결 타임아웃 4초
            .responseTimeout(Duration.ofSeconds(5))  // 서버 응답 타임아웃 5초
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS))
            );
    }

    @Bean
    public WebClient kakaoApiClient(
        @Value("${api.kakao.rest-api-key}") String key,
        HttpClient commonHttpClient
    ) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(commonHttpClient))
            .baseUrl("https://dapi.kakao.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + key)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Bean
    public WebClient kmaApiClient(
        @Value("${api.weather.kma-api-key}") String key,
        HttpClient commonHttpClient
    ) {
        DefaultUriBuilderFactory builder =
            new DefaultUriBuilderFactory(
                "https://apihub.kma.go.kr");
        builder.setEncodingMode(EncodingMode.VALUES_ONLY);

        // 모든 요청에 serviceKey, dataType 쿼리 파라미터 자동 추가
        ExchangeFilterFunction appendFixedParams =
            ExchangeFilterFunction.ofRequestProcessor(req -> {
                URI newUri = UriComponentsBuilder.fromUri(req.url())
                    .replaceQueryParam("authKey")
                    .replaceQueryParam("dataType")
                    .queryParam("authKey", key)
                    .queryParam("dataType", "JSON")
                    .build(false)
                    .toUri();
                ClientRequest newReq = ClientRequest.from(req).url(newUri).build();
                return Mono.just(newReq);
            });

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(commonHttpClient))
            .uriBuilderFactory(builder)
            .baseUrl("https://apihub.kma.go.kr")
            .filter(appendFixedParams)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Bean
    public WebClient openWeatherApiClient(
        @Value("${api.weather.open-weather-api-key}") String key,
        HttpClient commonHttpClient
    ) {
        DefaultUriBuilderFactory builder =
            new DefaultUriBuilderFactory(
                "api.openweathermap.org");
        builder.setEncodingMode(EncodingMode.VALUES_ONLY);

        ExchangeFilterFunction appendFixedParams =
            ExchangeFilterFunction.ofRequestProcessor(req -> {
                URI newUri = UriComponentsBuilder.fromUri(req.url())
                    .replaceQueryParam("appid")
                    .queryParam("appid", key)
                    .queryParam("lang", "kr")
                    .queryParam("units", "metric")
                    .build(false)
                    .toUri();
                ClientRequest newReq = ClientRequest.from(req).url(newUri).build();
                return Mono.just(newReq);
            });

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(commonHttpClient))
            .uriBuilderFactory(builder)
            .baseUrl("api.openweathermap.org/data/2.5/forecast")
            .filter(appendFixedParams)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
