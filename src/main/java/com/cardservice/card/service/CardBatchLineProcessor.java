package com.cardservice.card.service;

import com.cardservice.card.batch.BatchItemStatus;
import com.cardservice.card.batch.BatchLine;
import com.cardservice.card.dto.BatchItemResult;
import com.cardservice.card.dto.CreateCardResponse;
import com.cardservice.exception.DuplicateCardException;
import com.cardservice.exception.InvalidCardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardBatchLineProcessor {

    private static final Logger log = LoggerFactory.getLogger(CardBatchLineProcessor.class);

    private final CardService cardService;

    public CardBatchLineProcessor(CardService cardService) {
        this.cardService = cardService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchItemResult process(BatchLine line, String batchId) {
        try {
            CreateCardResponse response = cardService.createFromBatch(line.cardNumber(), batchId, line.sequence());
            return new BatchItemResult(line.sequence(), BatchItemStatus.SUCCESS, response.id(), null);
        } catch (InvalidCardException e) {
            log.warn("Batch {}: invalid card at sequence {}", batchId, line.sequence());
            return new BatchItemResult(line.sequence(), BatchItemStatus.INVALID, null, "INVALID_CARD_NUMBER");
        } catch (DuplicateCardException e) {
            log.warn("Batch {}: duplicate card at sequence {}", batchId, line.sequence());
            return new BatchItemResult(line.sequence(), BatchItemStatus.DUPLICATE, null, "CARD_ALREADY_EXISTS");
        }
    }
}
