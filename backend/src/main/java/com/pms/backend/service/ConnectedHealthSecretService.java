package com.pms.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
public class ConnectedHealthSecretService {
    private final TextEncryptor encryptor;

    public ConnectedHealthSecretService(@Value("${pms.connected-health.secret:${pms.jwt.secret}}") String secret) {
        this.encryptor = Encryptors.delux(normalizedSecret(secret), salt(secret));
    }

    public String encrypt(String value) {
        return value == null || value.isBlank() ? null : encryptor.encrypt(value);
    }

    public String decrypt(String value) {
        return value == null || value.isBlank() ? null : encryptor.decrypt(value);
    }

    private String normalizedSecret(String value) {
        return value == null || value.isBlank() ? "pms-connected-health-secret" : value;
    }

    private String salt(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizedSecret(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int index = 0; index < 8; index += 1) {
                hex.append(String.format("%02x", digest[index]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("Connected-health encryption could not be initialized.", error);
        }
    }
}
