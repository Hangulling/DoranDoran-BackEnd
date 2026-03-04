package com.dorandoran.user.service;

import com.dorandoran.user.entity.PushDeliveryLog;
import com.dorandoran.user.entity.UserNotificationSetting;
import com.dorandoran.user.entity.UserInterestTopic;
import com.dorandoran.user.repository.InterestTopicRepository;
import com.dorandoran.user.repository.PushDeliveryLogRepository;
import com.dorandoran.user.repository.UserInterestTopicRepository;
import com.dorandoran.user.repository.UserNotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserInterestTopicRepository userInterestTopicRepository;
    private final InterestTopicRepository interestTopicRepository;
    private final PushNotificationService pushNotificationService;
    private final ConceptChatbotResolver conceptChatbotResolver;
    private final PushDeliveryLogRepository pushDeliveryLogRepository;

    /**
     * 채팅방 컨셉(페르소나) 후보들.
     */
    private static final String[] CONCEPTS = {"honey", "friend", "senior", "coworker"};

    private String pickRandomConcept() {
        int idx = ThreadLocalRandom.current().nextInt(CONCEPTS.length);
        return CONCEPTS[idx];
    }

    /**
     * 임시 테스트용: koachchatapp@gmail.com 계정은
     * 일일 dedup 를 건너뛰고, 배치가 돌 때마다 푸시 발송.
     * (user_id: 3fa11b2f-0e4d-4332-aa76-308407ec68ca)
     */
    private static final UUID FORCE_NO_DEDUP_USER_ID =
        UUID.fromString("3fa11b2f-0e4d-4332-aa76-308407ec68ca");

    @Scheduled(cron = "${notification.daily.cron:0 0 9 * * *}")
    public void sendDailyInterestNotifications() {
        java.time.LocalDate today = java.time.LocalDate.now();
        log.info("[DailyInterestPush] job started, date={}", today);

        final List<UserNotificationSetting> targets;
        try {
            targets = userNotificationSettingRepository.findByPushEnabledTrue();
            log.info("[DailyInterestPush] loaded targets, count={}", targets.size());
        } catch (Exception e) {
            log.error("[DailyInterestPush] failed to load targets: {}", e.getMessage(), e);
            return;
        }

        for (UserNotificationSetting setting : targets) {
            UUID userId = setting.getUserId();
            try {
                log.debug("[DailyInterestPush] processing userId={}", userId);
                boolean forceNoDedup = FORCE_NO_DEDUP_USER_ID.equals(userId);

                List<UserInterestTopic> topics = userInterestTopicRepository.findByIdUserId(userId);
                if (topics == null || topics.isEmpty()) {
                    log.debug("[DailyInterestPush] skip userId={} because no interest topics", userId);
                    continue;
                }

                List<String> keys = topics.stream()
                    .map(t -> t.getId().getTopicKey())
                    .toList();
                Map<String, String> labels = interestTopicRepository.findAllById(keys).stream()
                    .collect(Collectors.toMap(
                        t -> t.getTopicKey(),
                        t -> t.getLabel(),
                        (a, b) -> a
                    ));

                String body = buildBody(keys, labels);
                log.debug("[DailyInterestPush] built body for userId={}: {}", userId, body);

                // 관심 주제 라벨 중 하나를 topic 으로 랜덤 선택
                var resolvedLabels = keys.stream()
                    .map(k -> labels.getOrDefault(k, k))
                    .toList();
                String topic = resolvedLabels.get(ThreadLocalRandom.current().nextInt(resolvedLabels.size()));

                // 컨셉 하나 랜덤 선택
                String concept = pickRandomConcept();
                UUID chatbotId = conceptChatbotResolver.resolve(concept);
                if (chatbotId == null) {
                    log.warn("[DailyInterestPush] skip userId={} because chatbotId could not be resolved for concept={}", userId, concept);
                    continue;
                }

                log.info("[DailyInterestPush] selected topic={}, concept={}, chatbotId={} for userId={}",
                    topic, concept, chatbotId, userId);

                // 일반 유저는 하루 1회만 발송 (userId + sentDate 기준 dedup)
                if (!forceNoDedup &&
                    pushDeliveryLogRepository.existsByUserIdAndSentDate(userId, today)) {
                    log.debug("[DailyInterestPush] skip userId={} because already sent today", userId);
                    continue;
                }

                try {
                    log.info("[DailyInterestPush] sending push via sendByTopic: userId={}, chatbotId={}, topic={}, concept={}",
                        userId, chatbotId, topic, concept);
                    pushNotificationService.sendByTopic(
                        userId,
                        chatbotId,
                        topic,
                        concept,
                        null
                    );

                    if (!forceNoDedup) {
                        PushDeliveryLog saved = pushDeliveryLogRepository.save(PushDeliveryLog.builder()
                            .userId(userId)
                            // chatroomId는 더 이상 의미 있는 값이 없으므로 chatbotId를 대신 기록
                            .chatroomId(chatbotId)
                            .sentDate(today)
                            .build());
                        log.info("[DailyInterestPush] logged daily delivery: id={}, userId={}, chatbotId={}, sentDate={}",
                            saved.getId(), userId, chatbotId, today);
                    } else {
                        log.info("[DailyInterestPush] forceNoDedup user, skip logging dedup entry for userId={}", userId);
                    }
                } catch (Exception e) {
                    log.error("[DailyInterestPush] error while sending push via sendByTopic: userId={}, chatbotId={}, topic={}, concept={}, error={}",
                        userId, chatbotId, topic, concept, e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("[DailyInterestPush] error while processing userId={}: {}", userId, e.getMessage(), e);
            }
        }

        log.info("[DailyInterestPush] job finished, date={}", today);
    }

    private String buildBody(List<String> keys, Map<String, String> labels) {
        if (keys == null || keys.isEmpty()) {
            return "대화를 이어가볼까요?";
        }
        List<String> resolved = keys.stream()
            .map(k -> labels.getOrDefault(k, k))
            .collect(Collectors.toList());
        if (resolved.size() == 1) {
            return resolved.get(0) + " 주제로 이야기 이어가요.";
        }
        String first = resolved.get(0);
        String second = resolved.get(1);
        return "오늘은 " + first + ", " + second + " 이야기 어때요?";
    }
}
