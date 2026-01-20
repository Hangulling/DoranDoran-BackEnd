package com.dorandoran.user.service;

import com.dorandoran.user.entity.UserStats;
import com.dorandoran.user.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserStatsRepository userStatsRepository;

    @Transactional
    public UserStats touchStreak(UUID userId, LocalDate today) {
        UserStats stats = userStatsRepository.findById(userId)
            .orElseGet(() -> UserStats.builder()
                .userId(userId)
                .streakCount(0)
                .perfectCount(0)
                .build());

        LocalDate lastActive = stats.getLastActiveDate();
        if (lastActive == null) {
            stats.setStreakCount(1);
        } else if (lastActive.isEqual(today)) {
            return stats;
        } else if (lastActive.plusDays(1).isEqual(today)) {
            stats.setStreakCount(stats.getStreakCount() + 1);
        } else {
            stats.setStreakCount(1);
        }

        stats.setLastActiveDate(today);
        return userStatsRepository.save(stats);
    }

    @Transactional(readOnly = true)
    public UserStats getOrCreate(UUID userId) {
        return userStatsRepository.findById(userId)
            .orElseGet(() -> UserStats.builder()
                .userId(userId)
                .streakCount(0)
                .perfectCount(0)
                .build());
    }

    @Transactional
    public UserStats incrementPerfect(UUID userId) {
        UserStats stats = userStatsRepository.findById(userId)
            .orElseGet(() -> UserStats.builder()
                .userId(userId)
                .streakCount(0)
                .perfectCount(0)
                .build());
        stats.setPerfectCount(stats.getPerfectCount() + 1);
        return userStatsRepository.save(stats);
    }
}
