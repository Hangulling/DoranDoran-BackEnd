package com.dorandoran.store.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 설정
 *
 * <p>Store Service의 API 문서를 자동 생성하기 위한 설정</p>
 *
 * <h3>접근 방법:</h3>
 * <ul>
 *   <li>Swagger UI: http://localhost:8084/swagger-ui.html</li>
 *   <li>API Docs: http://localhost:8084/api-docs</li>
 * </ul>
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>API 문서 자동 생성</li>
 *   <li>Try it out 기능 (API 테스트)</li>
 *   <li>스키마 정의 자동화</li>
 * </ul>
 *
 * @author DoranDoran Team
 * @version 1.0
 * @since 2025-10-11
 */
@Configuration
public class OpenApiConfig {

  /**
   * OpenAPI 문서 설정
   *
   * <p>Store Service의 API 정보, 서버 정보, 라이센스 등을 정의</p>
   *
   * @return OpenAPI 설정 객체
   */
  @Bean
  public OpenAPI storeServiceOpenAPI() {
    return new OpenAPI()
        // API 기본 정보
        .info(new Info()
            .title("DoranDoran Store Service API")  // API 제목
            .description("보관함 서비스 - 사용자가 저장한 표현 관리")  // API 설명
            .version("v1.0.0")  // API 버전

            // 연락처 정보
            .contact(new Contact()
                .name("DoranDoran Team")
                .email("support@dorandoran.com")
                .url("https://dorandoran.com"))

            // 라이센스 정보
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))

        // 서버 정보 (여러 환경)
        .servers(List.of(
            // 로컬 개발 서버
            new Server()
                .url("http://localhost:8084")
                .description("로컬 개발 환경"),

            // Docker 환경
            new Server()
                .url("http://localhost:8084")
                .description("Docker 환경"),

            // Gateway를 통한 접근
            new Server()
                .url("http://localhost:8080/store")
                .description("API Gateway 경유")
        ));
  }
}
