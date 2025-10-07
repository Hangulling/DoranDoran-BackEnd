package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.billing.AiUsageEvent;
import com.dorandoran.chat.entity.billing.MonthlyUserCost;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.repository.billing.AiUsageEventRepository;
import com.dorandoran.chat.repository.billing.MonthlyUserCostRepository;
import com.dorandoran.chat.repository.UserRepository;
import com.dorandoran.chat.repository.ChatRoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final AiUsageEventRepository aiUsageEventRepository;
    private final MonthlyUserCostRepository monthlyUserCostRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public void recordUsage(UUID userId, UUID chatroomId, String provider, String model,
                            String requestId, int inTokens, int outTokens, double costIn, double costOut) {
        // User와 ChatRoom 객체 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
            .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + chatroomId));
        
        AiUsageEvent event = AiUsageEvent.builder()
            .id(UUID.randomUUID())
            .eventTime(OffsetDateTime.now(ZoneOffset.UTC))
            .user(user)
            .chatRoom(chatRoom)
            .provider(provider)
            .model(model)
            .requestId(requestId)
            .inputTokens(inTokens)
            .outputTokens(outTokens)
            .costIn(costIn)
            .costOut(costOut)
            .build();
        aiUsageEventRepository.save(event);

        upsertMonthly(userId, event.getEventTime().toLocalDate().withDayOfMonth(1), inTokens, outTokens, costIn, costOut);
    }

    @Transactional
    public void upsertMonthly(UUID userId, LocalDate month, long inTokens, long outTokens, double costIn, double costOut) {
        // User 객체 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        MonthlyUserCost row = monthlyUserCostRepository.findByUserAndBillingMonth(user, month)
            .orElseGet(() -> MonthlyUserCost.builder()
                .billingMonth(month)
                .user(user)
                .inputTokens(0L)
                .outputTokens(0L)
                .costIn(0.0)
                .costOut(0.0)
                .totalCost(0.0)
                .lastAggregatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());

        row.setInputTokens(row.getInputTokens() + inTokens);
        row.setOutputTokens(row.getOutputTokens() + outTokens);
        row.setCostIn(row.getCostIn() + costIn);
        row.setCostOut(row.getCostOut() + costOut);
        row.setTotalCost(row.getCostIn() + row.getCostOut());
        row.setLastAggregatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        monthlyUserCostRepository.save(row);
    }
}


