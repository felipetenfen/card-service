CREATE TABLE cards (
    id               VARCHAR(36)  NOT NULL,
    card_number_hash VARCHAR(64)  NOT NULL,
    created_at       DATETIME     NOT NULL,
    batch_id         VARCHAR(8)   NULL,
    batch_sequence   VARCHAR(6)   NULL,
    CONSTRAINT pk_cards PRIMARY KEY (id),
    CONSTRAINT uk_cards_card_number_hash UNIQUE (card_number_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cards_batch_id ON cards (batch_id);
