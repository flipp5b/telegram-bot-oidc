package com.github.flipp5b.telegrambotoidc.ws;


import com.github.flipp5b.telegrambotoidc.BotProperties;
import com.github.flipp5b.telegrambotoidc.auth.OidcService;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

@Component
@Path("/auth")
public class AuthEndpoint {
    private final URI botUri;
    private final OidcService oidcService;

    public AuthEndpoint(BotProperties botProperties, OidcService oidcService) throws URISyntaxException {
        botUri = new URI("tg://resolve?domain=" + botProperties.getUsername());
        this.oidcService = oidcService;
    }

    @GET
    @Produces("text/plain; charset=UTF-8")
    public Response auth(@QueryParam("state") String state, @QueryParam("code") String code) {
        return oidcService.completeAuth(state, code)
                .map(userInfo -> Response.temporaryRedirect(botUri).build())
                .orElseGet(() -> Response.serverError().entity("Cannot complete authentication").build());
    }
}
