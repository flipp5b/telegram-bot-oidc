# Telegram Bot with OpenId Connect authentication

This project demonstrates how to implement authentication in Telegram bot using OpenId Connect (OIDC).

Implementation tools:

- [Keycloak](https://www.keycloak.org) — open source Identity and Access Management solution (OpenId provider).
- [Spring Boot](https://spring.io/projects/spring-boot) — application framework.
- [TelegramBots](https://github.com/rubenlagus/TelegramBots) — library to create Telegram Bots in Java.
- [ScribeJava](https://github.com/scribejava/scribejava) — OAuth client Java library.
- [Java JWT от Auth0](https://github.com/auth0/java-jwt) — Java implementation of JSON Web Token (JWT).

This demo comprises of two services:

- Telegram bot
- OpenId provider

## Building

Ensure you have JDK 11. To build the project run:

```shell script
./mvnw package
```

## Running

Ensure you have Docker and Docker Compose installed.  
Create file `.env` in project root directory using `.env.template` as a base. Specify correct values for the
following entries:

- `BASEURL`: `http://address:8080` where `address` is an IP address/domain name of your host.
- `OIDC_KEYCLOAK_BASEURL`: `http://address:8180` where `address` is an IP address/domain name of your host (do not use `localhost`).
- `BOT_USERNAME`: name of your telegram bot.
- `BOT_TOKEN`: token of your telegram bot.

Ok. Now, run the demo using following command:

```shell script
docker-compose up --build
```
