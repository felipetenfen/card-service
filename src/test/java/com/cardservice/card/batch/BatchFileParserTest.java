package com.cardservice.card.batch;

import com.cardservice.exception.InvalidBatchFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchFileParserTest {

    private BatchFileParser parser;

    @BeforeEach
    void setUp() {
        parser = new BatchFileParser();
    }

    @Test
    void shouldParseValidFile() {
        String content =
                "CARD-SERVICE-BATCH           20180524LOTE0001000002\n" +
                "C1     4532015112830366                             \n" +
                "C2     5425233430109903                             \n" +
                "LOTE0001000002                                      ";

        BatchParseResult result = parser.parse(toStream(content));

        assertThat(result.header().batchId()).isEqualTo("LOTE0001");
        assertThat(result.header().quantity()).isEqualTo(2);
        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().get(0).cardNumber()).isEqualTo("4532015112830366");
        assertThat(result.lines().get(1).cardNumber()).isEqualTo("5425233430109903");
        assertThat(result.trailer().quantity()).isEqualTo(2);
    }

    @Test
    void shouldThrowWhenHeaderCountDoesNotMatchLines() {
        String content =
                "CARD-SERVICE-BATCH           20180524LOTE0001000005\n" +
                "C1     4532015112830366                             \n" +
                "LOTE0001000005                                      ";

        assertThatThrownBy(() -> parser.parse(toStream(content)))
                .isInstanceOf(InvalidBatchFileException.class)
                .hasMessageContaining("Record count mismatch");
    }

    @Test
    void shouldThrowWhenHeaderAndTrailerCountDiffer() {
        String content =
                "CARD-SERVICE-BATCH           20180524LOTE0001000001\n" +
                "C1     4532015112830366                             \n" +
                "LOTE0001000099                                      ";

        assertThatThrownBy(() -> parser.parse(toStream(content)))
                .isInstanceOf(InvalidBatchFileException.class)
                .hasMessageContaining("mismatch");
    }

    @Test
    void shouldThrowForFileTooShort() {
        assertThatThrownBy(() -> parser.parse(toStream("ONLYONELINE")))
                .isInstanceOf(InvalidBatchFileException.class);
    }

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.US_ASCII));
    }
}
