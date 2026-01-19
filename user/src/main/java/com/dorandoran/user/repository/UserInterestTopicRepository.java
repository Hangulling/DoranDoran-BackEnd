package com.dorandoran.user.repository;

import com.dorandoran.user.entity.UserInterestTopic;
import com.dorandoran.user.entity.UserInterestTopicId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserInterestTopicRepository extends JpaRepository<UserInterestTopic, UserInterestTopicId> {
    List<UserInterestTopic> findByIdUserId(UUID userId);
    void deleteByIdUserId(UUID userId);
}
