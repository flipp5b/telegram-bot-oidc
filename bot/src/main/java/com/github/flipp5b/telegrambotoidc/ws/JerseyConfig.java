package com.github.flipp5b.telegrambotoidc.ws;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        register(AuthEndpoint.class);
    }
}
