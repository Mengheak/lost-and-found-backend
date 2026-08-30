package com.group5.lostandfoundjava.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API for Swagger UI, served at {@code /swagger-ui.html}.
 *
 * <p>springdoc discovers the endpoints by itself; this class only adds the parts it cannot guess —
 * the introduction, the "Authorize" button, and human-readable group names.
 */
@Configuration
public class OpenApiConfig {

    /** Referenced by {@code @SecurityRequirement(name = BEARER_SCHEME)} on protected endpoints. */
    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lost & Found API")
                        .description(
                                """
                                Backend API for the Lost & Found application.

                                **Response envelope** — every endpoint returns `{ "success", "message", "data" }`.
                                Paged endpoints put a `PageResponse` in `data` with `content`, `page`, `size`,
                                `totalElements`, `totalPages`, `first` and `last`.

                                **Authentication** — call `POST /api/auth/login` (or `/register`), copy the
                                `accessToken` from the response, then press **Authorize** above and paste it.
                                Access tokens are short-lived; use `POST /api/auth/refresh` to get a new one.

                                **Roles** — endpoints under `/api/admin` and the write side of `/api/categories`
                                require the `ADMIN` role. Everything else is either public or available to any
                                signed-in user.

                                **Real-time chat** is not part of this document: it is a STOMP-over-WebSocket
                                endpoint at `/ws`. Connect with the access token in the CONNECT frame's
                                `Authorization` header, subscribe to `/topic/conversations/{id}` and publish to
                                `/app/conversations/{id}/send`.
                                """)
                        .version("v1")
                        .contact(new Contact().name("Group 5")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the raw access token; Swagger adds the `Bearer ` prefix.")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Registration, login and token refresh"),
                        new Tag().name("Users").description("Own profile and public profiles of other users"),
                        new Tag().name("Items").description("Reporting, searching and managing lost or found items"),
                        new Tag()
                                .name("Categories")
                                .description("Item taxonomy; readable by anyone, editable by admins"),
                        new Tag().name("Saved Items").description("A user's personal shortlist of items"),
                        new Tag().name("Conversations").description("Chat threads between two users about an item"),
                        new Tag().name("Messages").description("Messages inside a conversation"),
                        new Tag().name("Ratings").description("Reputation left after an item is returned"),
                        new Tag().name("Notifications").description("In-app notification feed"),
                        new Tag().name("Admin").description("User administration; requires the ADMIN role")));
    }
}
