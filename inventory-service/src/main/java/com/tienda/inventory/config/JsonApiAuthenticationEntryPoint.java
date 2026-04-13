package com.tienda.inventory.config;

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
     * @param ex
     * @throws IOException
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        String cid = MDC.get("correlationId");
        if (cid == null) cid = "";
        log.warn("401 [{}] No autenticado en: {}", cid, request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("errors", List.of(Map.of(
                "status", "401", "code", "UNAUTHORIZED",
                "title", "No autenticado",
                "detail", "Se requiere un token JWT valido.",
                "meta", Map.of("correlationId", cid)
        ))));
    }
}
