package com.cardservice.card.service;

import com.cardservice.card.batch.*;
import com.cardservice.card.dto.BatchItemResult;
import com.cardservice.card.dto.BatchUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardBatchServiceTest {

    @Mock private BatchFileParser batchFileParser;
    @Mock private CardBatchLineProcessor lineProcessor;

    private CardBatchService cardBatchService;

    @BeforeEach
    void setUp() {
        cardBatchService = new CardBatchService(batchFileParser, lineProcessor);
    }

    @Test
    void shouldProcessBatchSuccessfully() {
        BatchHeader header = new BatchHeader("TEST", LocalDate.now(), "LOTE001", 2);
        BatchLine line1 = new BatchLine("1", "4532015112830366");
        BatchLine line2 = new BatchLine("2", "5425233430109903");
        BatchTrailer trailer = new BatchTrailer("LOTE001", 2);
        BatchParseResult result = new BatchParseResult(header, List.of(line1, line2), trailer);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        BatchItemResult item1 = new BatchItemResult("1", BatchItemStatus.SUCCESS, id1, null);
        BatchItemResult item2 = new BatchItemResult("2", BatchItemStatus.SUCCESS, id2, null);

        when(batchFileParser.parse(any(InputStream.class))).thenReturn(result);
        when(lineProcessor.process(line1, "LOTE001")).thenReturn(item1);
        when(lineProcessor.process(line2, "LOTE001")).thenReturn(item2);

        BatchUploadResponse response = cardBatchService.process(mock(InputStream.class));

        assertThat(response.batchId()).isEqualTo("LOTE001");
        assertThat(response.totalDeclared()).isEqualTo(2);
        assertThat(response.totalInserted()).isEqualTo(2);
        assertThat(response.totalRejected()).isEqualTo(0);
        assertThat(response.results()).hasSize(2);
    }

    @Test
    void shouldHandlePartialFailures() {
        BatchHeader header = new BatchHeader("TEST", LocalDate.now(), "LOTE001", 2);
        BatchLine line1 = new BatchLine("1", "4532015112830366");
        BatchLine line2 = new BatchLine("2", "invalid");
        BatchTrailer trailer = new BatchTrailer("LOTE001", 2);
        BatchParseResult result = new BatchParseResult(header, List.of(line1, line2), trailer);

        UUID id1 = UUID.randomUUID();
        BatchItemResult item1 = new BatchItemResult("1", BatchItemStatus.SUCCESS, id1, null);
        BatchItemResult item2 = new BatchItemResult("2", BatchItemStatus.INVALID, null, "INVALID_CARD_NUMBER");

        when(batchFileParser.parse(any(InputStream.class))).thenReturn(result);
        when(lineProcessor.process(line1, "LOTE001")).thenReturn(item1);
        when(lineProcessor.process(line2, "LOTE001")).thenReturn(item2);

        BatchUploadResponse response = cardBatchService.process(mock(InputStream.class));

        assertThat(response.totalInserted()).isEqualTo(1);
        assertThat(response.totalRejected()).isEqualTo(1);
    }

    @Test
    void shouldCountDuplicateCardAsRejected() {
        BatchHeader header = new BatchHeader("TEST", LocalDate.now(), "LOTE001", 2);
        BatchLine line1 = new BatchLine("1", "4532015112830366");
        BatchLine line2 = new BatchLine("2", "5425233430109903");
        BatchTrailer trailer = new BatchTrailer("LOTE001", 2);
        BatchParseResult result = new BatchParseResult(header, List.of(line1, line2), trailer);

        UUID id1 = UUID.randomUUID();
        BatchItemResult item1 = new BatchItemResult("1", BatchItemStatus.SUCCESS, id1, null);
        BatchItemResult item2 = new BatchItemResult("2", BatchItemStatus.DUPLICATE, null, "CARD_ALREADY_EXISTS");

        when(batchFileParser.parse(any(InputStream.class))).thenReturn(result);
        when(lineProcessor.process(line1, "LOTE001")).thenReturn(item1);
        when(lineProcessor.process(line2, "LOTE001")).thenReturn(item2);

        BatchUploadResponse response = cardBatchService.process(mock(InputStream.class));

        assertThat(response.totalInserted()).isEqualTo(1);
        assertThat(response.totalRejected()).isEqualTo(1);
        assertThat(response.results().get(1).status()).isEqualTo(BatchItemStatus.DUPLICATE);
        assertThat(response.results().get(1).reason()).isEqualTo("CARD_ALREADY_EXISTS");
    }
}
