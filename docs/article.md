# Как подружить Telegram-бот с OpenId Connect

Представим себе ситуацию: аналитики компании Foobar Inc. провели тщательное исследование конъюнктуры рынка и бизнес-процессов компании и пришли к выводу, что для оптимизации издержек и многократного увеличения прибыли Foobar кровь из носу требуется Telegram-бот компаньон, способный подбодрить сотрудников в трудную минуту.

Естественно, Foobar не может позволить, чтобы коварные конкуренты воспользовались их ноу-хау, просто добавив их бота себе в контакты. Поэтому требуется, чтобы бот разговаривал только с сотрудниками Foobar, прошедшими аутентификацию в корпоративной системе единого входа (SSO) на основе OpenId Connect.

<video>https://www.youtube.com/watch?v=kJ65rKtOrTw</video>

<cut/>

## В теории

OpenId Connect (OIDC) — это протокол аутентификации, основанный на семействе спецификаций OAuth 2.0. В нем процесс аутентификации может проходить по различным сценариям, называемым потоками (flow), и включает в себя три стороны:

- владелец ресурса (resource owner) — пользователь;
- клиент (client) — приложение, запрашивающее аутентификацию;
- сервер авторизации (authorization server) — приложение, хранящее информацию о пользователе и способное его аутентифицировать.

Получаемая клиентом в результате аутентификации информация о пользователе представляется в виде [JWT](https://ru.wikipedia.org/wiki/JSON_Web_Token)-токенов (JSON Web Token). Не будем сейчас углубляться в терминологию и детали [спецификации OIDC](https://openid.net/specs/openid-connect-core-1_0.html), поскольку и без того есть немало статей, в том числе и на [Хабре](https://habr.com/ru/company/flant/blog/475942/), позволяющих составить представление о базовых принципах работы OIDC.

Для решения поставленной задачи, помимо реализации основной функциональности бота, нам необходимо реализовать механизм авторизации: при получении каждого нового сообщения от пользователя необходимо проверять наличие и актуальность связки между данным Telegram-пользователем и учеткой в системе SSO. Неавторизованные пользователи при этом должны направляться на страницу аутентификации.

Для начала нам необходимо определиться с тем, какой поток аутентификации мы будем использовать. В спецификации OIDC описано несколько возможных сценариев аутентификации пользователей:

- поток с кодом авторизации (authorization code flow),
- неявный поток (implicit flow),
- гибридный поток (hybrid flow),
- и другие потоки, определенные в OAuth 2.0.

Поскольку наш бот — это серверное приложение с возможностью доступа по HTTP, нам подойдет поток с кодом авторизации.

Схема взаимодействия будет выглядеть следующим образом:

<img src="https://habrastorage.org/webt/6z/cu/tx/6zcutxveqfiyt3h5kwjcluhwyec.png" alt="Схема потока с кодом авторизации" align="center"/>

1. Неаутентифицированный пользователь пишет боту.
2. Бот генерирует ссылку на сервер авторизации с запросом кода авторизации и отправляет ее пользователю.
3. Пользователь переходит по ссылке.
4. Сервер аутентифицирует пользователя (например, с помощью формы логина).
5. После окончания процесса аутентификации сервер авторизации перенаправляет пользователя на callback URL бота с указанием кода авторизации.
6. Бот просит сервер авторизации обменять код авторизации на токен доступа и ID токен.
7. Сервер авторизации направляет боту запрошенные токены.

В описанном сценарии все хорошо, кроме того, что на последнем шаге, когда бот получил токен доступа и ID токен, у него уже нет информации о том, какой Telegram-пользователь инициировал всю цепочку действий.

Здесь самое время вспомнить про параметр [state](https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest) запроса аутентификации, который рекомендуется использовать для предотвращения атак межсайтовой подделки запросов (XSRF). Значение state формируется на стороне бота на шаге 2 и добавляется в URL в виде параметра, затем сервер авторизации на шаге 5 в неизменном виде передает это значение в callback URL бота. Таким образом, если мы на шаге 2 свяжем сгенерированный state с id Telegram-пользователя, то на шаге 7 по значению state сможем сопоставить id Telegram-пользователя с полученными токенами. Бинго!

## На практике

Итак, перейдем к реализации, вооружившись следующими инструментами:

- [Keycloak](https://www.keycloak.org) — open source система идентификации и управления доступом (Identity and Access Management), реализующая стандарты OAuth 2.0, Open ID Connect и SAML.
- [Spring Boot](https://spring.io/projects/spring-boot) в качестве каркаса для приложения.
- [TelegramBots](https://github.com/rubenlagus/TelegramBots) — популярная Java библиотека для создания Telegram-ботов. Небольшой бонус — наличие интеграции со Spring Boot.
- [ScribeJava](https://github.com/scribejava/scribejava) — полнофункциональный OAuth клиент для Java. Хорош своей простой, минимальным количеством зависимостей и наличием пресетов для множества OAuth провайдеров, в том числе и для Keycloak. Здесь стоит заметить, что Keycloak также имеет клиентские адаптеры для веб-приложений под разные платформы, и Spring Boot — не исключение. Эти адаптеры позволяют настроить аутентификацию пользователей через Keycloak несколькими строчками в конфигурационном файле, но поскольку наш клиент — Telegram-бот, и аутентификация требует запоминания связки state с id Telegram-пользователя, нам такой вариант не подходит.
- [Java JWT от Auth0](https://github.com/auth0/java-jwt) — реализация JWT для Java, она нам пригодится для работы с ID токенами.

Для начала посмотрим на реализацию бота:

```java
@Component
public class Bot extends TelegramLongPollingBot {
    private final OidcService oidcService;
    // ...

    // Метод, который вызывается при получении ботом новых сообщений,
    // inline-запросов и прочих обновлений.
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            log.debug("Update has no message. Skip processing.");
            return;
        }

        // Id Telegram-пользователя.
        var userId = update.getMessage().getFrom().getId();
        var chatId = update.getMessage().getChatId();
        // Запрашиваем UserInfo (структуру с информацией о пользователе,
        // полученной от сервера авторизации) по id Telegram-пользователя.
        oidcService.findUserInfo(userId).ifPresentOrElse(
                userInfo -> greet(userInfo, chatId),
                () -> askForLogin(userId, chatId));
    }

    private void greet(UserInfo userInfo, Long chatId) {
        // Здесь могло быть обращение к смежному сервису с использованием
        // токена доступа. При этом с точки зрения смежного сервиса обращение
        // бы выполнялось от имени пользователя, приславшего боту сообщение.
        var username = userInfo.getPreferredUsername();
        var message = String.format(
                "Hello, <b>%s</b>!\nYou are the best! Have a nice day!",
                username);
        sendHtmlMessage(message, chatId);
    }

    private void askForLogin(Integer userId, Long chatId) {
        // Формируем URL для аутентификации пользователя
        // (см. шаг 2 на схеме взаимодействия).
        var url = oidcService.getAuthUrl(userId);
        var message = String.format("Please, <a href=\"%s\">log in</a>.", url);
        sendHtmlMessage(message, chatId);
    }
    // ...
}
```

Далее — определение метода поиска UserInfo по id Telegram-пользователя и метода формирования URL для аутентификации пользователя:

```java
@Service
public class OidcService {
    // OAuth20Service — класс из ScribeJava.
    // Через него, собственно, и происходит все общение с сервером авторизации.
    private final OAuth20Service oAuthService;
    // UserTrackerStorage — хранилище трекеров пользователей
    // (связок state -> id Telegram-пользователя).
    private final UserTrackerStorage userTrackers;
    // TokenStorage — "умное" хранилище токенов (токенов доступа, ID токенов и
    // токенов обновления (refresh token)) в привязке к id Telegram-пользователя.
    // Если срок действия запрашиваемого токена доступа истек, хранилище само
    // обращается к серверу авторизации за свежим токеном доступа, предъявляя
    // соответствующий токен обновления.
    // При этом заметим, что поскольку бот не является типичным веб-приложением,
    // взаимодействующим с пользователем через браузер, нам требуются особые
    // токены обновления, которые бы позволили запрашивать новые токены доступа,
    // даже когда пользователь не залогинен на сервере авторизации.
    // Это называется offline_access. Ниже мы увидим, как этого добиться.
    // Для краткости, реализацию TokenStorage в данной статье приводить не будем.
    private final TokenStorage accessTokens;

    public Optional<UserInfo> findUserInfo(Integer userId) {
        return accessTokens.find(userId)
                .map(UserInfo::of);
    }

    public String getAuthUrl(Integer userId) {
        var state = UUID.randomUUID().toString();
        userTrackers.put(state, userId);
        return oAuthService.getAuthorizationUrl(state);
    }
    // ...
}
```

Класс, содержащий информацию о пользователе, получаемую от сервера авторизации:

```java
public class UserInfo {
    private final String subject;
    private final String preferredUsername;

    static UserInfo of(OpenIdOAuth2AccessToken token) {
        // Декодируем ID токен и создаем на его основе UserInfo.
        var jwt = JWT.decode(token.getOpenIdToken());
        var subject = jwt.getSubject();
        var preferredUsername = jwt.getClaim("preferred_username").asString();

        return new UserInfo(subject, preferredUsername);
    }
}
```

Создание и настройка OAuth20Service:

```java
@Configuration
@EnableConfigurationProperties(OidcProperties.class)
class OidcAutoConfiguration {
    @Bean
    OAuth20Service oAuthService(OidcProperties properties) {
        // Для создания OAuth20Service указываем id клиента,
        // с которым он зарегистрирован на сервере авторизации, ...
        return new ServiceBuilder(properties.getClientId())
                // секрет клиента, ...
                .apiSecret(properties.getClientSecret())
                // а также запрашиваемые разрешения (scopes):
                // openid — значение по умолчанию для OpenId Connect,
                // offline_access — то самое разрешение для офлайн-доступа к
                // обновлению токенов, о котором мы говорили ранее.
                .defaultScope("openid offline_access")
                // Задаем callback, на который сервер авторизации будет
                // перенаправлять пользователя на шаге 5.
                .callback(properties.getCallback())
                .build(KeycloakApi.instance(properties.getBaseUrl(), properties.getRealm()));
    }
}
```

Callback endpoint нашего бота:

```java
@Component
@Path("/auth")
public class AuthEndpoint {
    private final URI botUri;
    private final OidcService oidcService;

    // ...

    @GET
    @Produces("text/plain; charset=UTF-8")
    public Response auth(
            @QueryParam("state") String state,
            @QueryParam("code") String code) {
        // Запрашиваем у сервера токены в обмен на код авторизации
        // (см. шаг 6 на схеме взаимодействия).
        return oidcService.completeAuth(state, code)
                // Если все ок, то редиректим обратно на чат с ботом.
                .map(userInfo -> Response.temporaryRedirect(botUri).build())
                // Если по указанному state не нашелся пользователь, либо если
                // произошла ошибка в ходе обмена кода авторизации на токены,
                // возвращаем HTTP-статус 500.
                .orElseGet(() -> Response.serverError().entity("Cannot complete authentication").build());
    }
}
```

Снова вернемся к OidcService, чтобы посмотреть на реализацию метода completeAuth:

```java
@Service
public class OidcService {
    private final OAuth20Service oAuthService;
    private final UserTrackerStorage userTrackers;
    private final TokenStorage accessTokens;

    // ...

    public Optional<UserInfo> completeAuth(String state, String code) {
        // Ищем id Telegram-пользователя по полученному state.
        return userTrackers.find(state)
                // Запрашиваем токены и сохраняем их в привязке к id
                // Telegram-пользователя.
                .map(userId -> requestAndStoreToken(code, userId))
                .map(UserInfo::of);
    }

    private OpenIdOAuth2AccessToken requestAndStoreToken(
            String code,
            Integer userId) {
        var token = requestToken(code);
        accessTokens.put(userId, token);
        return token;
    }

    private OpenIdOAuth2AccessToken requestToken(String code) {
        try {
            return (OpenIdOAuth2AccessToken) oAuthService.getAccessToken(code);
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException("Cannot get access token", e);
        }
    }
}
```

Готово!

## В заключение

Поставленная задача решена, но за кадром остался еще ряд вопросов, которые необходимо решить, прежде, чем катить это в прод. Например, офлайн-токены необходимо держать в персистентном хранилище, это позволит не направлять пользователей на повторную аутентификацию после каждого перезапуска/обновления бота.

Эти вопросы уже решены за нас в более развесистых OAuth-клиентах вроде [Spring Security](https://spring.io/projects/spring-security) и [Google OAuth Client](https://github.com/googleapis/google-oauth-java-client). Но для демонстрационных целей нам и так ок :)

Все исходники можно найти на [GitHub](https://github.com/flipp5b/telegram-bot-oidc).
