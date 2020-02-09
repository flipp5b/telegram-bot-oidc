package com.github.flipp5b.telegrambotoidc.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oidc.keycloak")
@Data
public class OidcProperties {
    private String clientId;
    private String clientSecret;
    private String callback;
    private String baseUrl;
    private String realm;
}
