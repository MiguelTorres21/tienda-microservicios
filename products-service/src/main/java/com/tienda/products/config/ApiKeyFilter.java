package com.tienda.products.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String INTERNAL_PREFIX = "/internal/v1/";

    private final String expectedApiKey;

    /**
     * @param expectedApiKey
     */
    public ApiKeyFilter(@Value("${internal.api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    /**
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(INTERNAL_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) correlationId = "";

            log.warn("401 [{}] API Key invalida o ausente en: {}", correlationId, request.getRequestURI());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"errors\":[{\"status\":\"401\",\"code\":\"INVALID_API_KEY\"," +
                            "\"title\":\"API Key invalida\"," +
                            "\"detail\":\"Se requiere una API Key valida en el header X-API-Key.\"," +
                            "\"meta\":{\"correlationId\":\"" + correlationId + "\"}}]}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
