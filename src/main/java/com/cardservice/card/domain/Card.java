package com.cardservice.card.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cartao armazenado no sistema. O numero NUNCA e persistido em texto puro -
 * apenas o HMAC-SHA256 do PAN normalizado e gravado em {@code cardNumberHash}.
 */
@Entity
@Table(name = "cards")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Card {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "card_number_hash", length = 64, nullable = false, unique = true, updatable = false)
    private String cardNumberHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "batch_id", length = 8)
    private String batchId;

    @Column(name = "batch_sequence", length = 6)
    private String batchSequence;

    public UUID getCardId() {
        return UUID.fromString(id);
    }
}
