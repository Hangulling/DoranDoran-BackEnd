package com.dorandoran.chat.service;

import com.dorandoran.chat.config.AIConfig;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.messaging.RedisMessagePublisher;
import com.dorandoran.chat.sse.SSEManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final SSEManager sseManager;
    private final ChatService chatService;
    private final AIConfig aiConfig;
    private final OpenAIClient openAIClient;
    private final PromptService promptService;
    private final BillingService billingService;
    private final RedisMessagePublisher redisPublisher;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void streamAIResponse(Message userMessage) {
        UUID chatroomId = userMessage.getChatRoom().getId();
        // 환경변수에서 주입된 키를 사용할 준비가 되어 있음 (aiConfig.getApiKey())
        // 실제 OpenAI 스트리밍 연동은 추후 구현
        sseManager.send(chatroomId, "ai_info", "model=" + aiConfig.getModel());
        // 간단: 사용자 메시지 content를 프롬프트로 사용
        String system = promptService.buildSystemPrompt(chatroomId);
        String content = userMessage.getContent();
        if (content != null && content.length() > aiConfig.getMaxPromptChars()) {
            content = content.substring(0, Math.max(0, aiConfig.getMaxPromptChars() - 3)) + "...";
        }
        final int[] inSum = {0};
        final int[] outSum = {0};

        openAIClient.streamRawCompletion(system, content)
            .flatMap(raw -> {
                // 사용량 집계 처리
                var usage = openAIClient.extractUsage(raw);
                if (!usage.isEmpty()) {
                    inSum[0] += usage.inputTokens();
                    outSum[0] += usage.outputTokens();
                    double costIn = (usage.inputTokens() / 1000.0) * aiConfig.getPricePer1kInput();
                    double costOut = (usage.outputTokens() / 1000.0) * aiConfig.getPricePer1kOutput();
                    sseManager.send(chatroomId, "ai_usage", String.format("tokens_in=%d,tokens_out=%d,cost_in=%.6f,cost_out=%.6f", usage.inputTokens(), usage.outputTokens(), costIn, costOut));
                    billingService.recordUsage(userMessage.getSenderId(), chatroomId, "openai", aiConfig.getModel(), null, usage.inputTokens(), usage.outputTokens(), costIn, costOut);
                }
                // 텍스트 조각 변환
                return openAIClient.extractText(raw)
                    .doOnNext(text -> sseManager.send(chatroomId, "ai_response", text));
            })
            .doOnError(ex -> {
                log.error("AI 스트리밍 중 오류 발생: chatroomId={}", chatroomId, ex);
                sseManager.send(chatroomId, "ai_error", ex.getMessage());
            })
            .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofSeconds(1)).transientErrors(true))
            .doOnComplete(() -> {
                try {
//                    Message saved = chatService.sendMessage(chatroomId, null, "bot", "(AI 응답 완료)", "text");
                    Message saved = chatService.sendTemporaryMessage(chatroomId, null, "bot", "(AI ì'ë‹µ ì™„ë£Œ)", "text");
                    sseManager.send(chatroomId, "ai_response_done", saved.getId());
                    double totalCostIn = (inSum[0] / 1000.0) * aiConfig.getPricePer1kInput();
                    double totalCostOut = (outSum[0] / 1000.0) * aiConfig.getPricePer1kOutput();
                    sseManager.send(chatroomId, "ai_usage_total", String.format("tokens_in=%d,tokens_out=%d,cost_in=%.6f,cost_out=%.6f", inSum[0], outSum[0], totalCostIn, totalCostOut));

                    // AI 응답 완료 이벤트 발행 (Redis Pub/Sub)
                    redisPublisher.publishAIResponseCompleteEvent(chatroomId, saved.getId(), saved.getContent(), inSum[0] + outSum[0]);

                    log.info("AI 응답 완료: chatroomId={}, messageId={}, tokens_in={}, tokens_out={}",
                        chatroomId, saved.getId(), inSum[0], outSum[0]);
                } catch (Exception e) {
                    log.error("AI 응답 완료 처리 중 오류: chatroomId={}", chatroomId, e);
                    sseManager.send(chatroomId, "ai_error", "AI 응답 완료 처리 중 오류가 발생했습니다.");
                }
            })
            .subscribe();
    }

    private String chunk(String s) { return s; }

    // 시스템 프롬프트 합성은 PromptService로 이동
}
