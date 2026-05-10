package com.cardservice.security.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

public class CardMaskingConverter extends MessageConverter {

    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(\\d{6})(\\d{3,9})(\\d{4})\\b");
    private static final String REPLACEMENT = "$1******$3";

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null) return null;
        return CARD_PATTERN.matcher(message).replaceAll(REPLACEMENT);
    }
}
