package com.dorandoran.user.service;

import com.dorandoran.user.entity.FcmToken;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final FcmTokenService fcmTokenService;
    private final ObjectProvider<FirebaseApp> firebaseAppProvider;
    private final PushNotificationAgent pushNotificationAgent;

    @Value("${app.deeplink.scheme:dorandoran://chat}")
    private String appScheme;

    @Value("${app.deeplink.universal-base:https://www.doran-chat.com/chat}")
    private String universalBase;

    public void sendToUser(UUID userId, String title, String body, UUID chatroomId, UUID messageId) {
        sendToUser(userId, title, body, chatroomId, messageId, Map.of());
    }

    /**
     * 공통 푸시 전송 로직.
     * - 기본 deeplink/universalLink/startMessage/sentAt 필드 외에 추가 데이터를 data payload에 포함할 수 있다.
     */
    public void sendToUser(UUID userId,
                           String title,
                           String body,
                           UUID chatroomId,
                           UUID messageId,
                           Map<String, String> extraData) {
        List<FcmToken> tokens = fcmTokenService.getTokens(userId);
        if (tokens.isEmpty()) {
            return;
        }
        FirebaseApp firebaseApp = firebaseAppProvider.getIfAvailable();
        for (FcmToken token : tokens) {
            if (firebaseApp == null) {
                log.info("푸시 전송 스킵(Firebase 미설정): userId={}, platform={}, token={}, title={}, body={}, chatroomId={}, messageId={}",
                    userId, token.getPlatform(), token.getToken(), title, body, chatroomId, messageId);
                continue;
            }

            Map<String, String> safeExtraData = extraData != null ? extraData : new HashMap<>();
            boolean isIos = token.getPlatform() != null && "ios".equalsIgnoreCase(token.getPlatform());
            String tokenValue = token.getToken() != null ? token.getToken() : "";
            String tokenPreview = tokenValue.length() > 12 ? tokenValue.substring(0, 12) + "..." : tokenValue;

            try {
                String deeplink = buildSchemeLink(chatroomId, messageId);
                String universalLink = buildUniversalLink(chatroomId, messageId);
                String startMessage = body != null ? body : "";
                String sentAt = OffsetDateTime.now().toString();

                if (isIos) {
                    log.info("[iOS FCM debug] send start: userId={}, tokenId={}, tokenPreview={}, title='{}', bodyLen={}, deeplink='{}', universalLink='{}', extraKeys={}",
                        userId,
                        token.getId(),
                        tokenPreview,
                        title,
                        body != null ? body.length() : 0,
                        deeplink,
                        universalLink,
                        safeExtraData.keySet());
                }

                Message.Builder builder = Message.builder()
                    .setToken(token.getToken())
                    .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                    .putData("deeplink", deeplink)
                    .putData("universalLink", universalLink)
                    .putData("chatroomId", chatroomId != null ? chatroomId.toString() : "")
                    .putData("messageId", messageId != null ? messageId.toString() : "")
                    .putData("startMessage", startMessage)
                    .putData("sentAt", sentAt);

                // 개념/주제 기반 푸시 등에서 추가 컨텍스트를 payload 에 포함
                for (Map.Entry<String, String> entry : safeExtraData.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                        builder.putData(key, value);
                    }
                }

                Message message = builder.build();
                log.debug("[FCM 401 디버그] FCM send 직전: FirebaseApp.name={}, userId={}, tokenId={}",
                    firebaseApp.getName(), userId, token.getId());
                String response = FirebaseMessaging.getInstance(firebaseApp).send(message);
                log.info("푸시 전송 성공: userId={}, tokenId={}, response={}", userId, token.getId(), response);
                if (isIos) {
                    log.info("[iOS FCM debug] send success: userId={}, tokenId={}, response={}", userId, token.getId(), response);
                }
            } catch (Exception e) {
                if (isIos) {
                    log.error("[iOS FCM debug] send failed: userId={}, tokenId={}, tokenPreview={}, errorType={}, errorMessage={}",
                        userId,
                        token.getId(),
                        tokenPreview,
                        e.getClass().getName(),
                        e.getMessage(),
                        e);
                } else {
                    log.warn("푸시 전송 실패: userId={}, tokenId={}, error={}", userId, token.getId(), e.getMessage());
                }
                if (isDeletableInvalidToken(e)) {
                    try {
                        fcmTokenService.deleteToken(token.getId());
                        log.info("무효 FCM 토큰 삭제: tokenId={}, userId={}, reason={}", token.getId(), userId, e.getMessage());
                    } catch (Exception ex) {
                        log.warn("FCM 토큰 삭제 실패: tokenId={}", token.getId(), ex);
                    }
                }
            }
        }
    }

    /**
     * 개념/주제 기반 일일 푸시 전송 헬퍼.
     * - PushNotificationAgent 로 title/body 생성
     * - chatroomId/messageId 는 사용하지 않고 null 로 둔다 (딥링크는 앱에서 chatbotId 등을 기반으로 처리)
     */
    public void sendByTopic(UUID userId,
                            UUID chatbotId,
                            String topic,
                            String concept,
                            Integer intimacyLevelOverride) {
        int intimacy = (intimacyLevelOverride != null ? intimacyLevelOverride : 1);
        PushNotificationAgent.PushNotificationText text =
            pushNotificationAgent.generateNotificationText(userId, chatbotId, topic, concept, intimacy);

        log.info("sendByTopic: userId={}, chatbotId={}, topic={}, concept={}, intimacy={}, title='{}', body='{}'",
            userId, chatbotId, topic, concept, intimacy, text.title(), text.body());

        // chatroomId/messageId 는 null 로 두고 사용자가 앱에서 진입 후 새 방/기존 방을 선택하도록 한다.
        Map<String, String> extra = new HashMap<>();
        if (topic != null && !topic.isBlank()) {
            extra.put("topic", topic);
        }
        if (concept != null && !concept.isBlank()) {
            extra.put("concept", concept);
        }
        if (chatbotId != null) {
            extra.put("chatbotId", chatbotId.toString());
        }
        extra.put("intimacyLevel", String.valueOf(intimacy));

        // data.notification.title/body도 제공하기 위해, top-level notification.title/body를 먼저 결정
        String notificationTitle = (concept != null && !concept.isBlank()) ? concept : text.title();
        String notificationBody = text.body() != null ? text.body() : "";

        // data.notification: { "title": "...", "body": "..." } 형태를 JSON 문자열로 넣는다.
        // FCM data payload 값은 string만 허용되므로, 최종 payload에서 따옴표가 이스케이프될 수는 있다.
        // 대신 여기서는 "이중 escape"를 피하기 위해 ObjectMapper로 직렬화한다.
        String notificationJson = buildNotificationJson(notificationTitle, notificationBody);
        extra.put("notification", notificationJson);

        // top-level notification.title/body도 의도한 값으로 맞춤
        sendToUser(userId, notificationTitle, notificationBody, null, null, extra);
    }

    private String buildNotificationJson(String title, String body) {
        try {
            return new ObjectMapper().writeValueAsString(Map.of(
                "title", title != null ? title : "",
                "body", body != null ? body : ""
            ));
        } catch (Exception e) {
            // 직렬화 실패 시에도 클라이언트가 최소 동작하게 빈 구조를 보낸다.
            log.warn("notification JSON 직렬화 실패: {}", e.getMessage());
            return "{\"title\":\"\",\"body\":\"\"}";
        }
    }

    private String buildSchemeLink(UUID chatroomId, UUID messageId) {
        StringBuilder sb = new StringBuilder(appScheme);
        sb.append("?roomId=").append(chatroomId != null ? chatroomId : "");
        if (messageId != null) {
            sb.append("&messageId=").append(messageId);
        }
        return sb.toString();
    }

    private String buildUniversalLink(UUID chatroomId, UUID messageId) {
        StringBuilder sb = new StringBuilder(universalBase);
        sb.append("?roomId=").append(chatroomId != null ? chatroomId : "");
        if (messageId != null) {
            sb.append("&messageId=").append(messageId);
        }
        return sb.toString();
    }

    /**
     * FCM이 "이 토큰은 더 이상 유효하지 않다"고 알린 경우 true.
     * UNREGISTERED, INVALID_ARGUMENT 또는 "Requested entity was not found" 메시지는 DB에서 삭제 후 재등록 유도.
     */
    private boolean isDeletableInvalidToken(Throwable e) {
        if (e instanceof FirebaseMessagingException fme) {
            MessagingErrorCode code = fme.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                return true;
            }
        }
        String msg = e.getMessage();
        if (msg != null && msg.contains("Requested entity was not found")) {
            return true;
        }
        return false;
    }
}
