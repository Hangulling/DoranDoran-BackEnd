package com.dorandoran.user.service;

import com.dorandoran.user.client.ChatRoomClient;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserInterestTopicRepository userInterestTopicRepository;
    private final InterestTopicRepository interestTopicRepository;
    private final PushNotificationService pushNotificationService;
    private final ChatRoomClient chatRoomClient;
    private final PushDeliveryLogRepository pushDeliveryLogRepository;

    @Scheduled(cron = "${notification.daily.cron:0 0 9 * * *}")
    public void sendDailyInterestNotifications() {
        List<UserNotificationSetting> targets = userNotificationSettingRepository.findByPushEnabledTrue();
        for (UserNotificationSetting setting : targets) {
            UUID userId = setting.getUserId();
            List<UserInterestTopic> topics = userInterestTopicRepository.findByIdUserId(userId);
            if (topics.isEmpty()) {
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

            List<ChatRoomClient.ChatRoomSummary> chatrooms = chatRoomClient.listChatRooms(userId);
            if (chatrooms.isEmpty()) {
                continue;
            }
            java.time.LocalDate today = java.time.LocalDate.now();
            for (ChatRoomClient.ChatRoomSummary chatroom : chatrooms) {
                UUID chatroomId = chatroom.id();
                if (pushDeliveryLogRepository.existsByUserIdAndChatroomIdAndSentDate(userId, chatroomId, today)) {
                    continue;
                }
                pushNotificationService.sendToUser(
                    userId,
                    "채팅방",
                    body,
                    chatroomId,
                    null
                );
                pushDeliveryLogRepository.save(PushDeliveryLog.builder()
                    .userId(userId)
                    .chatroomId(chatroomId)
                    .sentDate(today)
                    .build());
            }
        }
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
