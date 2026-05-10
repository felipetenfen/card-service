package com.cardservice.card.service;

import com.cardservice.card.batch.BatchFileParser;
import com.cardservice.card.batch.BatchItemStatus;
import com.cardservice.card.batch.BatchLine;
import com.cardservice.card.batch.BatchParseResult;
import com.cardservice.card.dto.BatchItemResult;
import com.cardservice.card.dto.BatchUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class CardBatchService {

    private static final Logger log = LoggerFactory.getLogger(CardBatchService.class);

    private final BatchFileParser batchFileParser;
    private final CardBatchLineProcessor lineProcessor;

    public CardBatchService(BatchFileParser batchFileParser, CardBatchLineProcessor lineProcessor) {
        this.batchFileParser = batchFileParser;
        this.lineProcessor = lineProcessor;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BatchUploadResponse process(InputStream input) {
        BatchParseResult parsed = batchFileParser.parse(input);
        String batchId = parsed.header().batchId();
        int totalDeclared = parsed.header().quantity();

        List<BatchItemResult> results = new ArrayList<>();
        int inserted = 0;
        int rejected = 0;

        for (BatchLine line : parsed.lines()) {
            BatchItemResult result = lineProcessor.process(line, batchId);
            results.add(result);
            if (BatchItemStatus.SUCCESS == result.status()) {
                inserted++;
            } else {
                rejected++;
            }
        }

        log.info("Batch {} processed: declared={} inserted={} rejected={}", batchId, totalDeclared, inserted, rejected);
        return new BatchUploadResponse(batchId, totalDeclared, inserted, rejected, results);
    }
}
