package com.dorandoran.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API Gateway 라우팅 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class GatewayRoutingTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void 라우팅_설정_검증() {
        // When
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        // Then
        assertNotNull(routes);
        assertFalse(routes.isEmpty());
        
        System.out.println("=== API Gateway 라우팅 설정 ===");
        for (Route route : routes) {
            System.out.println("Route ID: " + route.getId());
            System.out.println("URI: " + route.getUri());
            System.out.println("Predicates: " + route.getPredicate());
            System.out.println("Filters: " + route.getFilters());
            System.out.println("---");
        }
    }

    @Test
    void Auth_Service_라우팅_설정_확인() {
        // When
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        // Then
        assertNotNull(routes);
        
        boolean authRouteFound = routes.stream()
                .anyMatch(route -> route.getId().equals("auth-service"));
        
        assertTrue(authRouteFound, "Auth Service 라우팅이 설정되어야 함");
        
        Route authRoute = routes.stream()
                .filter(route -> route.getId().equals("auth-service"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(authRoute);
        assertTrue(authRoute.getUri().toString().contains("8081"));
        
        System.out.println("Auth Service 라우팅: " + authRoute.getUri());
    }

    @Test
    void User_Service_라우팅_설정_확인() {
        // When
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        // Then
        assertNotNull(routes);
        
        boolean userRouteFound = routes.stream()
                .anyMatch(route -> route.getId().equals("user-service"));
        
        assertTrue(userRouteFound, "User Service 라우팅이 설정되어야 함");
        
        Route userRoute = routes.stream()
                .filter(route -> route.getId().equals("user-service"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(userRoute);
        assertTrue(userRoute.getUri().toString().contains("8082"));
        
        System.out.println("User Service 라우팅: " + userRoute.getUri());
    }

    @Test
    void Chat_Service_라우팅_설정_확인() {
        // When
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        // Then
        assertNotNull(routes);
        
        boolean chatRouteFound = routes.stream()
                .anyMatch(route -> route.getId().equals("chat-service"));
        
        assertTrue(chatRouteFound, "Chat Service 라우팅이 설정되어야 함");
        
        Route chatRoute = routes.stream()
                .filter(route -> route.getId().equals("chat-service"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(chatRoute);
        assertTrue(chatRoute.getUri().toString().contains("8083"));
        
        System.out.println("Chat Service 라우팅: " + chatRoute.getUri());
    }

    @Test
    void Batch_Service_라우팅_설정_확인() {
        // When
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        // Then
        assertNotNull(routes);
        
        boolean batchRouteFound = routes.stream()
                .anyMatch(route -> route.getId().equals("batch-service"));
        
        assertTrue(batchRouteFound, "Batch Service 라우팅이 설정되어야 함");
        
        Route batchRoute = routes.stream()
                .filter(route -> route.getId().equals("batch-service"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(batchRoute);
        assertTrue(batchRoute.getUri().toString().contains("8085"));
        
        System.out.println("Batch Service 라우팅: " + batchRoute.getUri());
    }

    @Test
    void 라우팅_필터_설정_확인() {
        // When
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        // Then
        assertNotNull(routes);
        
        // Chat Service에 Rate Limiter 필터가 설정되어 있는지 확인
        Route chatRoute = routes.stream()
                .filter(route -> route.getId().equals("chat-service"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(chatRoute);
        assertFalse(chatRoute.getFilters().isEmpty(), "Chat Service에 필터가 설정되어야 함");
        
        // Batch Service에 Rate Limiter 필터가 설정되어 있는지 확인
        Route batchRoute = routes.stream()
                .filter(route -> route.getId().equals("batch-service"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(batchRoute);
        assertFalse(batchRoute.getFilters().isEmpty(), "Batch Service에 필터가 설정되어야 함");
        
        System.out.println("Chat Service 필터: " + chatRoute.getFilters());
        System.out.println("Batch Service 필터: " + batchRoute.getFilters());
    }

    @Test
    void 라우팅_예측자_설정_확인() {
        // When
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        // Then
        assertNotNull(routes);
        
        for (Route route : routes) {
            assertNotNull(route.getPredicate(), 
                       "Route " + route.getId() + "에 예측자가 설정되어야 함");
            
            System.out.println("Route " + route.getId() + " 예측자: " + route.getPredicate());
        }
    }
}
