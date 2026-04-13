package com.tienda.products.controller;

import com.tienda.products.config.JwtService;
import com.tienda.products.dto.LoginRequest;
import com.tienda.products.dto.LoginResponse;
import com.tienda.products.dto.jsonapi.JsonApiResponse;
import com.tienda.products.dto.jsonapi.JsonApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Emision de JWT para el frontend")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final Map<String, String> MOCK_USERS = Map.of(
            "admin", "$2a$10$m825l/NleIEk.hfMXUdyOu5X/H/XBOg5vSw1BD/Go7e8dh3k6W5Ee"
    );

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param jwtService
     * @param passwordEncoder
     */
    public AuthController(JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @param request
     * @return
     */
    @Operation(
            summary = "Login",
            description = "Autentica un usuario y devuelve un JWT. Credenciales de prueba: admin / admin123"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso -- devuelve token JWT en envelope JSON:API"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(schema = @Schema(example = """
                            {"errors":[{"status":"401","code":"INVALID_CREDENTIALS",
                            "title":"Credenciales incorrectas",
                            "detail":"Usuario o contrasena incorrectos.",
                            "meta":{"correlationId":"uuid"}}]}
                            """)))
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Intento de login para usuario: {}", request.username());

        String storedHash = MOCK_USERS.get(request.username());

        if (storedHash == null || !passwordEncoder.matches(request.password(), storedHash)) {
            log.warn("Login fallido para usuario: {}", request.username());

            String correlationId = MDC.get("correlationId");
            if (correlationId == null) correlationId = "";

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "errors", List.of(Map.of(
                            "status", "401",
                            "code", "INVALID_CREDENTIALS",
                            "title", "Credenciales incorrectas",
                            "detail", "Usuario o contrasena incorrectos.",
                            "meta", Map.of("correlationId", correlationId)
                    ))
            ));
        }

        String token = jwtService.generateToken(request.username());
        log.info("Login exitoso para usuario: {}", request.username());

        JsonApiResponse<LoginResponse> body = JsonApiResponseFactory.fromLogin(
                new LoginResponse(token, jwtService.getExpirationSeconds())
        );

        return ResponseEntity.ok(body);
    }
}