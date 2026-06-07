package com.bankafrica.bankingapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 definition powering the interactive docs at {@code /swagger-ui.html}.
 *
 * <p>A single {@code bearerAuth} security scheme is declared and applied globally, so the
 * Swagger UI shows an <b>Authorize</b> button: paste a JWT obtained from {@code /api/auth/login}
 * (or register) and every protected endpoint becomes callable straight from the browser.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI bankAfricaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Africa API")
                        .description("""
                                Secure, stateless banking API: registration & JWT login, account
                                operations (deposit, withdraw, transfer), an immutable transaction
                                ledger, idempotent money movement, and SWIFT MT103 generation for
                                transfers. All money operations act on the authenticated user's own
                                account, resolved from the bearer token.""")
                        .version("v1")
                        .contact(new Contact().name("Bank Africa").email("support@bankafrica.example"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste a JWT from /api/auth/login (no \"Bearer \" prefix needed).")));
    }
}
