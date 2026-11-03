package com.decisionhub.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String path;
    private Long retryAfterSeconds;
    private String remainingTime;

    public ApiErrorResponse(int status, String error, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.path = path;
    }

    public ApiErrorResponse(int status, String error, String path, Long retryAfterSeconds, String remainingTime) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.path = path;
        this.retryAfterSeconds = retryAfterSeconds;
        this.remainingTime = remainingTime;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getPath() {
        return path;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getRemainingTime() {
        return remainingTime;
    }
}
