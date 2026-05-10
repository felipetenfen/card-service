package com.cardservice.card.batch;

import com.cardservice.exception.InvalidBatchFileException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the fixed-position TXT batch file format.
 *
 * Header  : [0-28] name | [29-36] YYYYMMDD | [37-44] batchId | [45-50] quantity
 * Card    : [0]    "C"  | [1-6]   sequence  | [7-25]  cardNumber (trim trailing spaces)
 * Trailer : [0-7]  batchId | [8-13] quantity
 */
@Component
public class BatchFileParser {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public BatchParseResult parse(InputStream input) {
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII))) {
            lines = reader.lines().toList();
        } catch (IOException e) {
            throw new InvalidBatchFileException("Could not read batch file");
        }

        List<String> trimmed = new ArrayList<>(lines);
        while (!trimmed.isEmpty() && trimmed.getLast().isBlank()) {
            trimmed.removeLast();
        }
        lines = trimmed;

        if (lines.size() < 2) {
            throw new InvalidBatchFileException("File too short — missing header or trailer");
        }

        BatchHeader header = parseHeader(lines.getFirst());
        BatchTrailer trailer = parseTrailer(lines.getLast());

        List<BatchLine> cardLines = new ArrayList<>();
        for (int i = 1; i < lines.size() - 1; i++) {
            cardLines.add(parseCardLine(lines.get(i), i + 1));
        }

        if (cardLines.size() != header.quantity()) {
            throw new InvalidBatchFileException(
                    "Record count mismatch: header declares " + header.quantity() +
                    " but file contains " + cardLines.size() + " card lines");
        }
        if (trailer.quantity() != header.quantity()) {
            throw new InvalidBatchFileException(
                    "Record count mismatch: header=" + header.quantity() +
                    " trailer=" + trailer.quantity());
        }

        return new BatchParseResult(header, cardLines, trailer);
    }

    private BatchHeader parseHeader(String line) {
        line = padRight(line, 51);
        try {
            String name = line.substring(0, 29).trim();
            String dateStr = line.substring(29, 37).trim();
            String batchId = line.substring(37, 45).trim();
            String qtyStr = line.substring(45, 51).trim();

            LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
            int qty = Integer.parseInt(qtyStr);

            return new BatchHeader(name, date, batchId, qty);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new InvalidBatchFileException("Malformed header line: " + e.getMessage());
        }
    }

    private BatchTrailer parseTrailer(String line) {
        line = padRight(line, 14);
        try {
            String batchId = line.substring(0, 8).trim();
            String qtyStr = line.substring(8, 14).trim();
            int qty = Integer.parseInt(qtyStr);
            return new BatchTrailer(batchId, qty);
        } catch (NumberFormatException e) {
            throw new InvalidBatchFileException("Malformed trailer line: " + e.getMessage());
        }
    }

    private BatchLine parseCardLine(String line, int lineNumber) {
        if (line == null || line.isBlank()) {
            throw new InvalidBatchFileException("Empty card line at position " + lineNumber);
        }
        line = padRight(line, 26);
        if (line.charAt(0) != 'C') {
            throw new InvalidBatchFileException("Card line at position " + lineNumber + " does not start with 'C'");
        }
        String sequence = line.substring(1, 7).trim();
        String cardNumber = line.substring(7, 26).trim();
        return new BatchLine(sequence, cardNumber);
    }

    private String padRight(String s, int length) {
        if (s.length() >= length) return s;
        return String.format("%-" + length + "s", s);
    }
}
