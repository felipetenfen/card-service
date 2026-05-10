package com.cardservice.card.service;

import com.cardservice.card.domain.Card;
import com.cardservice.card.dto.CreateCardResponse;
import com.cardservice.card.dto.SearchCardResponse;
import com.cardservice.card.repository.CardRepository;
import com.cardservice.card.validation.LuhnCardValidator;
import com.cardservice.exception.DuplicateCardException;
import com.cardservice.exception.InvalidCardException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock private LuhnCardValidator cardValidator;
    @Mock private CardHashService cardHashService;
    @Mock private CardRepository cardRepository;

    private CardServiceImpl cardService;

    private static final String VALID_CARD = "4532015112830366";
    private static final String HASH = "abc123hash";

    @BeforeEach
    void setUp() {
        cardService = new CardServiceImpl(cardValidator, cardHashService, cardRepository);
    }

    @Test
    void shouldCreateCardSuccessfully() {
        when(cardValidator.isValid(VALID_CARD)).thenReturn(true);
        when(cardHashService.hash(VALID_CARD)).thenReturn(HASH);
        when(cardRepository.existsByCardNumberHash(HASH)).thenReturn(false);
        when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateCardResponse response = cardService.create(VALID_CARD);

        assertThat(response.id()).isNotNull();
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void shouldThrowInvalidCardExceptionWhenLuhnFails() {
        when(cardValidator.isValid(anyString())).thenReturn(false);

        assertThatThrownBy(() -> cardService.create("1234567890123456"))
                .isInstanceOf(InvalidCardException.class);
    }

    @Test
    void shouldThrowDuplicateCardExceptionWhenAlreadyExists() {
        when(cardValidator.isValid(VALID_CARD)).thenReturn(true);
        when(cardHashService.hash(VALID_CARD)).thenReturn(HASH);
        when(cardRepository.existsByCardNumberHash(HASH)).thenReturn(true);

        assertThatThrownBy(() -> cardService.create(VALID_CARD))
                .isInstanceOf(DuplicateCardException.class);
    }

    @Test
    void shouldReturnFoundWhenCardExists() {
        String cardId = UUID.randomUUID().toString();
        Card card = Card.builder()
                .id(cardId)
                .cardNumberHash(HASH)
                .createdAt(LocalDateTime.now())
                .build();

        when(cardValidator.isValid(VALID_CARD)).thenReturn(true);
        when(cardHashService.hash(VALID_CARD)).thenReturn(HASH);
        when(cardRepository.findByCardNumberHash(HASH)).thenReturn(Optional.of(card));

        SearchCardResponse response = cardService.search(VALID_CARD);

        assertThat(response.found()).isTrue();
        assertThat(response.id()).isEqualTo(UUID.fromString(cardId));
    }

    @Test
    void shouldReturnNotFoundWhenCardDoesNotExist() {
        when(cardValidator.isValid(VALID_CARD)).thenReturn(true);
        when(cardHashService.hash(VALID_CARD)).thenReturn(HASH);
        when(cardRepository.findByCardNumberHash(HASH)).thenReturn(Optional.empty());

        SearchCardResponse response = cardService.search(VALID_CARD);

        assertThat(response.found()).isFalse();
        assertThat(response.id()).isNull();
    }
}
