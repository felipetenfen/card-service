package com.cardservice.exception;

public class DuplicateCardException extends RuntimeException {
    public DuplicateCardException() {
        super("Card number already registered in the system");
    }
}
