package com.dorandoran.chat.controller;

import com.dorandoran.chat.entity.billing.MonthlyUserCost;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.repository.billing.MonthlyUserCostRepository;
import com.dorandoran.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final MonthlyUserCostRepository monthlyUserCostRepository;
    private final UserRepository userRepository;

    @GetMapping("/users/{userId}/months/{month}")
    public ResponseEntity<List<MonthlyUserCost>> getMonthly(
            @PathVariable UUID userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        LocalDate firstDay = month.withDayOfMonth(1);
        // User 객체 조회 후 Repository 메서드 호출
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        List<MonthlyUserCost> list = monthlyUserCostRepository.findByUserAndBillingMonthBetween(user, firstDay, firstDay);
        return ResponseEntity.ok(list);
    }
}


