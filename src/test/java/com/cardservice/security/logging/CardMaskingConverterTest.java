package com.cardservice.security.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CardMaskingConverterTest {

    private CardMaskingConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CardMaskingConverter();
    }

    @Test
    void shouldMaskPanPreservingBinAndLastFour() {
        assertThat(convert("Card: 4532015112830366 stored"))
                .isEqualTo("Card: 453201******0366 stored");
    }

    @Test
    void shouldMaskMultiplePansInSameMessage() {
        assertThat(convert("4532015112830366 and 5425233430109903"))
                .isEqualTo("453201******0366 and 542523******9903");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4532015112830366",   // 16 digits (standard Visa)
            "5425233430109903",   // 16 digits (Mastercard)
            "4111111111111111",   // 16 digits
            "4000000000000002",   // 16 digits
    })
    void shouldMaskValidCardNumbers(String pan) {
        String result = convert(pan);
        assertThat(result).startsWith(pan.substring(0, 6));
        assertThat(result).endsWith(pan.substring(pan.length() - 4));
        assertThat(result).contains("******");
        assertThat(result).doesNotContain(pan.substring(6, pan.length() - 4));
    }

    @Test
    void shouldNotMaskNumbersTooShortToBeACard() {
        assertThat(convert("Code: 12345")).isEqualTo("Code: 12345");
    }

    @Test
    void shouldNotAlterMessageWithNoCardNumber() {
        String message = "User authenticated: username=admin";
        assertThat(convert(message)).isEqualTo(message);
    }

    @Test
    void shouldReturnNullWhenMessageIsNull() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(null);
        assertThat(converter.convert(event)).isNull();
    }

    private String convert(String message) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(message);
        return converter.convert(event);
    }
}
