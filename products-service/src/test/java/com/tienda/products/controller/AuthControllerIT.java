package com.tienda.products.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.products.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController — POST /api/v1/auth/login")
class AuthControllerIT extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Test
    @DisplayName("credenciales correctas devuelven 200 con token en envelope JSON:API")
    void login_returnsJwt_withValidCredentials() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("auth-tokens"))
                .andExpect(jsonPath("$.data.attributes.token").isNotEmpty())
                .andExpect(jsonPath("$.data.attributes.expiresIn").isNumber());
    }

    @Test
    @DisplayName("token devuelto es un JWT con 3 partes (header.payload.signature)")
    void login_returnsWellFormedJwt() throws Exception {
        String responseBody = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "admin123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        String token = (String) ((Map<String, Object>)
                ((Map<String, Object>) objectMapper.readValue(responseBody, Map.class)
                        .get("data")).get("attributes")).get("token");

        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("contraseña incorrecta devuelve 401 con código INVALID_CREDENTIALS")
    void login_returns401_withWrongPassword() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].status").value("401"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.errors[0].meta.correlationId").exists());
    }

    @Test
    @DisplayName("usuario inexistente devuelve 401 con código INVALID_CREDENTIALS")
    void login_returns401_withUnknownUser() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("no-existe", "cualquier")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("username en blanco devuelve 422 VALIDATION_ERROR")
    void login_returns422_whenUsernameBlank() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("", "admin123")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("password en blanco devuelve 422 VALIDATION_ERROR")
    void login_returns422_whenPasswordBlank() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("body vacío {} devuelve 422")
    void login_returns422_whenBodyEmpty() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }


    private String loginBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("username", username, "password", password));
    }
}
