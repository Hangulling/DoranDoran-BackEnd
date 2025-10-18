package com.dorandoran.auth.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 클라이언트 설정
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor feignHmacRequestInterceptor() {
        return new FeignHmacInterceptor();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
