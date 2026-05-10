package com.cardservice.card.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class CardHashService {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] keyBytes;

    public CardHashService(@Value("${card.hmac.key}") String hmacKey) {
        byte[] bytes = hmacKey.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("CARD_HMAC_KEY must be at least 32 bytes");
        }
        this.keyBytes = bytes;
    }

    public String hash(String cardNumber) {
        String normalized = cardNumber.replaceAll("[\\s-]", "");
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(keyBytes, ALGORITHM));
            byte[] raw = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
