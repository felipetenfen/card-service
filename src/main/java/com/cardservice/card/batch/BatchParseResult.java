package com.cardservice.card.batch;

import java.util.List;

public record BatchParseResult(BatchHeader header, List<BatchLine> lines, BatchTrailer trailer) {}
