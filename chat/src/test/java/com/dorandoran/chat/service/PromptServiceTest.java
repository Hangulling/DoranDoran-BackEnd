package com.dorandoran.chat.service;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Chatbot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatbotRepository chatbotRepository;

    @InjectMocks
    private PromptService promptService;

    @Test
    @DisplayName("룸이 없으면 기본 시스템 프롬프트를 반환한다")
    void buildSystemPrompt_whenRoomMissing_returnsDefault() {
        UUID chatroomId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.empty());

        String prompt = promptService.buildSystemPrompt(chatroomId);

        assertThat(prompt).contains("도란도란의 AI 어시스턴트");
    }

    @Test
    @DisplayName("봇 메타와 룸 컨텍스트를 합성해 한국어 지시가 포함된 프롬프트를 생성한다")
    void buildSystemPrompt_mergesBotAndRoomContext() throws Exception {
        UUID chatroomId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID botId = UUID.fromString("00000000-0000-0000-0000-0000000000b0");

        Chatbot bot = Chatbot.builder().id(botId).build();
        ChatRoom room = ChatRoom.builder()
            .id(chatroomId)
            .chatbot(bot)
            .contextData(new ObjectMapper().readTree("{\n" +
                "  \"conversationSummary\": \"요약입니다\",\n" +
                "  \"userPreferences\": { \"responseLength\": \"short\", \"language\": \"ko\", \"topics\": [\"java\", \"spring\"] },\n" +
                "  \"sessionData\": { \"currentTopic\": \"테스트\" }\n" +
                "}"))
            .build();
        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(room));

        Chatbot botDetails = Chatbot.builder()
            .id(botId)
            .systemPrompt("너는 친절한 비서야.")
            .personality("{\n" +
                "  \"traits\": [\"친절함\", \"신속함\"],\n" +
                "  \"speakingStyle\": { \"honorific\": true, \"formality\": \"polite\", \"length\": \"short\" },\n" +
                "  \"guardrails\": { \"refuseTopics\": [\"정치\"], \"escalationHint\": \"전문가 상담을 권유하세요\" },\n" +
                "  \"domainKnowledge\": [\"java\", \"spring\"],\n" +
                "  \"fewShot\": [ { \"user\": \"안녕\", \"assistant\": \"안녕하세요!\" } ]\n" +
                "}")
            .capabilities("{\n" +
                "  \"responseStyle\": { \"format\": \"markdown\", \"bulletPreference\": \"prefer\", \"maxLength\": 300 },\n" +
                "  \"safety\": { \"profanityFilter\": true, \"piiRedaction\": true }\n" +
                "}")
            .build();
        when(chatbotRepository.findById(botId)).thenReturn(Optional.of(botDetails));

        String prompt = promptService.buildSystemPrompt(chatroomId);

        // 한글 문구를 정확히 검증
        assertThat(prompt)
            .contains("너는 친절한 비서야.")
            .contains("성격 특성: 친절함, 신속함")
            .contains("존댓말을 사용하세요")
            .contains("말투 격식: polite")
            .contains("답변 길이 선호: short")
            .contains("아래 주제는 답변을 정중히 거부하세요: 정치")
            .contains("필요 시 다음 안내를 덧붙이세요: 전문가 상담을 권유하세요")
            .contains("선호/전문 도메인: java, spring")
            .contains("예시 대화")
            .contains("사용자: 안녕")
            .contains("어시스턴트: 안녕하세요!")
            .contains("응답 포맷: markdown")
            .contains("불릿 사용: prefer")
            .contains("최대 길이: 300")
            .contains("욕설/비속어는 완곡하게 표현을 바꾸세요")
            .contains("개인정보는 식별 불가하게 마스킹하세요")
            .contains("대화 요약")
            .contains("요약입니다")
            .contains("사용자 선호")
            .contains("선호 응답 길이: short")
            .contains("언어: ko")
            .contains("관심 주제: java, spring")
            .contains("현재 주제")
            .contains("테스트")
            .contains("응답은 한국어로, 핵심 위주로 간결하게 작성하세요");
    }

    @Test
    @DisplayName("긴 프롬프트는 8000자 제한으로 잘린다")
    void buildSystemPrompt_truncatesLongOutput() {
        UUID chatroomId = UUID.fromString("00000000-0000-0000-0000-000000000012");
        UUID botId = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        StringBuilder veryLong = new StringBuilder();
        for (int i = 0; i < 9000; i++) veryLong.append('a');

        Chatbot bot = Chatbot.builder().id(botId).build();
        ChatRoom room = ChatRoom.builder().id(chatroomId).chatbot(bot).build();
        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(room));

        Chatbot botDetails = Chatbot.builder().id(botId).systemPrompt(veryLong.toString()).build();
        when(chatbotRepository.findById(botId)).thenReturn(Optional.of(botDetails));

        String prompt = promptService.buildSystemPrompt(chatroomId);

        assertThat(prompt.length()).isLessThanOrEqualTo(8000);
        assertThat(prompt).endsWith("...");
    }
}


