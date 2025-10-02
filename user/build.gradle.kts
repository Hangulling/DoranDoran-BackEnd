plugins { 
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("java")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":common"))
    implementation(project(":infra:infra-persistence"))
    
    // Prometheus 메트릭
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // 데이터베이스
    runtimeOnly("org.postgresql:postgresql:42.7.3")
    
    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2")
    
    // Lombok은 common에서 제공, annotationProcessor만 필요
    annotationProcessor("org.projectlombok:lombok")
}
