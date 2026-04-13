package com.tienda.products.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JsonApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JsonApiAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper
     */
    public JsonApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param request
     * @param response
     * @param authException
     * @throws IOException
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "";
        }

        log.warn("401 [{}] Acceso no autenticado a: {} — {}",
                correlationId, request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "errors", List.of(Map.of(
                        "status", "401",
                        "code", "UNAUTHORIZED",
                        "title", "No autenticado",
                        "detail", "Se requiere un token JWT valido para acceder a este recurso.",
                        "meta", Map.of("correlationId", correlationId)
                ))
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}
