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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChatRoomHardDeleteBatchTest {

    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired UserRepository userRepository;
    @Autowired ChatbotRepository chatbotRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void 배치_하드삭제_SQL_검증_isDeleted_true_행_삭제_ON_CASCADE() {
        // given
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test2@example.com");
        userRepository.save(user);

        Chatbot bot = new Chatbot();
        bot.setId(UUID.randomUUID());
        bot.setName("봇");
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

        // when: 소프트 삭제 후 배치 하드 삭제 SQL 수행
        room.setIsDeleted(true);
        chatRoomRepository.save(room);
        int deleted = jdbcTemplate.update("DELETE FROM chat_schema.chatrooms WHERE is_deleted = TRUE");

        // then: 방은 삭제되고, 메시지는 ON DELETE CASCADE로 삭제됨
        assertThat(deleted).isEqualTo(1);
        assertThat(chatRoomRepository.findById(room.getId())).isEmpty();
        assertThat(messageRepository.findByChatRoomId(room.getId())).isEmpty();
    }
}


