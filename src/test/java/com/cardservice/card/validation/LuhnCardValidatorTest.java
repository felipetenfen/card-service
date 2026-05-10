package com.cardservice.card.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LuhnCardValidatorTest {

    private LuhnCardValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LuhnCardValidator(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4532015112830366",
            "5425233430109903",
            "4111111111111111"
    })
    void shouldReturnTrueForValidCards(String cardNumber) {
        assertThat(validator.isValid(cardNumber)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4456897999999999",
            "1234567890123456",
            "4111111111111112"
    })
    void shouldReturnFalseForInvalidLuhn(String cardNumber) {
        assertThat(validator.isValid(cardNumber)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "12345678901234567890",
            "abcd1234efgh5678",
            ""
    })
    void shouldReturnFalseForMalformedInput(String cardNumber) {
        assertThat(validator.isValid(cardNumber)).isFalse();
    }

    @Test
    void shouldReturnFalseForNull() {
        assertThat(validator.isValid(null)).isFalse();
    }

    @Test
    void shouldReturnTrueForAnyInputWhenDisabled() {
        LuhnCardValidator disabled = new LuhnCardValidator(false);
        assertThat(disabled.isValid("4111111111111112")).isTrue();
        assertThat(disabled.isValid("49012808719106293")).isTrue();
    }
}
