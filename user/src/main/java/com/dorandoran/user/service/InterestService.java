package com.dorandoran.user.service;

import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.user.dto.InterestTopicDto;
import com.dorandoran.user.entity.InterestTopic;
import com.dorandoran.user.entity.UserInterestTopic;
import com.dorandoran.user.entity.UserInterestTopicId;
import com.dorandoran.user.repository.InterestTopicRepository;
import com.dorandoran.user.repository.UserInterestTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterestService {

    private final InterestTopicRepository interestTopicRepository;
    private final UserInterestTopicRepository userInterestTopicRepository;

    @Transactional(readOnly = true)
    public List<InterestTopicDto> getUserInterests(UUID userId) {
        List<UserInterestTopic> mappings = userInterestTopicRepository.findByIdUserId(userId);
        if (mappings.isEmpty()) {
            return List.of();
        }
        List<String> keys = mappings.stream()
            .map(m -> m.getId().getTopicKey())
            .toList();
        return interestTopicRepository.findAllById(keys).stream()
            .filter(InterestTopic::isActive)
            .map(topic -> new InterestTopicDto(topic.getTopicKey(), topic.getLabel()))
            .collect(Collectors.toList());
    }

    @Transactional
    public List<InterestTopicDto> updateUserInterests(UUID userId, List<String> topicKeys) {
        List<String> keys = topicKeys == null ? List.of() : topicKeys.stream()
            .filter(k -> k != null && !k.isBlank())
            .distinct()
            .toList();

        for (String key : keys) {
            if (!interestTopicRepository.existsById(key)) {
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "존재하지 않는 관심 주제입니다: " + key);
            }
        }

        userInterestTopicRepository.deleteByIdUserId(userId);
        List<UserInterestTopic> newMappings = keys.stream()
            .map(key -> UserInterestTopic.builder()
                .id(new UserInterestTopicId(userId, key))
                .build())
            .toList();
        userInterestTopicRepository.saveAll(newMappings);
        return getUserInterests(userId);
    }
}
