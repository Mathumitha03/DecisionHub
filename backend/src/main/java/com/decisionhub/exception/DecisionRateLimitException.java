package com.decisionhub.exception;

/**
 * Exception thrown when a user exceeds the decision creation rate limit.
 */
public class DecisionRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;
    private final String remainingTimeFormatted;

    public DecisionRateLimitException(String message, long retryAfterSeconds, String remainingTimeFormatted) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.remainingTimeFormatted = remainingTimeFormatted;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getRemainingTimeFormatted() {
        return remainingTimeFormatted;
    }
}
