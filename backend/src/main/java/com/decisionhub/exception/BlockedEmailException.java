package com.decisionhub.exception;

public class BlockedEmailException extends RuntimeException {
    public BlockedEmailException(String message) {
        super(message);
    }
}
