package com.cardservice.card.service;

import com.cardservice.card.dto.CreateCardResponse;
import com.cardservice.card.dto.SearchCardResponse;

public interface CardService {
    CreateCardResponse create(String rawCardNumber);
    CreateCardResponse createFromBatch(String rawCardNumber, String batchId, String sequence);
    SearchCardResponse search(String rawCardNumber);
}
