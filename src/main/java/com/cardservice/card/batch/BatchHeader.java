package com.cardservice.card.batch;

import java.time.LocalDate;

public record BatchHeader(String name, LocalDate date, String batchId, int quantity) {}
