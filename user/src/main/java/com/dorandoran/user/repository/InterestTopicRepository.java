package com.dorandoran.user.repository;

import com.dorandoran.user.entity.InterestTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterestTopicRepository extends JpaRepository<InterestTopic, String> {
    List<InterestTopic> findByIsActiveTrueOrderByTopicKeyAsc();
}
