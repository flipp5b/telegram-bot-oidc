package com.github.flipp5b.telegrambotoidc.auth;

import com.auth0.jwt.JWT;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import lombok.Value;

@Value
public class UserInfo {
    String subject;
    String preferredUsername;

    static UserInfo of(OpenIdOAuth2AccessToken token) {
        var jwt = JWT.decode(token.getOpenIdToken());
        var subject = jwt.getSubject();
        var preferredUsername = jwt.getClaim("preferred_username").asString();

        return new UserInfo(subject, preferredUsername);
    }
}
