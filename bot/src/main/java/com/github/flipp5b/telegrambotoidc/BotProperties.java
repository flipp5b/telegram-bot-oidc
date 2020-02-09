package com.github.flipp5b.telegrambotoidc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@ConfigurationProperties(prefix = "bot")
@Data
public class BotProperties {
    private String username;
    private String token;
    private Proxy proxy;

    public DefaultBotOptions createOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        if (proxy != null) {
            options.setProxyHost(proxy.getHost());
            options.setProxyPort(proxy.getPort());
            var type = DefaultBotOptions.ProxyType.valueOf(proxy.getType());
            options.setProxyType(type);
        }
        return options;
    }

    @Data
    public static class Proxy {
        private String host;
        private Integer port;
        private String type;
    }
}
