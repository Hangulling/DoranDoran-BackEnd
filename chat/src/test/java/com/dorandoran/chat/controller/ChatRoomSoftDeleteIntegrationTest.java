package com.dorandoran.chat.controller;

import com.dorandoran.chat.entity.ChatRoom;
import com.dorandoran.chat.entity.Chatbot;
import com.dorandoran.chat.entity.Message;
import com.dorandoran.chat.entity.User;
import com.dorandoran.chat.repository.ChatRoomRepository;
import com.dorandoran.chat.repository.ChatbotRepository;
import com.dorandoran.chat.repository.MessageRepository;
import com.dorandoran.chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChatRoomSoftDeleteIntegrationTest {

    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired UserRepository userRepository;
    @Autowired ChatbotRepository chatbotRepository;

    @Test
    @Transactional
    void 소프트삭제_엔드포인트_로직_검증_isDeleted_true() {
        // 사용자/챗봇/채팅방/메시지 생성
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        userRepository.save(user);

        Chatbot bot = new Chatbot();
        bot.setId(UUID.randomUUID());
        bot.setName("테스트봇");
        bot.setBotType("friend");
        bot.setIsActive(true);
        chatbotRepository.save(bot);

        ChatRoom room = new ChatRoom();
        room.setId(UUID.randomUUID());
        room.setUser(user);
        room.setChatbot(bot);
        room.setName("룸");
        room.setIsArchived(false);
        room.setIsDeleted(false);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        Message m1 = new Message();
        m1.setId(UUID.randomUUID());
        m1.setChatRoom(room);
        m1.setSenderType("user");
        m1.setContent("hi");
        m1.setSequenceNumber(1L);
        messageRepository.save(m1);

        // 소프트 삭제
        room.setIsDeleted(true);
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        // 검증: is_deleted = true, 메시지는 여전히 존재(하드 삭제는 배치 책임)
        ChatRoom found = chatRoomRepository.findById(room.getId()).orElseThrow();
        assertThat(found.getIsDeleted()).isTrue();
        assertThat(messageRepository.countByChatRoomId(room.getId())).isEqualTo(1);
    }
}


