package com.cardservice.card.service;

import com.cardservice.card.domain.Card;
import com.cardservice.card.dto.CreateCardResponse;
import com.cardservice.card.dto.SearchCardResponse;
import com.cardservice.card.repository.CardRepository;
import com.cardservice.card.validation.CardValidator;
import com.cardservice.exception.DuplicateCardException;
import com.cardservice.exception.InvalidCardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CardServiceImpl implements CardService {

    private static final Logger log = LoggerFactory.getLogger(CardServiceImpl.class);

    private final CardValidator cardValidator;
    private final CardHashService cardHashService;
    private final CardRepository cardRepository;

    public CardServiceImpl(CardValidator cardValidator,
                           CardHashService cardHashService,
                           CardRepository cardRepository) {
        this.cardValidator = cardValidator;
        this.cardHashService = cardHashService;
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public CreateCardResponse create(String rawCardNumber) {
        validate(rawCardNumber);
        String hash = cardHashService.hash(rawCardNumber);
        checkDuplicate(hash);
        Card saved = save(hash, null, null);
        log.info("Card created with id={}", saved.getId());
        return new CreateCardResponse(saved.getCardId(), saved.getCreatedAt());
    }

    @Override
    @Transactional(noRollbackFor = {DuplicateCardException.class, InvalidCardException.class})
    public CreateCardResponse createFromBatch(String rawCardNumber, String batchId, String sequence) {
        validate(rawCardNumber);
        String hash = cardHashService.hash(rawCardNumber);
        checkDuplicate(hash);
        Card saved = save(hash, batchId, sequence);
        log.info("Batch card created with id={} batchId={} seq={}", saved.getId(), batchId, sequence);
        return new CreateCardResponse(saved.getCardId(), saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public SearchCardResponse search(String rawCardNumber) {
        validate(rawCardNumber);
        String hash = cardHashService.hash(rawCardNumber);
        return cardRepository.findByCardNumberHash(hash)
                .map(c -> new SearchCardResponse(true, c.getCardId()))
                .orElse(new SearchCardResponse(false, null));
    }

    private void validate(String rawCardNumber) {
        if (!cardValidator.isValid(rawCardNumber)) {
            throw new InvalidCardException("Card number failed Luhn validation");
        }
    }

    private void checkDuplicate(String hash) {
        if (cardRepository.existsByCardNumberHash(hash)) {
            throw new DuplicateCardException();
        }
    }

    private Card save(String hash, String batchId, String sequence) {
        try {
            Card card = Card.builder()
                    .id(UUID.randomUUID().toString())
                    .cardNumberHash(hash)
                    .createdAt(LocalDateTime.now())
                    .batchId(batchId)
                    .batchSequence(sequence)
                    .build();
            return cardRepository.save(card);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCardException();
        }
    }
}
