package com.github.flipp5b.telegrambotoidc.auth;

import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
class UserTrackerStorage {
    private final Duration expiration = Duration.ofMinutes(30);
    private final ConcurrentMap<String, Integer> trackers = ExpiringMap.builder()
            .expiration(expiration.toMinutes(), TimeUnit.MINUTES)
            .expirationListener((t, u) -> log.debug("Auth tracker expired: {} -> {}", t, u))
            .build();

    void put(String tracker, Integer userId) {
        trackers.put(tracker, userId);
    }

    Optional<Integer> find(String tracker) {
        Integer userId = trackers.remove(tracker);
        if (userId != null) {
            log.debug("User identified: {} -> {}. Auth tracker invalidated.", tracker, userId);
            return Optional.of(userId);
        } else {
            log.debug("Auth tracker is invalid.");
            return Optional.empty();
        }
    }
}
