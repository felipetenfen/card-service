package com.cardservice.card.repository;

import com.cardservice.card.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, String> {

    boolean existsByCardNumberHash(String cardNumberHash);

    Optional<Card> findByCardNumberHash(String cardNumberHash);
}
