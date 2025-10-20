package com.dorandoran.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final HmacAuthInterceptor hmacAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(hmacAuthInterceptor).addPathPatterns("/**");
    }

    // CORS 설정은 Gateway에서 처리하므로 제거
    // @Override
    // public void addCorsMappings(CorsRegistry registry) {
    //     registry.addMapping("/**")
    //         .allowedOriginPatterns("*")
    //         .allowedMethods("*")
    //         .allowedHeaders("*")
    //         .allowCredentials(true);
    // }
}


