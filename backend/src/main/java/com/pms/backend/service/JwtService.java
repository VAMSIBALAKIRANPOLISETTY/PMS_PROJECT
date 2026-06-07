package com.pms.backend.service;

import com.pms.backend.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration expiration;
    private final Clock clock;

    @Autowired
    public JwtService(
            @Value("${pms.jwt.secret}") String secret,
            @Value("${pms.jwt.issuer}") String issuer,
            @Value("${pms.jwt.expiration-hours}") long expirationHours) {
        this(secret, issuer, Duration.ofHours(expirationHours), Clock.systemUTC());
    }

    JwtService(String secret, String issuer, Duration expiration, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(normalizedSecret(secret));
        this.issuer = issuer;
        this.expiration = expiration;
        this.clock = clock;
    }

    public String createToken(AppUser user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiration);
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("role", user.getRole().name())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .signWith(signingKey)
                .compact();
    }

    public Long userIdFromBearer(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Login required.");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Login required.");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | NumberFormatException error) {
            throw new IllegalArgumentException("Invalid or expired token.");
        }
    }

    private static byte[] normalizedSecret(String secret) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) {
            return bytes;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("Unable to initialize JWT signing key.", error);
        }
    }
}
