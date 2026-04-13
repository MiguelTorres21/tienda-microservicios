package com.tienda.inventory.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


public final class JwtTestHelper {

    public static final String JWT_SECRET = "cambiar-este-secreto-en-produccion-min-32-chars";

    private JwtTestHelper() {
    }

    /**
     * @param subject
     * @return
     */
    public static String generateToken(String subject) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }

    public static String bearerHeader() {
        return "Bearer " + generateToken("test-user");
    }
}
