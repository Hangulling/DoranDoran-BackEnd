package com.dorandoran.user.service;

import com.dorandoran.user.entity.FcmToken;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final FcmTokenService fcmTokenService;
    private final ObjectProvider<FirebaseApp> firebaseAppProvider;

    @Value("${app.deeplink.scheme:dorandoran://chat}")
    private String appScheme;

    @Value("${app.deeplink.universal-base:https://www.doran-chat.com/chat}")
    private String universalBase;

    public void sendToUser(UUID userId, String title, String body, UUID chatroomId, UUID messageId) {
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
            try {
                Message message = Message.builder()
                    .setToken(token.getToken())
                    .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                    .putData("deeplink", buildSchemeLink(chatroomId, messageId))
                    .putData("universalLink", buildUniversalLink(chatroomId, messageId))
                    .putData("chatroomId", chatroomId != null ? chatroomId.toString() : "")
                    .putData("messageId", messageId != null ? messageId.toString() : "")
                    .putData("startMessage", body != null ? body : "")
                    .putData("sentAt", java.time.OffsetDateTime.now().toString())
                    .build();
                String response = FirebaseMessaging.getInstance(firebaseApp).send(message);
                log.info("푸시 전송 성공: userId={}, tokenId={}, response={}", userId, token.getId(), response);
            } catch (Exception e) {
                log.warn("푸시 전송 실패: userId={}, tokenId={}, error={}", userId, token.getId(), e.getMessage());
            }
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
}
