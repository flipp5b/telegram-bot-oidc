package com.github.flipp5b.telegrambotoidc.auth;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessTokenErrorResponse;
import com.github.scribejava.core.oauth.OAuth20Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import net.jodah.expiringmap.ExpiringValue;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
class TokenStorage {
    private final ExpiringMap<Integer, OpenIdOAuth2AccessToken> accessTokens = ExpiringMap.builder()
            .variableExpiration()
            .expirationListener((u, t) -> log.debug("Access token expired (user id {})", u))
            .expiringEntryLoader(this::refreshAccessToken)
            .build();
    private final Map<Integer, String> offlineTokens = new ConcurrentHashMap<>();
    private final OAuth20Service oAuthService;

    Optional<OpenIdOAuth2AccessToken> find(Integer userId) {
        return Optional.ofNullable(accessTokens.get(userId));
    }

    void put(Integer userId, OpenIdOAuth2AccessToken token) {
        accessTokens.put(userId, token, token.getExpiresIn(), TimeUnit.SECONDS);
        offlineTokens.put(userId, token.getRefreshToken());
    }

    private ExpiringValue<OpenIdOAuth2AccessToken> refreshAccessToken(Integer userId) {
        log.debug("Cache miss (user id {}). Refreshing access token.", userId);
        try {
            var offlineToken = offlineTokens.get(userId);
            if (offlineToken == null) {
                log.debug("Cannot find offline token. Skip refresh.");
                return null;
            }
            var accessToken = (OpenIdOAuth2AccessToken) oAuthService.refreshAccessToken(offlineToken);
            return new ExpiringValue<>(accessToken, accessToken.getExpiresIn(), TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | OAuth2AccessTokenErrorResponse e) {
            log.error("Cannot refresh access token", e);
            return null;
        }
    }
}
