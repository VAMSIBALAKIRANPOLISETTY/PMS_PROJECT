package com.pms.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTests {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-07T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsSignedJwtAndExtractsUserIdFromBearerHeader() {
        JwtService jwtService = service("shared-secret");

        String token = jwtService.createToken(user(42L, Role.USER));

        assertEquals(3, token.split("\\.").length);
        assertEquals(42L, jwtService.userIdFromBearer("Bearer " + token));
    }

    @Test
    void rejectsMissingBearerHeader() {
        JwtService jwtService = service("shared-secret");

        assertThrows(IllegalArgumentException.class, () -> jwtService.userIdFromBearer(null));
        assertThrows(IllegalArgumentException.class, () -> jwtService.userIdFromBearer("Basic abc"));
    }

    @Test
    void rejectsMalformedToken() {
        JwtService jwtService = service("shared-secret");

        assertThrows(IllegalArgumentException.class, () -> jwtService.userIdFromBearer("Bearer not-a-jwt"));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService expiredService = new JwtService("shared-secret", "PMS Health", Duration.ofHours(-1), FIXED_CLOCK);
        String token = expiredService.createToken(user(42L, Role.USER));

        assertThrows(IllegalArgumentException.class, () -> service("shared-secret").userIdFromBearer("Bearer " + token));
    }

    @Test
    void rejectsWrongSignature() {
        String token = service("first-secret").createToken(user(42L, Role.USER));

        assertThrows(IllegalArgumentException.class, () -> service("second-secret").userIdFromBearer("Bearer " + token));
    }

    private JwtService service(String secret) {
        return new JwtService(secret, "PMS Health", Duration.ofHours(12), FIXED_CLOCK);
    }

    private AppUser user(Long id, Role role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setRole(role);
        user.setUsername("jwtuser");
        user.setEmail("jwt@example.com");
        return user;
    }
}
