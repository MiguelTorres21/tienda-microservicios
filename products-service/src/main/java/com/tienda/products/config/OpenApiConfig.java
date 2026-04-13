package com.tienda.products.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    /**
     * @return
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Products Service API")
                        .version("1.0.0")
                        .description("""
                                Microservicio de gestión de productos.
                                
                                **Autenticación:**
                                - Endpoints `/api/v1/products/**` requieren JWT Bearer.
                                - Obtén el token con `POST /api/v1/auth/login`.
                                - Endpoints `/internal/v1/**` requieren el header `X-API-Key` (uso inter-servicios).
                                """)
                        .contact(new Contact().name("Tienda Dev Team")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en POST /api/v1/auth/login"))
                        .addSecuritySchemes("apiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API Key para llamadas inter-servicios (/internal/v1/**)"))
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
