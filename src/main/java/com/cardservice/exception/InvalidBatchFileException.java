package com.cardservice.exception;

public class InvalidBatchFileException extends RuntimeException {
    public InvalidBatchFileException(String message) {
        super(message);
    }
}
