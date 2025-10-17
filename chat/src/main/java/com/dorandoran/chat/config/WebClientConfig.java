package com.dorandoran.chat.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 전역 설정
 * OpenAI API 호출을 위한 타임아웃 및 연결 설정
 */
@Configuration
public class WebClientConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10초 연결 타임아웃
            .responseTimeout(Duration.ofSeconds(60)) // 60초 응답 타임아웃
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS))
            );
    }

    @Bean
    public WebClient.Builder webClientBuilder(HttpClient httpClient) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }
}
