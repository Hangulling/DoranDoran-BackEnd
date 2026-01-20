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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromptServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatbotRepository chatbotRepository;

    @Mock
    private com.dorandoran.chat.repository.IntimacyProgressRepository intimacyProgressRepository;

    @Mock
    private PromptLoaderService promptLoaderService;

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
        when(intimacyProgressRepository.findByChatRoomId(chatroomId)).thenReturn(Optional.empty());
        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(room));
        when(promptLoaderService.loadPrompt(any(), any(), anyInt(), any()))
            .thenReturn(null);

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

        assertThat(prompt)
            .contains("응답 포맷: markdown")
            .contains("불릿 사용: prefer")
            .contains("최대 길이: 300")
            .contains("대화 요약")
            .contains("요약입니다")
            .contains("사용자 선호")
            .contains("선호 응답 길이: short")
            .contains("언어: ko")
            .contains("관심 주제: java, spring")
            .contains("현재 주제")
            .contains("테스트")
            .contains("한국어로 답해");
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
        when(intimacyProgressRepository.findByChatRoomId(chatroomId)).thenReturn(Optional.empty());
        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(room));

        Chatbot botDetails = Chatbot.builder().id(botId).systemPrompt(veryLong.toString()).build();
        when(chatbotRepository.findById(botId)).thenReturn(Optional.of(botDetails));
        when(promptLoaderService.loadPrompt(any(), any(), anyInt(), any()))
            .thenReturn(veryLong.toString());

        String prompt = promptService.buildSystemPrompt(chatroomId);

        assertThat(prompt.length()).isLessThanOrEqualTo(8000);
        assertThat(prompt).endsWith("...");
    }
}


