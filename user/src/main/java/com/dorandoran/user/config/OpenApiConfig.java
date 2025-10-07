package com.dorandoran.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 설정
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DoranDoran User Service API")
                        .description("사용자 관리 서비스 API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DoranDoran Development Team")
                                .email("dev@dorandoran.com")
                                .url("https://dorandoran.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("User Service (Direct)"),
                        new Server()
                                .url("http://localhost:8080/api/users")
                                .description("User Service (via Gateway)")
                ));
    }
}


