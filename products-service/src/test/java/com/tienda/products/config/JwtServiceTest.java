package com.tienda.products.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "test-secret-32-chars-minimum-ok!!";
    private static final long EXP_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXP_MS);
    }


    @Test
    @DisplayName("generateToken devuelve un token no nulo y no vacío")
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken("admin");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateToken produce tokens distintos en llamadas sucesivas")
    void generateToken_producesDistinctTokens() {
        String t1 = jwtService.generateToken("admin");
        String t2 = jwtService.generateToken("admin");
        assertThat(jwtService.isValid(t1)).isTrue();
        assertThat(jwtService.isValid(t2)).isTrue();
    }


    @Test
    @DisplayName("extractUsername devuelve el username correcto del token")
    void extractUsername_returnsCorrectSubject() {
        String token = jwtService.generateToken("admin");
        String extracted = jwtService.extractUsername(token);
        assertThat(extracted).isEqualTo("admin");
    }

    @Test
    @DisplayName("extractUsername distingue entre distintos usernames")
    void extractUsername_distinguishesUsers() {
        String tokenAdmin = jwtService.generateToken("admin");
        String tokenUser = jwtService.generateToken("otroUsuario");

        assertThat(jwtService.extractUsername(tokenAdmin)).isEqualTo("admin");
        assertThat(jwtService.extractUsername(tokenUser)).isEqualTo("otroUsuario");
    }


    @Test
    @DisplayName("isValid devuelve true para un token recién generado")
    void isValid_trueForFreshToken() {
        String token = jwtService.generateToken("admin");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid devuelve false para un token manipulado")
    void isValid_falseForTamperedToken() {
        String token = jwtService.generateToken("admin");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isValid devuelve false para una cadena arbitraria")
    void isValid_falseForArbitraryString() {
        assertThat(jwtService.isValid("esto.no.es.un.jwt")).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    @DisplayName("isValid devuelve false para un token firmado con secreto distinto")
    void isValid_falseForTokenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService("otro-secreto-32-chars-minimum-ok!!", EXP_MS);
        String foreignToken = otherService.generateToken("admin");
        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("isValid devuelve false para un token expirado")
    void isValid_falseForExpiredToken() throws InterruptedException {
        JwtService shortLivedService = new JwtService(SECRET, 1L);
        String token = shortLivedService.generateToken("admin");
        Thread.sleep(20); // Esperar a que expire
        assertThat(shortLivedService.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("getExpirationSeconds devuelve expirationMs / 1000")
    void getExpirationSeconds_returnsCorrectValue() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600L);
    }
}
