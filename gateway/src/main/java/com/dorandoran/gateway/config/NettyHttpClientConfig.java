package com.dorandoran.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.HttpProtocol;

import java.util.concurrent.TimeUnit;

/**
 * Netty HttpClient 설정 (SSE 타임아웃 해결)
 */
@Configuration
public class NettyHttpClientConfig {

    /**
     * SSE를 위한 긴 타임아웃 설정
     * read-timeout을 10분으로 설정하여 SSE 연결 유지
     * HTTP/2를 비활성화하여 HTTP/1.1 사용 (SSE 호환성)
     */
    @Bean
    public HttpClientCustomizer httpClientCustomizer() {
        return new HttpClientCustomizer() {
            @Override
            public HttpClient customize(HttpClient httpClient) {
                return httpClient
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 45000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .protocol(HttpProtocol.HTTP11) // HTTP/1.1 강제 (SSE 호환성)
                        .doOnConnected(conn -> {
                            conn.addHandlerLast(new ReadTimeoutHandler(600, TimeUnit.SECONDS)); // 10분
                            conn.addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS)); // 1분
                        });
            }
        };
    }
}

