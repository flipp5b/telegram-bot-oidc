package com.github.flipp5b.telegrambotoidc;

import com.github.flipp5b.telegrambotoidc.auth.OidcService;
import com.github.flipp5b.telegrambotoidc.auth.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class Bot extends TelegramLongPollingBot {
    private final String username;
    private final String token;
    private final OidcService oidcService;

    public Bot(BotProperties properties, OidcService oidcService) {
        super(properties.createOptions());
        this.username = properties.getUsername();
        this.token = properties.getToken();
        this.oidcService = oidcService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.debug("Got a new update: {}", update);
        if (!update.hasMessage()) {
            log.debug("Update has no message. Skip processing.");
            return;
        }

        var userId = update.getMessage().getFrom().getId();
        var chatId = update.getMessage().getChatId();
        oidcService.findUserInfo(userId).ifPresentOrElse(
                userInfo -> greet(userInfo, chatId),
                () -> askForLogin(userId, chatId));
    }

    private void greet(UserInfo userInfo, Long chatId) {
        var username = userInfo.getPreferredUsername();
        var message = String.format("Hello, <b>%s</b>!\nYou are the best! Have a nice day!", username);
        sendHtmlMessage(message, chatId);
    }

    private void askForLogin(Integer userId, Long chatId) {
        var url = oidcService.getAuthUrl(userId);
        var message = String.format("Please, <a href=\"%s\">log in</a>.", url);
        sendHtmlMessage(message, chatId);
    }

    void sendHtmlMessage(String message, Long chatId) {
        var sendMessage = new SendMessage(chatId, message);
        sendMessage.setParseMode("HTML");
        tryExecute(sendMessage);
    }

    private void tryExecute(BotApiMethod<?> action) {
        try {
            execute(action);
        } catch (TelegramApiException e) {
            String message = String.format(
                    "Ooops! Something went wrong during action execution. Action: %s. Error: %s.",
                    action,
                    e);
            log.error(message, e);
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
